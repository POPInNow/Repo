/*
 * Copyright (C) 2018 POP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.popinnow.android.repo.behavior

import android.support.annotation.CheckResult
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.impl.MemoryCacheImpl
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS

class MemoryCacheBehaviorTest {

  @CheckResult
  private fun createCache(
    debug: String,
    time: Long,
    maxSize: Int = 10
  ): MemoryCache {
    return MemoryCacheImpl(debug, time, SECONDS, maxSize)
  }

  private fun assertCacheIsEmpty(
    cache: MemoryCache,
    key: String
  ) {
    cache.get(key) { it }
        .startNow()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete()
  }

  private fun assertCacheSingleValue(
    cache: MemoryCache,
    key: String,
    value: String
  ) {
    cache.get(key) { it as String }
        .startNow()
        .test()
        .assertValue(value)
        .assertNoErrors()
        .assertComplete()
  }

  private fun assertCacheValues(
    cache: MemoryCache,
    key: String,
    vararg values: String
  ) {
    cache.get(key) { it as String }
        .startNow()
        .test()
        .assertValueSequence(arrayListOf(*values))
        .assertNoErrors()
        .assertComplete()
  }

  private fun assertCacheSize(
    cache: MemoryCache,
    size: Int
  ) {
    assert(cache.size() == size) { "Wrong cache size: ${cache.size()}, expected $size" }
  }

  private fun fillCacheWithDummyData(
    cache: MemoryCache,
    size: Int
  ) {
    val originalCacheSize = cache.size()
    for (i in 1..size) {
      cache.add("key$i", "data$i")
      assertCacheSize(cache, originalCacheSize + i)
    }
  }

  /**
   * Starts empty by default. If nothing puts into the cache, it returns no data.
   */
  @Test
  fun `MemoryCacheBehaviorTest is empty by default`() {
    val cache = createCache("is empty default", 30)
    assertCacheIsEmpty(
        cache,
        DEFAULT_KEY
    )
  }

  /**
   * Does it actually work
   */
  @Test
  fun `MemoryCacheBehaviorTest maintains cache`() {
    val cache = createCache("maintain cache", 30)

    // Putting data into the cache fills it
    cache.add(
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // So it returns data on query
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // As long as it does not time out or clear, it will continue to return data
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

  }

  /**
   * When you invalidate the cache, the old data is gone immediately
   */
  @Test
  fun `MemoryCacheBehaviorTest invalidates`() {
    val cache = createCache("invalidates", 30)
    // Putting data into the cache fills it
    cache.add(
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // So it returns data on query
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // Assert the size of the cache is correct
    assertCacheSize(cache, 1)

    // Invalidating the cache then clears it
    cache.invalidate(DEFAULT_KEY)

    // So it does not return
    assertCacheIsEmpty(
        cache,
        DEFAULT_KEY
    )

    // Assert the size of the cache is correct
    assertCacheSize(cache, 0)
  }

  /**
   * When you clear the cache, the old data is gone immediately
   */
  @Test
  fun `MemoryCacheBehaviorTest clears`() {
    val cache = createCache("clears", 30)
    // Putting data into the cache fills it
    cache.add(
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // So it returns data on query
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // Assert the size of the cache is correct
    assertCacheSize(cache, 1)

    // Put some more data
    fillCacheWithDummyData(cache, 5)

    // Assert the size of the cache is correct
    assertCacheSize(cache, 6)

    // Invalidating the cache then clears it
    cache.clearAll()

    // So it does not return
    assertCacheIsEmpty(
        cache,
        DEFAULT_KEY
    )

    // Assert the size of the cache is correct
    assertCacheSize(cache, 0)
  }

  /**
   * When you invalidate the cache for one key, other keys are not touched
   */
  @Test
  fun `MemoryCacheBehaviorTest invalidate is unique`() {
    val cache = createCache("invalidate is unique", 30)
    // Putting data into the cache fills it
    cache.add(
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // So it returns data on query
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // Assert the size of the cache is correct
    assertCacheSize(cache, 1)

    // Put some more data
    fillCacheWithDummyData(cache, 5)

    // Assert the size of the cache is correct
    assertCacheSize(cache, 6)

    // Invalidating the cache then clears it
    cache.invalidate(DEFAULT_KEY)

    // Assert the size of the cache is correct
    assertCacheSize(cache, 5)

    // Assert the invalidated key is gone
    assertCacheIsEmpty(
        cache,
        DEFAULT_KEY
    )

    // Assert that other keys still exist
    assertCacheSingleValue(cache, "key1", "data1")
    assertCacheSingleValue(cache, "key2", "data2")
    assertCacheSingleValue(cache, "key3", "data3")
    assertCacheSingleValue(cache, "key4", "data4")
    assertCacheSingleValue(cache, "key5", "data5")

    // Invalidating the cache then clears it
    cache.invalidate("key1")
    cache.invalidate("key2")

    // Assert the size of the cache is correct
    assertCacheSize(cache, 3)

    // Assert the invalidated key is gone
    assertCacheIsEmpty(cache, "key1")
    assertCacheIsEmpty(cache, "key2")

    // Assert that other keys still exist
    assertCacheSingleValue(cache, "key3", "data3")
    assertCacheSingleValue(cache, "key4", "data4")
    assertCacheSingleValue(cache, "key5", "data5")
  }

  /**
   * When the cache is filled, old entries are evicted
   */
  @Test
  fun `MemoryCacheBehaviorTest size is bounded`() {
    val cache = createCache("size bound", 30, maxSize = 2)
    // Putting data into the cache fills it
    cache.add(
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // So it returns data on query
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // Assert the size of the cache is correct
    assertCacheSize(cache, 1)

    // Put some more data
    cache.add("key1", "data1")

    // Assert the size of the cache is correct
    assertCacheSize(cache, 2)

    // Put some more data
    cache.add("key2", "data2")

    // Assert the size of the cache is correctly bounded
    assertCacheSize(cache, 2)
  }

  /**
   * Once the time expires, cache will not expose its data even if it has some
   */
  @Test
  fun `MemoryCacheBehaviorTest times out`() {
    val cache = createCache("times out", 1)

    cache.add(
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT
    )

    // After time out passes, cache does not return value
    Observable.just("")
        .delay(2, SECONDS)
        .startNow()
        .blockingSubscribe {
          assertCacheIsEmpty(
              cache,
              DEFAULT_KEY
          )
        }
  }

  /**
   * Adding multiple elements will track all of them internally until it is invalidated
   */
  @Test
  fun `MemoryCacheBehaviorTest tracks history`() {
    val cache = createCache("tracks history", 30)
    val expect1 = "Hello"
    val expect2 = "World"

    cache.add(DEFAULT_KEY, expect1)
    assertCacheSingleValue(
        cache,
        DEFAULT_KEY, expect1
    )

    cache.add(DEFAULT_KEY, expect2)
    assertCacheValues(
        cache,
        DEFAULT_KEY, expect1, expect2
    )
  }

  /**
   * Calling put in the middle of a stream from the cache does not modify the output of the stream
   */
  @Test
  fun `MemoryCacheBehaviorTest put does not modify stream`() {
    val cache = createCache("put does not modify", 30)
    val extraPuts = "Extra"
    val expect1 = "Testing"
    val expect2 = "Puts"

    cache.add(DEFAULT_KEY, DEFAULT_EXPECT)
    cache.get(DEFAULT_KEY) { it as String }
        .doOnSubscribe { cache.add(DEFAULT_KEY, expect1) }
        .startNow()
        .test()
        // The put happens after the cache is accessed, so it should not appear
        .assertValue(DEFAULT_EXPECT)
        .assertValueCount(1)
        .assertComplete()

    cache.add(DEFAULT_KEY, extraPuts)
    cache.get(DEFAULT_KEY) { it as String }
        .doOnSubscribe { cache.add(DEFAULT_KEY, expect2) }
        .startNow()
        .test()
        // The put happens after the cache is accessed, so it should not appear
        // but the first access plus the first put should
        .assertValueSequence(arrayListOf(DEFAULT_EXPECT, expect1, extraPuts))
        .assertValueCount(3)
        .assertComplete()

    // Finally, everything should be in cache on next get
    assertCacheValues(
        cache,
        DEFAULT_KEY,
        DEFAULT_EXPECT, expect1, extraPuts, expect2
    )
  }

  /**
   * Calling get with the wrong key data type kills the cache
   */
  @Test
  fun `MemoryCacheBehaviorTest get with wrong key type kills cache`() {

    val cache = createCache("wrong key kills cache", 30)

    // Cache is valid with data
    cache.add(DEFAULT_KEY, DEFAULT_EXPECT)
    assertCacheSingleValue(cache, DEFAULT_KEY, DEFAULT_EXPECT)

    // Calling get with wrong expected type kills cache
    cache.get(DEFAULT_KEY) { it as Int }
        .startNow()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete()

    // Once bad key is requested, cache is blown away
    assertCacheIsEmpty(cache, DEFAULT_KEY)
  }

  companion object {

    private const val DEFAULT_KEY = "example key"
    private const val DEFAULT_EXPECT = "Hello World"
  }
}

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
import com.popinnow.android.repo.RepoBuilder
import com.popinnow.android.repo.impl.MemoryCacheImpl
import com.popinnow.android.repo.newRepoBuilder
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(MockitoJUnitRunner::class)
class RepoBehaviorTest {

  @CheckResult
  private inline fun builder(): RepoBuilder {
    return newRepoBuilder().debug(true)
        .scheduler(DEFAULT_SCHEDULER)
  }

  @Test
  fun `RepoBehavior Observable no-cache simple get`() {
    val repo = builder().build()

    repo.observe(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (no cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable memory cache simple get`() {
    val memoryCache = MemoryCacheImpl(true, 30, SECONDS, 10)
    val repo = builder()
        .memoryCache(memoryCache)
        .build()

    // Juice the memory cache
    DEFAULT_OBSERVABLE_CACHE_EXPECT.forEach { memoryCache.add(DEFAULT_OBSERVABLE_KEY, it) }

    repo.observe(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_CACHE_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable get fills caches`() {
    val repo = builder()
        .memoryCache()
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    repo.observe(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable cached results returned before upstream`() {
    val repo = builder()
        .memoryCache()
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    repo.observe(false, DEFAULT_OBSERVABLE_KEY) { Observable.fromArray("New", "Data") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + arrayListOf("New", "Data"))
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable only previous cached result returned`() {
    val repo = builder()
        .memoryCache()
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    val data = arrayListOf(
        "New", "Data", "But", "I", "Wonder", "How", "Long", "Its", "Going", "To", "Be", "Before",
        "We", "Start", "Running", "Out", "of", "Memory"
    )
    repo.observe(false, DEFAULT_OBSERVABLE_KEY) { Observable.fromIterable(data) }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + data)
        .assertComplete()

    repo.observe(false, DEFAULT_OBSERVABLE_KEY) {
      Observable.fromIterable(DEFAULT_OBSERVABLE_PERSIST_EXPECT)
    }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(data + DEFAULT_OBSERVABLE_PERSIST_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Single no-cache simple get`() {
    val repo = builder().build()

    repo.get(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache or upstream
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Single memory cache simple get`() {
    val memoryCache = MemoryCacheImpl(true, 30, SECONDS, 10)
    val repo = builder()
        .memoryCache(memoryCache)
        .build()

    // Juice the memory cache
    memoryCache.add(DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_CACHE_EXPECT)

    repo.get<List<String>>(false, DEFAULT_SINGLE_KEY) {
      throw AssertionError("Upstream should be avoided")
    }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache or upstream
        .assertValue(DEFAULT_SINGLE_CACHE_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Single get fills caches`() {
    val repo = builder()
        .memoryCache()
        .build()

    repo.get(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()

    repo.get<List<String>>(false, DEFAULT_SINGLE_KEY) {
      throw AssertionError("Upstream should be avoided")
    }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  companion object {

    private val DEFAULT_SCHEDULER = Schedulers.trampoline()

    private const val DEFAULT_OBSERVABLE_KEY = "example-key-observe"
    private val DEFAULT_OBSERVABLE_CACHE_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_OBSERVABLE_PERSIST_EXPECT = arrayListOf("Persister", "Defaults")
    private val DEFAULT_OBSERVABLE_FETCH_EXPECT = arrayListOf("Upstream", "Payload")
    private val DEFAULT_OBSERVABLE_UPSTREAM = { _: String ->
      Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT)
    }

    private const val DEFAULT_SINGLE_KEY = "example-key"
    private val DEFAULT_SINGLE_CACHE_EXPECT = listOf("Hello", "World")
    private val DEFAULT_SINGLE_FETCH_EXPECT = listOf("Upstream", "Payload")
    private val DEFAULT_SINGLE_UPSTREAM = { _: String -> Single.just(DEFAULT_SINGLE_FETCH_EXPECT) }
  }
}


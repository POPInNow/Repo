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

import androidx.annotation.CheckResult
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Repo
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
  private fun <T : Any> builder(debug: String): RepoBuilder<T> {
    return newRepoBuilder<T>().debug(debug)
        .scheduler(DEFAULT_SCHEDULER)
  }

  @Test
  fun `RepoBehavior Observable no-cache simple get`() {
    val repo = builder<String>("observable no-cache simple get").build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (no cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable memory cache simple get`() {
    val debug = "observable cache simple get"
    val memoryCache = MemoryCacheImpl<String>(debug, 30, SECONDS)
    val repo = builder<String>(debug)
        .memoryCache(memoryCache)
        .build()

    observableSimpleGet(repo, memoryCache)
  }

  @Test
  fun `RepoBehavior Observable memory cache with persister simple get`() {
    val debug = "observable cache with persister simple get"
    val memoryCache = MemoryCacheImpl<String>(debug, 30, SECONDS)
    val repo = builder<String>(debug)
        .memoryCache(memoryCache)
        .persister(
            createTempFile(prefix = "repo_test"),
            TestPersisterMapper()
        )
        .build()

    observableSimpleGet(repo, memoryCache)
  }

  private fun observableSimpleGet(
    repo: Repo<String>,
    memoryCache: MemoryCache<String>
  ) {
    // Juice the memory cache
    DEFAULT_OBSERVABLE_CACHE_EXPECT.forEach { memoryCache.add(it) }

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_CACHE_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable get fills caches`() {
    val repo = builder<String>("observable get fills cache")
        .memoryCache()
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable cached results returned before upstream`() {
    val repo = builder<String>("observable cache before upstream")
        .memoryCache()
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    repo.observe(false) { Observable.fromArray("New", "Data") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + arrayListOf("New", "Data"))
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable only previous cached result returned`() {
    val repo = builder<String>("observable only previous cache returns")
        .memoryCache()
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
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
    repo.observe(false) { Observable.fromIterable(data) }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + data)
        .assertComplete()

    repo.observe(false) { Observable.fromIterable(DEFAULT_OBSERVABLE_CACHE_EXPECT) }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(data + DEFAULT_OBSERVABLE_CACHE_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Single no-cache simple get`() {
    val repo = builder<List<String>>("single no-cache simple get").build()

    repo.get(false, DEFAULT_SINGLE_UPSTREAM)
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
    val debug = "single cache simple get"
    val memoryCache = MemoryCacheImpl<List<String>>(debug, 30, SECONDS)
    val repo = builder<List<String>>(debug)
        .memoryCache(memoryCache)
        .build()

    // Juice the memory cache
    memoryCache.add(DEFAULT_SINGLE_CACHE_EXPECT)

    repo.get(false) { throw AssertionError("Upstream should be avoided") }
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
    val repo = builder<List<String>>("single get fills caches")
        .memoryCache()
        .build()

    repo.get(false, DEFAULT_SINGLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()

    repo.get(false) { throw AssertionError("Upstream should be avoided") }
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

    private val DEFAULT_OBSERVABLE_CACHE_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_OBSERVABLE_PERSIST_EXPECT = arrayListOf("Persister", "Defaults")
    private val DEFAULT_OBSERVABLE_FETCH_EXPECT = arrayListOf("Upstream", "Payload")
    private val DEFAULT_OBSERVABLE_UPSTREAM = {
      Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT)
    }

    private val DEFAULT_SINGLE_CACHE_EXPECT = listOf("Hello", "World")
    private val DEFAULT_SINGLE_FETCH_EXPECT = listOf("Upstream", "Payload")
    private val DEFAULT_SINGLE_UPSTREAM = { Single.just(DEFAULT_SINGLE_FETCH_EXPECT) }
  }
}


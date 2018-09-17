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
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit.SECONDS

@Deprecated("This test will eventually be removed in favor of Repo and RepoBehaviorTest")
@RunWith(MockitoJUnitRunner::class)
class ObservableRepoBehaviorTest {

  @CheckResult
  private fun builder(): RepoBuilder {
    return newRepoBuilder().debug(true)
        .scheduler(DEFAULT_SCHEDULER)
  }

  @Test
  fun `ObservableRepoBehavior no-cache simple get`() {
    val repo = builder()
        .buildObservable<String>()

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (no cache)
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `ObservableRepoBehavior memory cache simple get`() {
    val memoryCache = MemoryCacheImpl(true, 30, SECONDS, 10)
    val repo = builder()
        .memoryCache(memoryCache)
        .buildObservable<String>()

    // Juice the memory cache
    DEFAULT_CACHE_EXPECT.forEach { memoryCache.add(DEFAULT_KEY, it) }

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_CACHE_EXPECT + DEFAULT_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `ObservableRepoBehavior get fills caches`() {
    val repo = builder()
        .memoryCache()
        .buildObservable<String>()

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
        .assertComplete()

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_FETCH_EXPECT + DEFAULT_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `ObservableRepoBehavior cached results returned before upstream`() {
    val repo = builder()
        .memoryCache()
        .buildObservable<String>()

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
        .assertComplete()

    repo.get(false, DEFAULT_KEY) { Observable.fromArray("New", "Data") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_FETCH_EXPECT + arrayListOf("New", "Data"))
        .assertComplete()
  }

  @Test
  fun `ObservableRepoBehavior only previous cached result returned`() {
    val repo = builder()
        .memoryCache()
        .buildObservable<String>()

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
        .assertComplete()

    val data = arrayListOf(
        "New", "Data", "But", "I", "Wonder", "How", "Long", "Its", "Going", "To", "Be", "Before",
        "We", "Start", "Running", "Out", "of", "Memory"
    )
    repo.get(false, DEFAULT_KEY) { Observable.fromIterable(data) }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_FETCH_EXPECT + data)
        .assertComplete()

    repo.get(false, DEFAULT_KEY) { Observable.fromIterable(DEFAULT_PERSIST_EXPECT) }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(data + DEFAULT_PERSIST_EXPECT)
        .assertComplete()
  }

  companion object {

    private const val DEFAULT_KEY = "example-key"
    private val DEFAULT_SCHEDULER = Schedulers.trampoline()
    private val DEFAULT_CACHE_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_PERSIST_EXPECT = arrayListOf("Persister", "Defaults")
    private val DEFAULT_FETCH_EXPECT = arrayListOf("Upstream", "Payload")
    private val DEFAULT_UPSTREAM = { _: String -> Observable.fromIterable(DEFAULT_FETCH_EXPECT) }
  }
}


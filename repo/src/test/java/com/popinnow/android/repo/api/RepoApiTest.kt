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

package com.popinnow.android.repo.api

import android.support.annotation.CheckResult
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.impl.RepoImpl
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RepoApiTest {

  @Mock lateinit var fetcher: Fetcher
  @Mock lateinit var memoryCache: MemoryCache
  @Mock lateinit var persister: Persister
  private lateinit var validator: MockRepoOrderValidator

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    validator = MockRepoOrderValidator(memoryCache, persister, fetcher)
  }

  @CheckResult
  private fun createRepo(debug: String): RepoImpl {
    return RepoImpl(fetcher, memoryCache, persister, DEFAULT_SCHEDULER, debug)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `RepoApi Observable memory hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.fromIterable(DEFAULT_OBSERVABLE_CACHE_EXPECT),
        mapper
    )
    validator.onVisitPersisterReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.error<String>(AssertionError("Persister should be missed")),
        mapper
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    // Testing function
    createRepo("observable memory hit priority")
        .testingObserve(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM, mapper)
        .startNow()
        .test()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_CACHE_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(!validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `RepoApi Observable busted memory hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    createRepo("observable busted memory hit priority")
        .testingObserve(true, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM, mapper)
        .startNow()
        .test()
        // Cache then upstream (but cache is busted)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When data is returned from the persister, memory cache is hit but empty and upstream is called after
   */
  @Test
  fun `RepoApi Observable persister hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.fromIterable(DEFAULT_OBSERVABLE_PERSIST_EXPECT),
        mapper
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    createRepo("observable persister hit priority")
        .testingObserve(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM, mapper)
        .startNow()
        .test()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_PERSIST_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When data is returned from the persister, memory cache is hit but empty and upstream is called after
   */
  @Test
  fun `RepoApi Observable busted persister hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    createRepo("observable busted persister hit priority")
        .testingObserve(true, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM, mapper)
        .startNow()
        .test()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When no caching layer exists, we just hit the fetcher, but we visit everyone first
   */
  @Test
  fun `RepoApi Observable fetcher delivers even without caching layer`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    createRepo("observable fetcher delivers without cache")
        .testingObserve(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM, mapper)
        .startNow()
        .test()
        // Cache then upstream (but no caches)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When no caching layer exists, we just hit the fetcher, but we visit everyone first
   */
  @Test
  fun `RepoApi Observable busted fetcher delivers even without caching layer`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    createRepo("observable busted fetcher delivers without cache")
        .testingObserve(true, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM, mapper)
        .startNow()
        .test()
        // Cache then upstream (but no caches)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `RepoApi Single memory hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(
        DEFAULT_SINGLE_KEY,
        Observable.just(DEFAULT_SINGLE_CACHE_EXPECT),
        mapper
    )
    validator.onVisitPersisterReturn(
        DEFAULT_SINGLE_KEY,
        Observable.error<String>(AssertionError("Persister should be missed")),
        mapper
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY,
        Observable.error<String>(AssertionError("Fetcher should be missed")),
        DEFAULT_SCHEDULER,
        DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    createRepo("single memory hit priority")
        .testingGet(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE, mapper)
        .startNow()
        .test()
        // Cache or upstream
        .assertValue(DEFAULT_SINGLE_CACHE_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(!validator.persisterVisited)
    assert(!validator.upstreamVisited)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `RepoApi Single busted memory hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY,
        Observable.just(DEFAULT_SINGLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    createRepo("single busted memory hit priority")
        .testingGet(true, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE, mapper)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When data is returned from the persister, memory cache is hit but empty and upstream is called after
   */
  @Test
  fun `RepoApi Single persister hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(
        DEFAULT_SINGLE_KEY,
        Observable.just(DEFAULT_SINGLE_PERSIST_EXPECT),
        mapper
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY,
        Observable.error<String>(AssertionError("Fetcher should be missed")),
        DEFAULT_SCHEDULER,
        DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    createRepo("single persister hit priority")
        .testingGet(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE, mapper)
        .startNow()
        .test()
        // Cache or upstream
        .assertValue(DEFAULT_SINGLE_PERSIST_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(!validator.upstreamVisited)
  }

  /**
   * When data is returned from the persister, memory cache is hit but empty and upstream is called after
   */
  @Test
  fun `RepoApi Single busted persister hit takes priority`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY,
        Observable.just(DEFAULT_SINGLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    createRepo("single busted persister hit priority")
        .testingGet(true, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE, mapper)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When no caching layer exists, we just hit the fetcher, but we visit everyone first
   */
  @Test
  fun `RepoApi Single fetcher delivers even without caching layer`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    createRepo("single fetcher delivers")
        .testingGet(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE, mapper)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  /**
   * When no caching layer exists, we just hit the fetcher, but we visit everyone first
   */
  @Test
  fun `RepoApi Single busted fetcher delivers even without caching layer`() {
    val mapper = generateMapper<String>()
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty(), mapper)
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    createRepo("single busted fetcher delivers")
        .testingGet(true, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE, mapper)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  companion object {

    private val DEFAULT_SCHEDULER = Schedulers.trampoline()

    private const val DEFAULT_OBSERVABLE_KEY = "example-key-observe"
    private val DEFAULT_OBSERVABLE_CACHE_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_OBSERVABLE_PERSIST_EXPECT = arrayListOf("Persister", "Defaults")
    private val DEFAULT_OBSERVABLE_FETCH_EXPECT = arrayListOf("Upstream", "Payload")
    private val DEFAULT_OBSERVABLE_UPSTREAM = {
      Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT)
    }

    private const val DEFAULT_SINGLE_KEY = "example-key-get"
    private const val DEFAULT_SINGLE_CACHE_EXPECT = "Cache"
    private const val DEFAULT_SINGLE_PERSIST_EXPECT = "Persister"
    private const val DEFAULT_SINGLE_FETCH_EXPECT = "Upstream"
    private val DEFAULT_SINGLE_UPSTREAM = { Single.just(DEFAULT_SINGLE_FETCH_EXPECT) }
    private val DEFAULT_SINGLE_UPSTREAM_OBSERVABLE = {
      DEFAULT_SINGLE_UPSTREAM().toObservable()
    }

    @CheckResult
    private fun <T : Any> generateMapper(): (Any) -> T {
      return mapper@{ item: Any ->
        @Suppress("UNCHECKED_CAST")
        return@mapper item as T
      }
    }
  }
}


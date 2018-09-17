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

  @Mock lateinit var fetcher: Fetcher<String>
  @Mock lateinit var memoryCache: MemoryCache<String>
  @Mock lateinit var persister: Persister<String>
  private lateinit var repo: RepoImpl<String>
  private lateinit var validator: MockRepoOrderValidator<String>

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    repo = RepoImpl(fetcher, memoryCache, persister, DEFAULT_SCHEDULER, true)
    validator = MockRepoOrderValidator(memoryCache, persister, fetcher)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `RepoApi Observable memory hit takes priority`() {
    validator.onVisitMemoryReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_CACHE_EXPECT)
    )
    validator.onVisitPersisterReturn(
        DEFAULT_OBSERVABLE_KEY,
        Observable.error<String>(AssertionError("Persister should be missed"))
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    repo.get(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
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
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    repo.get(true, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
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
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_PERSIST_EXPECT)
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    repo.get(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
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
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    repo.get(true, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
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
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    repo.get(false, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
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
    validator.onVisitMemoryReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_OBSERVABLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_OBSERVABLE_KEY, Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER,
        DEFAULT_OBSERVABLE_UPSTREAM
    )

    repo.get(true, DEFAULT_OBSERVABLE_KEY, DEFAULT_OBSERVABLE_UPSTREAM)
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
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_CACHE_EXPECT))
    validator.onVisitPersisterReturn(
        DEFAULT_SINGLE_KEY, Observable.error<String>(AssertionError("Persister should be missed"))
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.error<String>(AssertionError("Fetcher should be missed")),
        DEFAULT_SCHEDULER, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    repo.testingGet(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE)
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
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    repo.testingGet(true, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE)
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
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(
        DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_PERSIST_EXPECT)
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.error<String>(AssertionError("Fetcher should be missed")),
        DEFAULT_SCHEDULER, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    repo.testingGet(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE)
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
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    repo.testingGet(true, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE)
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
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    repo.testingGet(false, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE)
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
    validator.onVisitMemoryReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_SINGLE_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_SINGLE_KEY, Observable.just(DEFAULT_SINGLE_FETCH_EXPECT),
        DEFAULT_SCHEDULER, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    repo.testingGet(true, DEFAULT_SINGLE_KEY, DEFAULT_SINGLE_UPSTREAM_OBSERVABLE)
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

    private const val DEFAULT_OBSERVABLE_KEY = "example-key"
    private val DEFAULT_OBSERVABLE_CACHE_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_OBSERVABLE_PERSIST_EXPECT = arrayListOf("Persister", "Defaults")
    private val DEFAULT_OBSERVABLE_FETCH_EXPECT = arrayListOf("Upstream", "Payload")
    private val DEFAULT_OBSERVABLE_UPSTREAM = { _: String ->
      Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT)
    }

    private const val DEFAULT_SINGLE_KEY = "example-key"
    private const val DEFAULT_SINGLE_CACHE_EXPECT = "Cache"
    private const val DEFAULT_SINGLE_PERSIST_EXPECT = "Persister"
    private const val DEFAULT_SINGLE_FETCH_EXPECT = "Upstream"
    private val DEFAULT_SINGLE_UPSTREAM = { _: String -> Single.just(DEFAULT_SINGLE_FETCH_EXPECT) }
    private val DEFAULT_SINGLE_UPSTREAM_OBSERVABLE = { key: String ->
      DEFAULT_SINGLE_UPSTREAM(key).toObservable()
    }
  }
}


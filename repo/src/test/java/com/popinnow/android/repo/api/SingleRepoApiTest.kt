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
import com.popinnow.android.repo.impl.SingleRepoImpl
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
class SingleRepoApiTest {

  @Mock lateinit var fetcher: Fetcher<String>
  @Mock lateinit var memoryCache: MemoryCache<String>
  @Mock lateinit var persister: Persister<String>
  private lateinit var singleRepo: SingleRepoImpl<String>
  private lateinit var validator: MockRepoOrderValidator<String>

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    singleRepo = SingleRepoImpl(fetcher, memoryCache, persister, DEFAULT_SCHEDULER, true)
    validator = MockRepoOrderValidator(memoryCache, persister, fetcher)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `SingleRepoApi memory hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.just(DEFAULT_CACHE_EXPECT))
    validator.onVisitPersisterReturn(
        DEFAULT_KEY, Observable.error<String>(AssertionError("Persister should be missed"))
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.error<String>(AssertionError("Fetcher should be missed")),
        DEFAULT_SCHEDULER, DEFAULT_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream
        .assertValue(DEFAULT_CACHE_EXPECT)
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
  fun `SingleRepoApi busted memory hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.just(DEFAULT_FETCH_EXPECT),
        DEFAULT_SCHEDULER, DEFAULT_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(true, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_FETCH_EXPECT)
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
  fun `SingleRepoApi persister hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.just(DEFAULT_PERSIST_EXPECT))
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.error<String>(AssertionError("Fetcher should be missed")),
        DEFAULT_SCHEDULER, DEFAULT_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream
        .assertValue(DEFAULT_PERSIST_EXPECT)
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
  fun `SingleRepoApi busted persister hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.just(DEFAULT_FETCH_EXPECT),
        DEFAULT_SCHEDULER, DEFAULT_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(true, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_FETCH_EXPECT)
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
  fun `SingleRepoApi fetcher delivers even without caching layer`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.just(DEFAULT_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_FETCH_EXPECT)
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
  fun `SingleRepoApi busted fetcher delivers even without caching layer`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.just(DEFAULT_FETCH_EXPECT),
        DEFAULT_SCHEDULER, DEFAULT_UPSTREAM_OBSERVABLE
    )

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(true, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
  }

  companion object {

    private const val DEFAULT_KEY = "example-key"
    private const val DEFAULT_CACHE_EXPECT = "Cache"
    private const val DEFAULT_PERSIST_EXPECT = "Persister"
    private const val DEFAULT_FETCH_EXPECT = "Upstream"
    private val DEFAULT_SCHEDULER = Schedulers.trampoline()
    private val DEFAULT_UPSTREAM = { _: String -> Single.just(DEFAULT_FETCH_EXPECT) }
    private val DEFAULT_UPSTREAM_OBSERVABLE = { s: String -> DEFAULT_UPSTREAM(s).toObservable() }
  }
}


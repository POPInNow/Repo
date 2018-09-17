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
import com.popinnow.android.repo.impl.ObservableRepoImpl
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@Deprecated("This test will eventually be removed in favor of Repo and RepoApiTest")
@RunWith(MockitoJUnitRunner::class)
class ObservableRepoApiTest {

  @Mock lateinit var fetcher: Fetcher
  @Mock lateinit var memoryCache: MemoryCache
  @Mock lateinit var persister: Persister
  private lateinit var validator: MockRepoOrderValidator
  private lateinit var observableRepo: ObservableRepoImpl<String>

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    observableRepo = ObservableRepoImpl(fetcher, memoryCache, persister, DEFAULT_SCHEDULER, true)
    validator = MockRepoOrderValidator(memoryCache, persister, fetcher)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `ObservableRepoApi memory hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.fromIterable(DEFAULT_CACHE_EXPECT))
    validator.onVisitPersisterReturn(
        DEFAULT_KEY, Observable.error<String>(AssertionError("Persister should be missed"))
    )
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.fromIterable(DEFAULT_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_UPSTREAM
    )

    observableRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        // Cache then upstream
        .assertValueSequence(DEFAULT_CACHE_EXPECT + DEFAULT_FETCH_EXPECT)
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
  fun `ObservableRepoApi busted memory hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.fromIterable(DEFAULT_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_UPSTREAM
    )

    observableRepo.get(true, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        // Cache then upstream (but cache is busted)
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
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
  fun `ObservableRepoApi persister hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.fromIterable(DEFAULT_PERSIST_EXPECT))
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.fromIterable(DEFAULT_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_UPSTREAM
    )

    observableRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        // Cache then upstream
        .assertValueSequence(DEFAULT_PERSIST_EXPECT + DEFAULT_FETCH_EXPECT)
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
  fun `ObservableRepoApi busted persister hit takes priority`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.fromIterable(DEFAULT_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_UPSTREAM
    )

    observableRepo.get(true, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        // Cache then upstream
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
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
  fun `ObservableRepoApi fetcher delivers even without caching layer`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.fromIterable(DEFAULT_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_UPSTREAM
    )

    observableRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        // Cache then upstream (but no caches)
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
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
  fun `ObservableRepoApi busted fetcher delivers even without caching layer`() {
    validator.onVisitMemoryReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitPersisterReturn(DEFAULT_KEY, Observable.empty())
    validator.onVisitUpstreamReturn(
        DEFAULT_KEY, Observable.fromIterable(DEFAULT_FETCH_EXPECT), DEFAULT_SCHEDULER,
        DEFAULT_UPSTREAM
    )

    observableRepo.get(true, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        // Cache then upstream (but no caches)
        .assertValueSequence(DEFAULT_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(validator.memoryVisited)
    assert(validator.persisterVisited)
    assert(validator.upstreamVisited)
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


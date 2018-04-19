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

import com.popinnow.android.repo.Counter
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.SingleRepo
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
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(MockitoJUnitRunner::class)
class SingleRepoApiTest {

  @Mock lateinit var fetcher: Fetcher<String>
  @Mock lateinit var memoryCache: MemoryCache<String>
  @Mock lateinit var persister: Persister<String>
  private lateinit var singleRepo: SingleRepoImpl<String>

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    singleRepo = SingleRepoImpl(fetcher, memoryCache, persister, DEFAULT_SCHEDULER, true)
  }

  /**
   * When data is returned from the cache, persister is missed and upstream is called after
   */
  @Test
  fun `SingleRepoApi memory hit takes priority`() {
    Mocks.whenever(memoryCache.get(DEFAULT_KEY))
        .thenReturn(Observable.just(DEFAULT_CACHE_EXPECT))

    Mocks.whenever(fetcher.fetch(DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE, DEFAULT_SCHEDULER))
        .thenReturn(Observable.just(DEFAULT_FETCH_EXPECT))

    Mocks.whenever(persister.read(DEFAULT_KEY))
        .thenReturn(Observable.error(AssertionError("Persister should be missed")))

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream
        .assertValue(DEFAULT_CACHE_EXPECT)
        .assertComplete()
        .assertNoErrors()
  }

  /**
   * When data is returned from the persister, memory cache is hit but empty and upstream is called after
   */
  @Test
  fun `SingleRepoApi persister hit takes priority`() {
    val counter = Counter(0)
    Mocks.whenever(memoryCache.get(DEFAULT_KEY))
        .thenReturn(Observable.defer {
          ++counter.count
          return@defer Observable.empty<String>()
        })

    Mocks.whenever(fetcher.fetch(DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE, DEFAULT_SCHEDULER))
        .thenReturn(Observable.just(DEFAULT_FETCH_EXPECT))

    Mocks.whenever(persister.read(DEFAULT_KEY))
        .thenReturn(Observable.just(DEFAULT_PERSIST_EXPECT))

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream
        .assertValue(DEFAULT_PERSIST_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(counter.count == 1) { "MemoryCache was not visited first" }
  }

  /**
   * When no caching layer exists, we just hit the fetcher, but we visit everyone first
   */
  @Test
  fun `SingleRepoApi fetcher delivers even without caching layer`() {
    val counter = Counter(0)
    Mocks.whenever(memoryCache.get(DEFAULT_KEY))
        .thenReturn(Observable.defer {
          ++counter.count
          return@defer Observable.empty<String>()
        })

    Mocks.whenever(fetcher.fetch(DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE, DEFAULT_SCHEDULER))
        .thenReturn(Observable.just(DEFAULT_FETCH_EXPECT))

    Mocks.whenever(persister.read(DEFAULT_KEY))
        .thenReturn(Observable.defer {
          ++counter.count
          return@defer Observable.empty<String>()
        })

    // Use the internal method just to avoid the conversion from Single to Observable not triggering Mockito
    singleRepo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM_OBSERVABLE)
        .startNow()
        .test()
        // Cache or upstream (but no caches)
        .assertValue(DEFAULT_FETCH_EXPECT)
        .assertComplete()
        .assertNoErrors()

    assert(counter.count == 2) { "Caching layer was not visited first" }
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


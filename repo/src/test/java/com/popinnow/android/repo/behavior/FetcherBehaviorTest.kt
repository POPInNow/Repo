/*
 * Copyright (C) 2019 POP Inc.
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
import com.popinnow.android.repo.Counter
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.impl.FetcherImpl
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(MockitoJUnitRunner::class)
class FetcherBehaviorTest : BaseBehaviorTest() {

  @CheckResult
  private fun createFetcher(tag: String): Fetcher<String> {
    return FetcherImpl(tag)
  }

  private fun assertFetch(
    fetcher: Fetcher<String>,
    upstream: () -> Observable<String>,
    values: List<String>
  ) {
    fetcher.fetch(upstream, Schedulers.io())
        .startNow()
        .test()
        // Need await because the fetch runs in background
        .awaitDone(5, SECONDS)
        .assertNoErrors()
        .assertValueSequence(values)
        .assertComplete()
  }

  /**
   *  Does the fetcher return the data we ask for
   */
  @Test
  fun `FetcherBehavior simple get`() {
    val fetcher = createFetcher("simple get")
    assertFetch(fetcher, DEFAULT_UPSTREAM, DEFAULT_EXPECT)
  }

  /**
   *  Does the fetcher return the data we ask for
   */
  @Test
  fun `FetcherBehavior attaches in flight requests`() {
    val fetcher = createFetcher("attach in flight")

    val counter = Counter(0)

    val upstream = upstream@{
      ++counter.count
      return@upstream DEFAULT_DELAYED
    }

    val threads = ArrayList<Thread>()
    for (i in 0 until 100) {
      val runnable = Runnable { assertFetch(fetcher, upstream, DEFAULT_EXPECT) }
      threads.add(Thread(runnable))
    }

    for (thread in threads) {
      thread.start()
    }

    for (thread in threads) {
      thread.join()
    }

    assert(counter.count == 1) {
      "Upstream hit more than once! ${counter.count}"
    }
  }

  /**
   *  Does invalidating the fetcher cache not stop the actual upstream request
   */
  @Test
  fun `FetcherBehavior invalidateCache does not stop upstream`() {
    val fetcher = createFetcher("invalidate does not skip upstream")

    val counter = Counter(0)

    val upstream = upstream@{
      ++counter.count
      return@upstream DEFAULT_DELAYED
    }

    fun startFetch() {
      fetcher.fetch(upstream, Schedulers.io())
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.trampoline())
          .subscribe()
    }

    // Start a fetch, increment the counter once
    startFetch()

    // After a small delay (mid flight) invalidate the cache
    Thread.sleep(500)
    fetcher.clearCaches()

    // The next fetch on the same key will start a new request
    startFetch()
    Thread.sleep(2000)

    // After both have completed, we should have hit the upstream both times
    assert(counter.count == 2) { "Upstream accessed wrong number of times! ${counter.count}" }
  }

  /**
   *  Does invalidating the fetcher cache not stop the actual upstream request
   */
  @Test
  fun `FetcherBehavior invalidate stops upstream`() {
    val fetcher = createFetcher("invalidate stop upstream")

    val counter = Counter(0)

    val upstream = { DEFAULT_DELAYED.doOnNext { ++counter.count } }

    fun startFetch() {
      fetcher.fetch(upstream, Schedulers.io())
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.trampoline())
          .subscribe()
    }

    // Start a fetch,  don't increment the counter yet
    startFetch()

    // After a small delay (mid flight) invalidate the request
    Thread.sleep(500)
    fetcher.clearAll()

    // The next fetch on the same key will start a new request, and should finish which increments.
    startFetch()
    Thread.sleep(2000)

    // After both have completed, we should have hit the upstream only once and emitted two values
    assert(
        counter.count == DEFAULT_EXPECT.size
    ) { "Upstream accessed more than expected! ${counter.count}" }
  }

  companion object {

    private val DEFAULT_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_UPSTREAM = { Observable.fromIterable(DEFAULT_EXPECT) }
    private val DEFAULT_DELAYED = Observable.just("")
        .delay(1, SECONDS)
        .flatMap { Observable.fromIterable(DEFAULT_EXPECT) }
  }

}

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
import com.popinnow.android.repo.impl.Logger
import com.popinnow.android.repo.logger.SystemLogger
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS

class FetcherBehaviorTest : BaseBehaviorTest() {

  @CheckResult
  private fun createFetcher(tag: String): Fetcher<String> {
    return FetcherImpl(Logger.create(tag, true, SystemLogger))
  }

  private fun assertFetch(
    fetcher: Fetcher<String>,
    upstream: () -> Observable<String>,
    values: List<String>
  ) {
    fetcher.fetch(Schedulers.io(), upstream)
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
  fun `FetcherBehavior clear does not stop upstream`() {
    val fetcher = createFetcher("clear does not skip upstream")

    val counter = Counter(0)

    val upstream = upstream@{
      ++counter.count
      return@upstream DEFAULT_DELAYED
    }

    fun startFetch() {
      fetcher.fetch(Schedulers.io(), upstream)
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.trampoline())
          .subscribe()
    }

    // Start a fetch, increment the counter once
    startFetch()

    // After a small delay (mid flight) invalidate the cache
    Thread.sleep(500)
    fetcher.clear()

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
  fun `FetcherBehavior cancel stops upstream`() {
    val fetcher = createFetcher("cancel stop upstream")

    val completed = Counter(0)
    val emitted = Counter(0)

    val upstream = {
      DEFAULT_DELAYED
          .doOnNext { ++emitted.count }
          .doOnComplete { ++completed.count }
    }

    fun startFetch() {
      fetcher.fetch(Schedulers.io(), upstream)
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.trampoline())
          .subscribe()
    }

    // Start a fetch,  don't increment the counter yet
    startFetch()

    // After a small delay (mid flight) cancel
    Thread.sleep(500)
    fetcher.cancel()

    // We should have cancelled the upstream so it should never complete
    Thread.sleep(2000)

    // After enough wait, we should have no emits and no completed
    assert(emitted.count == 0) {
      "Upstream emit accessed more than expected! ${emitted.count}"
    }

    assert(completed.count == 0) {
      "Upstream complete accessed more than expected! ${completed.count}"
    }
  }

  companion object {

    private val DEFAULT_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_UPSTREAM = { Observable.fromIterable(DEFAULT_EXPECT) }
    private val DEFAULT_DELAYED = Observable.just("")
        .delay(1, SECONDS)
        .flatMap { Observable.fromIterable(DEFAULT_EXPECT) }
  }

}

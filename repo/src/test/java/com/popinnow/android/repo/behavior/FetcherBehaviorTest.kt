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
import com.popinnow.android.repo.Counter
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.impl.FetcherImpl
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(MockitoJUnitRunner::class)
class FetcherBehaviorTest {

  @CheckResult
  private fun createFetcher(): Fetcher {
    return FetcherImpl(true)
  }

  private fun assertFetch(
    fetcher: Fetcher,
    key: String,
    upstream: (String) -> Observable<String>,
    values: List<String>
  ) {
    fetcher.fetch(key, upstream, Schedulers.io())
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
    val fetcher = createFetcher()
    assertFetch(fetcher, DEFAULT_KEY, DEFAULT_UPSTREAM, DEFAULT_EXPECT)
  }

  /**
   *  Does the fetcher return the data we ask for
   */
  @Test
  fun `FetcherBehavior attaches in flight requests`() {
    val fetcher = createFetcher()

    val counter = Counter(0)

    val upstream = { _: String ->
      ++counter.count
      DEFAULT_DELAYED
    }

    // Set up two basic threads to launch parallel requests
    val r1 = Runnable {
      assertFetch(fetcher, DEFAULT_KEY, upstream, DEFAULT_EXPECT)
    }

    val r2 = Runnable {
      assertFetch(fetcher, DEFAULT_KEY, upstream, DEFAULT_EXPECT)
    }

    val t1 = Thread(r1)
    val t2 = Thread(r2)

    var error1: Throwable? = null
    var error2: Throwable? = null

    t1.setUncaughtExceptionHandler { _, e -> error1 = e }
    t2.setUncaughtExceptionHandler { _, e -> error2 = e }

    // Two threads launched at the same time will only hit the upstream once while a request is alive
    t1.start()
    t2.start()

    t1.join()
    t2.join()

    // Fail the test if any errors occur on the threads
    error1.also {
      if (it != null) {
        throw it
      }
    }

    error2.also {
      if (it != null) {
        throw it
      }
    }

    assert(counter.count == 1) { "Upstream accessed directly more than once! ${counter.count}" }
  }

  /**
   *  Does invalidating the fetcher cache not stop the actual upstream request
   */
  @Test
  fun `FetcherBehavior invalidateCache does not stop upstream`() {
    val fetcher = createFetcher()

    val counter = Counter(0)

    val upstream = { _: String ->
      ++counter.count
      DEFAULT_DELAYED
    }

    fun startFetch() {
      fetcher.fetch(DEFAULT_KEY, upstream, Schedulers.io())
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.trampoline())
          .subscribe()
    }

    // Start a fetch, increment the counter once
    startFetch()

    // After a small delay (mid flight) invalidate the cache
    Thread.sleep(500)
    fetcher.invalidateCaches(DEFAULT_KEY)

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
    val fetcher = createFetcher()

    val counter = Counter(0)

    val upstream = { _: String ->
      DEFAULT_DELAYED.doOnNext { ++counter.count }
    }

    fun startFetch() {
      fetcher.fetch(DEFAULT_KEY, upstream, Schedulers.io())
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.trampoline())
          .subscribe()
    }

    // Start a fetch,  don't increment the counter yet
    startFetch()

    // After a small delay (mid flight) invalidate the request
    Thread.sleep(500)
    fetcher.invalidate(DEFAULT_KEY)

    // The next fetch on the same key will start a new request, and should finish which increments.
    startFetch()
    Thread.sleep(2000)

    // After both have completed, we should have hit the upstream only once and emitted two values
    assert(
        counter.count == DEFAULT_EXPECT.size
    ) { "Upstream accessed more than expected! ${counter.count}" }
  }

  /**
   *  Does the fetcher keep unique keys
   */
  @Test
  fun `FetcherBehavior keeps unique keys`() {
    val fetcher = createFetcher()

    // Two accesses on the fetcher in parallel do not clobber each others keys
    Observable.zip(
        fetcher.fetch("key1", { _: String -> Observable.just("This", "is") }, Schedulers.io()),
        fetcher.fetch("key2", { _: String -> Observable.just("a", "Test") }, Schedulers.io()),
        BiFunction<String, String, String> { t1, t2 -> t1 + t2 })
        .test()
        .awaitDone(5, SECONDS)
        .assertNoErrors()
        .assertValueSequence(arrayListOf("Thisa", "isTest"))
        .assertComplete()
  }

  companion object {

    private const val DEFAULT_KEY = "example-key"
    private val DEFAULT_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_UPSTREAM = { _: String -> Observable.fromIterable(DEFAULT_EXPECT) }
    private val DEFAULT_DELAYED = Observable.just("")
        .delay(1, SECONDS)
        .flatMap { Observable.fromIterable(DEFAULT_EXPECT) }
  }

}

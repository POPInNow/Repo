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

package com.popinnow.android.repo.impl

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Fetcher
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.ReplaySubject

/**
 * Default implementation of the SingleFetcher interface
 *
 * The SingleFetcher will memoryCache any in flight upstream requests until they have completed.
 */
internal class FetcherImpl<T : Any> internal constructor(
  debug: String
) : Fetcher<T> {

  private val logger by lazy { Logger("Fetcher[$debug]") }

  private val lock = Any()
  @Volatile private var inFlight: Observable<T>? = null
  @Volatile private var disposable: Disposable = Disposables.disposed()

  override fun fetch(
    scheduler: Scheduler,
    upstream: () -> Observable<T>
  ): Observable<T> {
    return Observable.defer<T> {
      synchronized(lock) {
        // We can't use the getOrPut() extension because it may run the upstream fetch even though
        // it guarantees no double data insertions.
        val cachedRequest: Observable<T>? = inFlight

        if (cachedRequest == null) {
          logger.log { "Attempting upstream" }
          return@defer fetchUpstream(upstream, scheduler)
        } else {
          logger.log { "Attaching in flight" }
          return@defer cachedRequest
        }
      }
    }
        // Once the fetch has ended, we can clear the in flight cache.
        // We do not use the terminate event since the public consumer can fall off, but we still
        // want the request to stay in flight if one exists.
        .doOnNext { clear() }
        .doOnError { cancel() }
        .doOnNext { logger.log { "--> Emit: $it" } }
  }

  @CheckResult
  private fun fetchUpstream(
    upstream: () -> Observable<T>,
    scheduler: Scheduler
  ): Observable<T> {
    // Create the subject which will be returned as the resulting observable
    val subject = ReplaySubject.create<T>()
        .toSerialized()

    // One last check to be sure we are not clobbering an in flight
    synchronized(lock) {
      val old: Observable<T>? = inFlight
      if (old != null) {
        logger.log { "Upstream attempt provides in flight request" }
        return old
      }

      // Set the new in flight
      inFlight = subject

      // We subscribe here internally so that the actual returned subject is not opinionated about
      // the scheduler it is running on.
      //
      // NOTE: This is not a completely transparent operation and
      // scheduler independence is not guaranteed. If you need exact operations to happen on exact
      // schedulers critical to your application, you may wish to implement your own stricter
      // implementation of the Fetcher interface.
      cancelInFlight()
      disposable = upstream()
          // We must tell the original stream source to subscribe on schedulers outside of the normal
          // returned flow else if the returned stream is terminated prematurely, the source will
          // emit on a dead thread.
          // We must both observe and subscribe or else emissions on a dead thread are possible.
          .observeOn(scheduler)
          .subscribeOn(scheduler)
          .subscribe(
              {
                logger.log { "----> Upstream emit: $it" }
                subject.onNext(it)
              },
              { subject.onError(it) },
              { subject.onComplete() },
              { subject.onSubscribe(it) }
          )
    }

    logger.log { "Provides new request" }
    return subject
  }

  override fun clear() {
    synchronized(lock) {
      logger.log { "Clear cached in-flight request" }
      inFlight = null
    }
  }

  private fun cancelInFlight() {
    synchronized(lock) {
      if (!disposable.isDisposed) {
        logger.log { "Cancel in flight" }
        disposable.dispose()
      }
    }
  }

  override fun cancel() {
    cancelInFlight()
    clear()
  }
}

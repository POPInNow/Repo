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

package com.popinnow.android.repo.impl

import android.support.annotation.CheckResult
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.internal.Invalidatable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of the SingleFetcher interface
 *
 * The SingleFetcher will memoryCache any in flight upstream requests until they have completed.
 */
internal class FetcherImpl internal constructor(
  debug: String
) : Fetcher, Invalidatable {

  private val logger = Logger("Fetcher[$debug]", debug.isNotBlank())
  private val inFlight: ConcurrentHashMap<String, Subject<*>> = ConcurrentHashMap()
  private val disposables: ConcurrentHashMap<String, Disposable> = ConcurrentHashMap()

  override fun <T : Any> fetch(
    key: String,
    upstream: () -> Observable<T>,
    scheduler: Scheduler
  ): Observable<T> {
    return Observable.defer<T> {
      // We can't use the getOrPut() extension because it may run the upstream fetch even though
      // it guarantees no double data insertions.
      @Suppress("UNCHECKED_CAST")
      val cachedRequest: Observable<T>? = inFlight[key] as? Observable<T>

      if (cachedRequest == null) {
        logger.log { "Attempting upstream: $key" }
        return@defer fetchUpstream(key, upstream, scheduler)
      } else {
        logger.log { "Attaching in flight: $key" }
        return@defer cachedRequest
      }
    }
        // Once the fetch has ended, we can clear the in flight cache and the upstream disposable
        // We do not use the terminate event since the public consumer can fall off, but we still
        // want the request to stay in flight if one exists.
        .doOnNext { invalidateCaches(key) }
        .doOnError { invalidate(key) }
        .doOnNext { logger.log { "--> Emit[$key]: $it" } }
  }

  @CheckResult
  private fun <T : Any> fetchUpstream(
    key: String,
    upstream: () -> Observable<T>,
    scheduler: Scheduler
  ): Observable<T> {
    // Create the subject which will be returned as the resulting observable
    val subject = ReplaySubject.create<T>()
        .toSerialized()

    // One last check to be sure we are not clobbering an in flight
    @Suppress("UNCHECKED_CAST")
    val old: Observable<T>? = inFlight.putIfAbsent(key, subject) as? Observable<T>
    if (old != null) {
      logger.log { "Upstream attempt provides in flight request: $key" }
      return old
    }

    // We subscribe here internally so that the actual returned subject is not opinionated about
    // the scheduler it is running on.
    //
    // NOTE: This is not a completely transparent operation and
    // scheduler independence is not guaranteed. If you need exact operations to happen on exact
    // schedulers critical to your application, you may wish to implement your own stricter
    // implementation of the Fetcher interface.
    cancelInFlight(key)
    disposables[key] = upstream()
        // We must tell the original stream source to subscribe on schedulers outside of the normal
        // returned flow else if the returned stream is terminated prematurely, the source will
        // emit on a dead thread.
        // We must both observe and subscribe or else emissions on a dead thread are possible.
        .observeOn(scheduler)
        .subscribeOn(scheduler)
        .subscribe(
            {
              logger.log { "----> Upstream emit[$key]: $it" }
              subject.onNext(it)
            },
            { subject.onError(it) },
            { subject.onComplete() },
            { subject.onSubscribe(it) }
        )

    logger.log { "Provides new request: $key" }
    return subject
  }

  private fun cancelInFlight(key: String) {
    logger.log { "Cancel in flight: $key" }
    disposables.remove(key)
        ?.dispose()
  }

  override fun invalidateCaches(key: String) {
    logger.log { "Invalidate cache: $key" }
    inFlight.remove(key)
  }

  override fun clearAll() {
    clearCaches()
    disposables.apply {
      for (disposable in values) {
        disposable.dispose()
      }
      clear()
    }
  }

  override fun invalidate(key: String) {
    logger.log { "Invalidate called" }
    invalidateCaches(key)
    cancelInFlight(key)
  }

  override fun clearCaches() {
    inFlight.clear()
  }
}

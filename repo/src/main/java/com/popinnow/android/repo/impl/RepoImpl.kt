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
import androidx.annotation.VisibleForTesting
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.Repo
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.util.concurrent.atomic.AtomicBoolean

internal class RepoImpl<T : Any> internal constructor(
  private val fetcher: Fetcher<T>,
  private val memoryCache: MemoryCache<T>,
  private val persister: Persister<T>,
  private val scheduler: Scheduler,
  private val logger: Logger
) : Repo<T> {

  @CheckResult
  private fun fetchCacheThenUpstream(
    upstream: Observable<T>,
    cache: Observable<T>,
    persist: Observable<T>
  ): Observable<T> {
    return cache.switchIfEmpty(persist)
        .concatWith(upstream)
  }

  @CheckResult
  private fun fetchCacheOrUpstream(
    upstream: Observable<T>,
    cache: Observable<T>,
    persist: Observable<T>
  ): Observable<T> {
    return cache.lastElement()
        .switchIfEmpty(persist.lastElement())
        .switchIfEmpty(upstream.singleOrError())
        .toObservable()
  }

  private fun fetch(
    fetchCacheAndUpstream: Boolean,
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Observable<T> {
    return Observable.defer {
      val freshData = Observable.defer { fetcher.fetch(scheduler, upstream) }
          // When the stream begins emitting, we clear the cache
          .doOnFirst { justInvalidateBackingCaches() }
          // When the upstream is subscribed to and returns data, it should be placed into the caches,
          // but subscribing to the caches should not reset the cached data.
          .doOnNext { internalPut(it) }

      if (bustCache) {
        logger.log { "Busting cache to fetch from upstream" }
        justInvalidateBackingCaches()
      } else {
        logger.log { "Fetching from repository" }
      }

      val memory: Observable<T> = Observable.defer { memoryCache.get() }
      val persist: Observable<T> = Observable.defer { persister.read() }
      if (fetchCacheAndUpstream) {
        return@defer fetchCacheThenUpstream(freshData, memory, persist)
      } else {
        return@defer fetchCacheOrUpstream(freshData, memory, persist)
      }
    }
        .doOnError { cancel() }
  }

  @CheckResult
  private inline fun <T : Any> Observable<T>.doOnFirst(crossinline consumer: (T) -> Unit): Observable<T> {
    return this.compose { source ->
      val firstEmitted = AtomicBoolean(false)
      return@compose source.doOnNext {
        if (firstEmitted.compareAndSet(false, true)) {
          consumer(it)
        }
      }
    }
  }

  /**
   * Exposed as internal so that it can be tested.
   */
  @VisibleForTesting
  internal fun testingGet(
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Single<T> {
    return fetch(false, bustCache, upstream).singleOrError()
  }

  /**
   * Exposed as internal so that it can be tested.
   */
  @VisibleForTesting
  internal fun testingObserve(
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Observable<T> {
    return fetch(true, bustCache, upstream)
  }

  override fun get(
    bustCache: Boolean,
    upstream: () -> Single<T>
  ): Single<T> {
    val realUpstream = { upstream().toObservable() }
    return fetch(false, bustCache, realUpstream).singleOrError()
  }

  override fun observe(
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Observable<T> {
    return fetch(true, bustCache, upstream)
  }

  private fun justInvalidateBackingCaches() {
    memoryCache.clear()
    persister.clear()
  }

  private fun internalPut(value: T) {
    logger.log { "Put data: $value" }

    // Store data directly into caches
    memoryCache.add(value)
    persister.write(value)

    // Stop the cachd data but keep the actual underlying in-flighgt since we could be mid get()
    fetcher.clear()
  }

  private fun internalPutAll(values: List<T>) {
    logger.log { "Put data: $values" }

    // Store data directly into caches
    memoryCache.addAll(values)
    persister.writeAll(values)

    // Cancel fetcher in flights
    fetcher.clear()
  }

  override fun push(value: T) {
    internalPut(value)
  }

  override fun pushAll(values: List<T>) {
    internalPutAll(values)
  }

  override fun replace(value: T) {
    justInvalidateBackingCaches()
    internalPut(value)
  }

  override fun replaceAll(
    values: List<T>
  ) {
    justInvalidateBackingCaches()
    internalPutAll(values)
  }

  override fun clear() {
    logger.log { "Clearing caches" }
    memoryCache.clear()
    persister.clear()
    fetcher.clear()
  }

  override fun cancel() {
    logger.log { "Cancelling Fetcher" }
    fetcher.cancel()

    clear()
  }

}

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
import android.support.annotation.VisibleForTesting
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.manager.MemoryCacheManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.util.concurrent.atomic.AtomicBoolean

internal class RepoImpl internal constructor(
  private val fetcher: Fetcher,
  private val memoryCache: MemoryCache,
  private val persister: Persister,
  private val scheduler: Scheduler,
  debug: Boolean,
  logTag: String = "RepoImpl"
) : Repo {

  private val logger by lazy { Logger(logTag, debug) }

  @CheckResult
  private fun <T : Any> fetchCacheThenUpstream(
    upstream: Observable<T>,
    cache: Observable<T>,
    persist: Observable<T>
  ): Observable<T> {
    return cache.switchIfEmpty(persist)
        .concatWith(upstream)
  }

  @CheckResult
  private fun <T : Any> fetchCacheOrUpstream(
    upstream: Observable<T>,
    cache: Observable<T>,
    persist: Observable<T>
  ): Observable<T> {
    return cache.lastElement()
        .switchIfEmpty(persist.lastElement())
        .switchIfEmpty(upstream.singleOrError())
        .toObservable()
  }

  private fun <T : Any> fetch(
    fetchCacheAndUpstream: Boolean,
    bustCache: Boolean,
    key: String,
    upstream: () -> Observable<T>,
    mapper: (Any) -> T
  ): Observable<T> {
    return Observable.defer {
      val freshData = fetcher.fetch(key, upstream, scheduler)
          // When the stream begins emitting, we clear the cache
          .doOnFirst { justInvalidateBackingCaches(key) }
          // When the upstream is subscribed to and returns data, it should be placed into the caches,
          // but subscribing to the caches should not reset the cached data.
          .doOnNext { internalPut(key, it) }

      if (bustCache) {
        logger.log { "Busting cache to fetch from upstream" }
        justInvalidateBackingCaches(key)
      } else {
        logger.log { "Fetching from repository" }
      }

      val memory: Observable<T> = memoryCache.get(key, mapper)
      val persist: Observable<T> = persister.read(key, mapper)
      if (fetchCacheAndUpstream) {
        return@defer fetchCacheThenUpstream(freshData, memory, persist)
      } else {
        return@defer fetchCacheOrUpstream(freshData, memory, persist)
      }
    }
        .doOnError { invalidate(key) }
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
  internal fun <T : Any> testingGet(
    bustCache: Boolean,
    key: String,
    upstream: () -> Observable<T>,
    mapper: (Any) -> T
  ): Single<T> {
    return fetch(false, bustCache, key, upstream, mapper).singleOrError()
  }

  /**
   * Exposed as internal so that it can be tested.
   */
  @VisibleForTesting
  internal fun <T : Any> testingObserve(
    bustCache: Boolean,
    key: String,
    upstream: () -> Observable<T>,
    mapper: (Any) -> T
  ): Observable<T> {
    return fetch(true, bustCache, key, upstream, mapper)
  }

  @CheckResult
  private fun <T : Any> mapper(item: Any): T {
    @Suppress("UNCHECKED_CAST")
    return item as T
  }

  override fun <T : Any> get(
    bustCache: Boolean,
    key: String,
    upstream: () -> Single<T>
  ): Single<T> {
    val realUpstream = { upstream().toObservable() }
    return fetch(false, bustCache, key, realUpstream, this::mapper).singleOrError()
  }

  override fun <T : Any> observe(
    bustCache: Boolean,
    key: String,
    upstream: () -> Observable<T>
  ): Observable<T> {
    return fetch(true, bustCache, key, upstream, this::mapper)
  }

  private fun justInvalidateBackingCaches(key: String) {
    memoryCache.invalidate(key)
    persister.invalidate(key)
  }

  private fun internalPut(
    key: String,
    value: Any
  ) {
    logger.log { "Put data: $key $value" }

    // Store data directly into caches
    memoryCache.add(key, value)
    persister.write(key, value)

    // Cancel fetcher in flights
    fetcher.invalidateCaches(key)
  }

  private fun internalPutAll(
    key: String,
    values: List<Any>
  ) {
    logger.log { "Put data: $key $values" }

    // Store data directly into caches
    memoryCache.addAll(key, values)
    persister.writeAll(key, values)

    // Cancel fetcher in flights
    fetcher.invalidateCaches(key)
  }

  override fun push(
    key: String,
    value: Any
  ) {
    internalPut(key, value)
  }

  override fun pushAll(
    key: String,
    values: List<Any>
  ) {
    internalPutAll(key, values)
  }

  override fun replace(
    key: String,
    value: Any
  ) {
    justInvalidateBackingCaches(key)
    internalPut(key, value)
  }

  override fun replaceAll(
    key: String,
    values: List<Any>
  ) {
    justInvalidateBackingCaches(key)
    internalPutAll(key, values)
  }

  override fun invalidateCaches(key: String) {
    logger.log { "Invalidating caches: $key" }
    justInvalidateBackingCaches(key)
    fetcher.invalidateCaches(key)
  }

  override fun invalidate(key: String) {
    invalidateCaches(key)
    fetcher.invalidate(key)
  }

  override fun clearCaches() {
    logger.log { "Clearing caches" }
    memoryCache.clearAll()
    persister.clearAll()
    fetcher.clearCaches()
  }

  override fun clearAll() {
    clearCaches()
    fetcher.clearAll()
  }

  override fun memoryCache(): MemoryCacheManager {
    return memoryCache
  }

}

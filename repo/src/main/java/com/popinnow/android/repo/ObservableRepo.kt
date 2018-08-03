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

package com.popinnow.android.repo

import android.support.annotation.CheckResult
import com.popinnow.android.repo.internal.CacheInvalidator
import com.popinnow.android.repo.manager.MemoryCacheManager
import io.reactivex.Observable

/**
 * ObservableRepo follows the cache-then-upstream model using [io.reactivex.Observable] streams.
 *
 * This Repo follows the cache-then-upstream model. When requests are made to this Repo
 * the [io.reactivex.Observer] is subscribed to a stream which is sourced from a potentially
 * endless [io.reactivex.Observable]. If any data exists in the cache for this Repo, it will be
 * emitted first. Once any cached data is emitted should it exist, the upstream will be subscribed
 * to and emit as usual.
 *
 * If caching is enabled for this Repo, the latest emitted item from the upstream data source will
 * be cached.
 */
interface ObservableRepo<T : Any> : CacheInvalidator {

  /**
   * Get data from this Repo, possibly from the provided upstream source.
   *
   * If [bustCache] is true, the Repo will skip any caching layers that may have data, and will
   * always fetch new data from the [upstream]. Any data retrieved from the [upstream] will still
   * be put back into the caches.
   *
   * The [key] is what controls uniqueness of requests.
   *
   * If no cached data exists in the Repo for a given [key], then fresh data will be pulled
   * from the [upstream]. If cached data exists, it will be emitted first, and then the [upstream]
   * will be pulled. Ordering is guaranteed - cached data will always emit first if it exists.
   *
   * @param bustCache Bypass any caching and pull data straight from the upstream source.
   * @param key The key for this request.
   * @param upstream The lazy upstream data source.
   * @return [Observable]
   */
  @CheckResult
  fun get(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Observable<T>
  ): Observable<T>

  /**
   * Get a manager that allows the querying of this Repo's [MemoryCache]
   *
   * @return [MemoryCacheManager]
   */
  @CheckResult
  fun memoryCache(): MemoryCacheManager<T>

  /**
   * Adds data into the Repo.
   *
   * @param key The key for this request
   * @param value The data to put into the Repo
   */
  fun add(
    key: String,
    value: T
  )

  /**
   * Adds data into the Repo.
   *
   * @param key The key for this request
   * @param value The data to put into the Repo
   */
  @Deprecated("Use add() instead", ReplaceWith("add(key, value)", "com.popinnow.android.repo.MemoryCache"))
  fun put(
    key: String,
    value: T
  )

  /**
   * Invalidates all caches requests for a given key.
   *
   * This will [invalidate] any configured [MemoryCache] or [Persister] for the given [key].
   * It will not cancel any in-flight requests being performed by a [Fetcher], but will clear
   * the known cache via [Fetcher.invalidateCaches]
   *
   * @param key The key for this request.
   * @see invalidate
   */
  override fun invalidateCaches(key: String)

  /**
   * Invalidates all caches and in-flight requests for a given key.
   *
   * This will [invalidate] any configured [MemoryCache] or [Persister] for the given [key], and
   * will also cancel any in-flight requests performed by [Fetcher] via [Fetcher.invalidate]
   *
   * @param key The key for this request.
   */
  override fun invalidate(key: String)

  /**
   * Invalidates all caches requests for a all keys.
   *
   * This will [clearAll] any configured [MemoryCache] or [Persister].
   * It will not cancel any in-flight requests being performed by a [Fetcher], but will clear
   * the known cache via [Fetcher.clearCaches].
   *
   * To clear all and cancel in-flight requests, see [clearAll]
   *
   * @see clearAll
   */
  override fun clearCaches()

  /**
   * Invalidates all caches and in-flight requests for a all keys.
   *
   * This will [clearAll] any configured [MemoryCache] or [Persister], and
   * will also cancel any in-flight requests performed by [Fetcher] via [Fetcher.clearAll]
   */
  override fun clearAll()
}

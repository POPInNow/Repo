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
import com.popinnow.android.repo.internal.CacheClearable
import com.popinnow.android.repo.manager.MemoryCacheManager
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Repo follows the cache-then-upstream model and cache-or-upstream model.
 *
 * This Repo follows the cache-then-upstream model. When requests are made to this Repo
 * the [io.reactivex.Observer] is subscribed to a stream which is sourced from a potentially
 * endless [io.reactivex.Observable]. If any data exists in the cache for this Repo, it will be
 * emitted first. Once any cached data is emitted should it exist, the upstream will be subscribed
 * to and emit as usual.
 *
 * If caching is enabled for this Repo, the latest emitted item from the upstream data source will
 * be cached.
 *
 * This Repo follows the cache-or-upstream model. When requests are made to this Repo
 * the [io.reactivex.Observer] is subscribed to a stream which is sourced from a
 * [io.reactivex.Single]. If any data exists in the cache for this Repo, it will be emitted instead
 * of calling the upstream.
 *
 * If any cached data exists and is emitted, the upstream will never be subscribed to.
 *
 * If caching is enabled for this Repo, the latest emitted item from the upstream data source will
 * be cached.
 */
interface Repo<T : Any> : CacheClearable {

  /**
   * Observe data from this Repo, possibly from the provided upstream source.
   *
   * If [bustCache] is true, the Repo will skip any caching layers that may have data, and will
   * always fetch new data from the [upstream]. Any data retrieved from the [upstream] will still
   * be put back into the caches.
   *
   * If no cached data exists in the Repo, then fresh data will be pulled
   * from the [upstream]. If cached data exists, it will be emitted first, and then the [upstream]
   * will be pulled. Ordering is guaranteed - cached data will always emit first if it exists.
   *
   * @param bustCache Bypass any caching and pull data straight from the upstream source.
   * @param upstream The lazy upstream data source.
   * @return [Observable]
   */
  @CheckResult
  fun observe(
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Observable<T>

  /**
   * Get data from this Repo, possibly from the provided upstream source.
   *
   * If [bustCache] is true, the Repo will skip any caching layers that may have data, and will
   * always fetch new data from the [upstream]. Any data retrieved from the [upstream] will still
   * be put back into the caches.
   *
   * If no cached data exists in the Repo, then fresh data will be pulled
   * from the [upstream]. If cached data exists, the latest will be emitted, and the [upstream]
   * will never be pulled.
   *
   * @param bustCache Bypass any caching and pull data straight from the upstream source.
   * @param upstream The lazy upstream data source.
   * @return [Single]
   */
  @CheckResult
  fun get(
    bustCache: Boolean,
    upstream: () -> Single<T>
  ): Single<T>

  /**
   * Get a manager that allows the querying of this Repo's [MemoryCache]
   *
   * @return [MemoryCacheManager]
   */
  @CheckResult
  fun memoryCache(): MemoryCacheManager

  /**
   * Replace data in the Repo with a single item.
   *
   * @param value The data to put into the Repo
   */
  fun replace(value: T)

  /**
   * Replace data into the Repo with a list of items.
   *
   * @param values The list data to put into the Repo
   */
  fun replaceAll(values: List<T>)

  /**
   * Push data into the Repo.
   *
   * @param value The data to put into the Repo
   */
  fun push(value: T)

  /**
   * Push a list of data into the Repo.
   *
   * @param values The list data to put into the Repo
   */
  fun pushAll(values: List<T>)

  /**
   * Invalidates all caches requests.
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
   * Invalidates all caches and in-flight requests.
   *
   * This will [clearAll] any configured [MemoryCache] or [Persister], and
   * will also cancel any in-flight requests performed by [Fetcher] via [Fetcher.clearAll]
   */
  override fun clearAll()
}

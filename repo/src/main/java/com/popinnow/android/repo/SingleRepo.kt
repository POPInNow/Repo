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
import io.reactivex.Single

/**
 * SingleRepo follows the cache-or-upstream model using [Single] streams.
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
@Deprecated("Use Repo<T> instead")
interface SingleRepo<T : Any> : CacheInvalidator {

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
   * from the [upstream]. If cached data exists, it will be emitted, and the [upstream]
   * will never be pulled.
   *
   * @param bustCache Bypass any caching and pull data straight from the upstream source.
   * @param key The key for this request.
   * @param upstream The lazy upstream data source.
   * @return [Single]
   */
  @CheckResult
  fun get(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Single<T>
  ): Single<T>

  /**
   * Get a manager that allows the querying of this Repo's [MemoryCache]
   *
   * @return [MemoryCacheManager]
   */
  @CheckResult
  fun memoryCache(): MemoryCacheManager<T>

  /**
   * Put data into the Repo.
   *
   * @param key The key for this request
   * @param value The data to put into the Repo
   */
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

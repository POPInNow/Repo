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

package com.popinnow.android.repo

import androidx.annotation.CheckResult
import com.popinnow.android.repo.internal.Cancellable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Repo follows the cache-then-upstream model and cache-or-upstream model.
 *
 * cache-then-upstream is implemented via the [observe] method
 * cache-or-upstream is implemented via the [get] method
 */
interface Repo<T : Any> : Cancellable {

  /**
   * Observe data from this Repo, possibly from the provided upstream source.
   *
   * If [bustCache] is true, the Repo will skip any caching layers that may have data, and will
   * always fetch new data from the [upstream]. Any data retrieved from the [upstream] will still
   * be put back into the caches.
   *
   * If no cached data exists in the Repo, then fresh data will be pulled
   * from the [upstream]. If cached data exists, it will be emitted first, and then the [upstream]
   * will be pulled.
   *
   * Ordering is guaranteed - cached data will always emit first if it exists.
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
   * Ordering is guaranteed - cached data will always emit instead of the upstream if it exists.
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
   * Cancels all in-flight requests currently being performed by the [Fetcher] implementation.
   *
   * @see [clear]
   */
  override fun cancel()

  /**
   * Clears all [MemoryCache] and [Persister] caches
   *
   * Does not stop any [Fetcher] in-flight requests
   *
   * @see [cancel]
   */
  override fun clear()
}

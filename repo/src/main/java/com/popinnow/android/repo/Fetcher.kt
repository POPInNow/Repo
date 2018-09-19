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
import io.reactivex.Observable
import io.reactivex.Scheduler

/**
 * Fetcher retrieves data from an upstream source.
 *
 * Fetcher does not care what the upstream source it - only that it talks [Observable].
 * Possible upstream sources can include the network, disk, memory, or databases.
 *
 * @see CacheInvalidator
 */
interface Fetcher : CacheInvalidator {

  /**
   * Fetch data from the upstream source.
   *
   * When fetching data from an upstream source, the key is used to determine uniqueness.
   * If [fetch] is called with a key which is currently still associated to an in flight request,
   * the [io.reactivex.Observer] will be attached to the existing in flight request instead of
   * calling the upstream again.
   *
   * @param key The key for this request.
   *            Passing a key which is already associated with an upstream request will attach
   *            the Observer to the existing upstream request instead of making a new one.
   *
   * @param upstream A lazy generator for the upstream data source observable.
   *            The upstream observable will only be called when it is making a new request.
   *            If the upstream is attached to a key which is already associated with an in flight
   *            request, the upstream will never be evaluated.
   *
   * @param scheduler The Scheduler to fetch the upstream observable from.
   *
   *            NOTE: While hot observables should in theory work, they are not tested.
   */
  @CheckResult
  fun <T : Any> fetch(
    key: String,
    upstream: () -> Observable<T>,
    scheduler: Scheduler
  ): Observable<T>

  /**
   * Clears an in flight request from the known cache, but does not cancel the request.
   *
   * Calling [invalidate] will remove the [key] from the cache of in-flight requests that is kept
   * by the [Fetcher], which will make any new requests to the same [key] launch a new upstream
   * request. It will not cancel any existing in-flight requests. They will complete normally and
   * deliver to any [io.reactivex.Observer] that has already subscribed.
   *
   * If the key is not known by the Fetcher, this operation is a no-op
   *
   * @param key The key for the request.
   * @see invalidate
   */
  override fun invalidateCaches(key: String)

  /**
   * Cancels an in-flight request and invalidates it.
   *
   * Similar to [invalidateCaches] but it also cancels the actual in-flight request to the upstream
   * if one is live.
   *
   *
   * To invalidate and cancel existing in-flight requests, see [invalidateCaches]
   * If the key is not known by the Fetcher, this operation is a no-op
   *
   * @param key The key for the request.
   */
  override fun invalidate(key: String)

  /**
   * Clears all in-flight requests from the known cache, but does not cancel any requests.
   *
   * Similar to [invalidate] but operates on all keys. This does not cancel
   * any existing in-flight requests.
   *
   * To clear and cancel all existing in-flight requests, see [clearAll]
   * @see clearAll
   */
  override fun clearCaches()

  /**
   * Cancels all in-flight requests and clears the known cache.
   *
   * Similar to [clearAll] but operates on all keys. This cancels all existing in-flight requests
   * and clears the known cache.
   */
  override fun clearAll()

}

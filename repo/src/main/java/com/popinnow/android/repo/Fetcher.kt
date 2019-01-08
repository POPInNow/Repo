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
import io.reactivex.Scheduler

/**
 * Fetcher retrieves data from an upstream source.
 *
 * Fetcher does not care what the upstream source it - only that it talks [Observable].
 * Possible upstream sources can include the network, disk, memory, or databases,
 * but the Fetcher is not limited to only these sources.
 */
interface Fetcher<T : Any> : Cancellable {

  /**
   * Fetch data from the upstream source.
   *
   * If [fetch] is called with a while still fetching an in flight request,
   * the [io.reactivex.Observer] will be attached to the existing in flight request instead of
   * calling the upstream again.
   *
   * @param scheduler The Scheduler to fetch the upstream observable from.
   *
   * @param upstream A lazy generator for the upstream data source observable.
   *            The upstream observable will only be called when it is making a new request.
   *            If the upstream is associated with an already in flight
   *            request, the upstream will never be evaluated.
   *
   *            NOTE: While hot observables should in theory work, they are not tested.
   */
  @CheckResult
  fun fetch(
    scheduler: Scheduler,
    upstream: () -> Observable<T>
  ): Observable<T>

  /**
   * Cancels all in-flight requests to an upstream but does not clear the cache
   *
   * @see clear
   * @see shutdown
   */
  override fun cancel()

  /**
   * Clears the cache but does not cancel any in-flight requests to an upstream
   *
   * @see cancel
   * @see shutdown
   */
  override fun clear()

  /**
   * Calls [cancel] and then calls [clear]
   *
   * @see cancel
   * @see clear
   */
  override fun shutdown()

}

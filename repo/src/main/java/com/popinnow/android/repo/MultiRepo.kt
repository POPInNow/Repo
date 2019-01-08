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
import com.popinnow.android.repo.internal.MultiCancellable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * A Repo-Like interface which maps over multiple similar Repo instances
 *
 * @see [Repo]
 */
interface MultiRepo<T : Any> : MultiCancellable {

  /**
   * Observe the [Repo] identified by [key]
   *
   * @see [Repo.observe]
   * @param key Unique key
   * @param bustCache
   * @param upstream
   * @return [Observable]
   */
  @CheckResult
  fun observe(
    key: String,
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Observable<T>

  /**
   * Get the [Repo] identified by [key]
   *
   * @see [Repo.get]
   * @param key Unique key
   * @param bustCache
   * @param upstream
   * @return [Single]
   */
  @CheckResult
  fun get(
    key: String,
    bustCache: Boolean,
    upstream: () -> Single<T>
  ): Single<T>

  /**
   * Replaces data in the caching layer for the [Repo] identified by [key]
   *
   * @see [Repo.replace]
   * @param key Unique key
   * @param value The data to put into the Repo
   */
  fun replace(
    key: String,
    value: T
  )

  /**
   * Replaces data in the caching layer for the [Repo] identified by [key]
   *
   * @see [Repo.replaceAll]
   * @param key Unique key
   * @param values The data to put into the Repo
   */
  fun replaceAll(
    key: String,
    values: List<T>
  )

  /**
   * Pushes data into the caching layer for the [Repo] identified by [key]
   *
   * Data will be appended to the end of any data that already is present.
   *
   * @see [Repo.push]
   * @param key Unique key
   * @param value The data to put into the Repo
   */
  fun push(
    key: String,
    value: T
  )

  /**
   * Pushes data into the caching layer for the [Repo] identified by [key]
   *
   * Data will be appended to the end of any data that already is present.
   *
   * @see [Repo.pushAll]
   * @param key Unique key
   * @param values The data to put into the Repo
   */
  fun pushAll(
    key: String,
    values: List<T>
  )

  /**
   * Cancels any in-flights requests for the [Repo] identified by [key]
   *
   * @param key Unique key
   * @see [Repo.cancel]
   */
  override fun cancel(key: String)

  /**
   * Clears cached data for the [Repo] identified by [key], but does not stop any in-flight requests
   *
   * @param key Unique key
   * @see [Repo.clear]
   */
  override fun clear(key: String)

  /**
   * Cancels all in-flight requests for all [Repo] objects held by this [MultiRepo]
   *
   * @see [Repo.cancel]
   */
  override fun cancel()

  /**
   * Clears cached data for all [Repo] objects held by this [MultiRepo]
   *
   * @see [Repo.cancel]
   */
  override fun clear()

  /**
   * Calls [cancel] and then [clear] for the [Repo] identified by [key]
   *
   * @param key Unique key
   * @see [Repo.shutdown]
   */
  override fun shutdown(key: String)

  /**
   * Calls [cancel] and then [clear] for all [Repo] objects held by this [MultiRepo]
   *
   * @see [Repo.shutdown]
   */
  override fun shutdown()
}

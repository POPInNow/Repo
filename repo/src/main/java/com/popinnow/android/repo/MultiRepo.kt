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
import com.popinnow.android.repo.internal.CacheInvalidatable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * A Repo-Like interface which maps over multiple similar Repo instances
 *
 * @see [Repo]
 */
interface MultiRepo<T : Any> : CacheClearable, CacheInvalidatable {

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
   * Clears the data for the [Repo] identified by [key]
   *
   * @see [Repo.clearAll]
   * @param key Unique key
   */
  override fun invalidate(key: String)

  /**
   * Clears the caching layer for the [Repo] identified by [key]
   *
   * @see [Repo.clearCaches]
   * @param key Unique key
   */
  override fun invalidateCaches(key: String)

  /**
   * Clears the caching layer for the all [Repo] instances tracked by this [MultiRepo]
   *
   * @see [Repo.clearCaches]
   */
  override fun clearCaches()

  /**
   * Clears the data for the all [Repo] instances tracked by this [MultiRepo]
   *
   * @see [Repo.clearAll]
   */
  override fun clearAll()
}

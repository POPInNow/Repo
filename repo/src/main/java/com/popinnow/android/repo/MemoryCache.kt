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
import com.popinnow.android.repo.internal.Invalidatable
import com.popinnow.android.repo.manager.MemoryCacheManager
import io.reactivex.Observable

/**
 * MemoryCache is an in-memory cache for [Observable] which are retrieved from a [Fetcher]
 *
 * The default implementation caches items based on time from the point that the item is written
 * to the cache.
 *
 * @see Invalidatable
 * @see MemoryCacheManager
 */
interface MemoryCache<T : Any> : Invalidatable, MemoryCacheManager<T> {

  /**
   * Retrieves data stored in the cache.
   *
   * If there is no data stored in the cache for the given [key], the cache will return an
   * [Observable.empty]
   *
   * @param key The key for this request.
   * @return [Observable]
   */
  @CheckResult
  fun get(key: String): Observable<T>

  /**
   * Adds data into the cache.
   *
   * If there is data in the cache already for the given [key], new data will be appended.
   *
   * @param key The key for this request.
   * @param value The data to put into the cache.
   */
  fun add(
    key: String,
    value: T
  )

  /**
   * Adds data into the cache.
   *
   * If there is data in the cache already for the given [key], new data will be appended.
   *
   * @param key The key for this request.
   * @param value The data to put into the cache.
   */
  @Deprecated(
      "Use add() instead", ReplaceWith("add(key, value)", "com.popinnow.android.repo.MemoryCache")
  )
  fun put(
    key: String,
    value: T
  )

}


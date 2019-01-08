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
import com.popinnow.android.repo.internal.Clearable
import io.reactivex.Observable

/**
 * MemoryCache is an in-memory cache for [Observable] which are retrieved from a [Fetcher]
 *
 * The default implementation caches items based on time from the point that the item is written
 * to the cache.
 */
interface MemoryCache<T : Any> : Clearable {

  /**
   * Retrieves data stored in the cache.
   *
   * If there is no data stored in the cache , the cache will return an [Observable.empty]
   *
   * @return [Observable]
   */
  @CheckResult
  fun get(): Observable<T>

  /**
   * Adds data into the cache.
   *
   * If there is data in the cache already, new data will be appended.
   *
   * @param value The data to put into the cache.
   */
  fun add(value: T)

  /**
   * Adds a list of data into the cache.
   *
   * If there is data in the cache already, new data will be appended.
   *
   * @param values The list of data to put into the cache.
   */
  fun addAll(values: List<T>)

  /**
   * Clears the cache
   */
  override fun clear()

}


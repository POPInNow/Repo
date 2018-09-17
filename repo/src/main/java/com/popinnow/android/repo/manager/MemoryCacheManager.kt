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

package com.popinnow.android.repo.manager

import android.support.annotation.CheckResult

interface MemoryCacheManager {

  /**
   * Get the current size of the cache
   *
   * @see [android.support.v4.util.LruCache.size]
   * @return Size of the memory cache
   */
  @CheckResult
  fun size(): Int

  /**
   * Get the max size of the cache
   *
   * A max size of 0 means that there is no memory cache
   *
   * @see [android.support.v4.util.LruCache.maxSize]
   * @return Max size of the memory cache
   */
  @CheckResult
  fun maxSize(): Int

  /**
   * Evicts the least recently used entries until the cache size matches the requested amount
   *
   * @see [android.support.v4.util.LruCache.trimToSize]
   * @param maxSize The max size the cache should be trimmed to.
   */
  fun trimToSize(maxSize: Int)

}

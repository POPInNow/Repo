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
import com.popinnow.android.repo.manager.MemoryCacheManager
import com.popinnow.android.repo.manager.MultiMemoryCacheManager
import io.reactivex.Observable
import io.reactivex.Single

// TODO Document
interface MultiRepo<T : Any> : CacheClearable, CacheInvalidatable {

  @CheckResult
  fun observe(
    key: String,
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Observable<T>

  @CheckResult
  fun get(
    key: String,
    bustCache: Boolean,
    upstream: () -> Single<T>
  ): Single<T>

  @CheckResult
  fun memoryCache(key: String): MemoryCacheManager

  @CheckResult
  fun memoryCache(): MultiMemoryCacheManager

  fun replace(
    key: String,
    value: T
  )

  fun replaceAll(
    key: String,
    values: List<T>
  )

  fun push(
    key: String,
    value: T
  )

  fun pushAll(
    key: String,
    values: List<T>
  )

  override fun invalidate(key: String)

  override fun invalidateCaches(key: String)

  override fun clearCaches()

  override fun clearAll()
}

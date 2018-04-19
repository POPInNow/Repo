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

package com.popinnow.android.repo.impl

import android.support.annotation.CheckResult
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.internal.CacheInvalidator
import io.reactivex.Observable
import io.reactivex.Scheduler

internal abstract class RepoImpl<T : Any> internal constructor(
  protected val fetcher: Fetcher<T>,
  protected val memoryCache: MemoryCache<T>,
  protected val persister: Persister<T>,
  private val scheduler: Scheduler
) : CacheInvalidator {

  @CheckResult
  protected abstract fun logger(): Logger

  @CheckResult
  protected abstract fun realFetch(
    key: String,
    upstream: Observable<T>,
    cache: Observable<T>,
    persist: Observable<T>
  ): Observable<T>

  protected fun fetch(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Observable<T>
  ): Observable<T> {
    return Observable.defer {
      val freshData = fetcher.fetch(key, upstream, scheduler)
      if (bustCache) {
        logger().log { "Busting cache to fetch from upstream" }
        return@defer freshData
      } else {
        val memory = memoryCache.get(key)
        val persist = persister.read(key)
        logger().log { "Fetching from repository" }
        return@defer realFetch(key, freshData, memory, persist)
      }
    }
        .doOnError { invalidate(key) }
  }

  protected fun justInvalidateBackingCaches(key: String) {
    memoryCache.invalidate(key)
    persister.invalidate(key)
  }

  override fun invalidateCaches(key: String) {
    logger().log { "Invalidating caches: $key" }
    justInvalidateBackingCaches(key)
    fetcher.invalidateCaches(key)
  }

  override fun clearCaches() {
    logger().log { "Clearing caches" }
    memoryCache.clearAll()
    persister.clearAll()
    fetcher.clearCaches()
  }

  protected fun internalPut(
    key: String,
    value: T
  ) {
    logger().log { "Put data: $key $value" }

    // Store data directly into caches
    memoryCache.put(key, value)
    persister.write(key, value)

    // Cancel fetcher in flights
    fetcher.invalidateCaches(key)
  }

  final override fun invalidate(key: String) {
    invalidateCaches(key)
    fetcher.invalidate(key)
  }

  final override fun clearAll() {
    clearCaches()
    fetcher.clearAll()
  }

}

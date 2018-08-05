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

import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.SingleRepo
import com.popinnow.android.repo.manager.MemoryCacheManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single

internal class SingleRepoImpl<T : Any> internal constructor(
  fetcher: Fetcher<T>,
  memoryCache: MemoryCache<T>,
  persister: Persister<T>,
  scheduler: Scheduler,
  debug: Boolean
) : RepoImpl<T>(fetcher, memoryCache, persister, scheduler), SingleRepo<T> {

  private val logger = Logger("SingleRepo", debug)

  override fun logger(): Logger {
    return logger
  }

  override fun get(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Single<T>
  ): Single<T> {
    val realUpstream: (String) -> Observable<T> = { upstream(it).toObservable() }
    return get(bustCache, key, realUpstream)
  }

  /**
   * Exposed as internal so that it can be tested.
   */
  internal fun get(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Observable<T>
  ): Single<T> {
    return fetch(bustCache, key, upstream).singleOrError()
  }

  /**
   * Fetching from a SingleRepo only returns the latest piece of data
   */
  override fun realFetch(
    key: String,
    upstream: Observable<T>,
    cache: Observable<T>,
    persist: Observable<T>
  ): Observable<T> {
    return cache.lastElement()
        .switchIfEmpty(persist.lastElement())
        .switchIfEmpty(upstream.singleOrError())
        .toObservable()
  }

  /**
   * Putting into a SingleRepo invalidates any caches before modifying
   */
  override fun put(
    key: String,
    value: T
  ) {
    justInvalidateBackingCaches(key)
    internalPut(key, value)
  }

  /**
   * Access the memory cache
   */
  override fun memoryCache(): MemoryCacheManager<T> {
    return memoryCache
  }
}

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
import com.popinnow.android.repo.ObservableRepo
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.manager.MemoryCacheManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import java.util.concurrent.atomic.AtomicBoolean

internal class ObservableRepoImpl<T : Any> internal constructor(
  fetcher: Fetcher<T>,
  memoryCache: MemoryCache<T>,
  persister: Persister<T>,
  scheduler: Scheduler,
  debug: Boolean
) : RepoImpl<T>(fetcher, memoryCache, persister, scheduler), ObservableRepo<T> {

  private val logger = Logger("ObservableRepo", debug)

  override fun logger(): Logger {
    return logger
  }

  override fun get(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Observable<T>
  ): Observable<T> {
    return fetch(bustCache, key, upstream)
  }

  override fun realFetch(
    key: String,
    upstream: Observable<T>,
    cache: Observable<T>,
    persist: Observable<T>
  ): Observable<T> {
    return cache.switchIfEmpty(persist)
        .concatWith(upstream
            // When the stream begins emitting, we clear the cache
            .doOnFirst { justInvalidateBackingCaches(key) }
            // When the upstream is subscribed to and returns data, it should be placed into the caches,
            // but subscribing to the caches should not reset the cached data.
            .doOnNext { internalPut(key, it) })
  }

  @CheckResult
  private inline fun Observable<T>.doOnFirst(crossinline consumer: (T) -> Unit): Observable<T> {
    return this.compose { source ->
      val firstEmitted = AtomicBoolean(false)
      return@compose source.doOnNext {
        if (firstEmitted.compareAndSet(false, true)) {
          consumer(it)
        }
      }
    }
  }

  override fun put(
    key: String,
    value: T
  ) {
    internalPut(key, value)
  }

  override fun memoryCache(): MemoryCacheManager<T> {
    return memoryCache
  }

}

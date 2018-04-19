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
import com.popinnow.android.repo.ObservableRepo
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.RepoBuilder
import com.popinnow.android.repo.SingleRepo
import com.popinnow.android.repo.noop.NoopCache
import com.popinnow.android.repo.noop.NoopPersister
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

internal class RepoBuilderImpl<T : Any> internal constructor(
  private var fetcher: Fetcher<T>? = null,
  private var memoryCache: MemoryCache<T>? = null,
  private var persister: Persister<T>? = null,
  private var scheduler: Scheduler? = null,
  private var debug: Boolean = false
) : RepoBuilder<T> {

  override fun scheduler(scheduler: () -> Scheduler): RepoBuilder<T> {
    return scheduler(scheduler())
  }

  override fun memoryCache(): RepoBuilder<T> {
    return memoryCache(DEFAULT_TIME, DEFAULT_UNIT)
  }

  override fun memoryCache(
    time: Long,
    timeUnit: TimeUnit
  ): RepoBuilder<T> {
    return memoryCache(time, timeUnit, DEFAULT_MAX_SIZE)
  }

  override fun memoryCache(maxSize: Int): RepoBuilder<T> {
    return memoryCache(DEFAULT_TIME, DEFAULT_UNIT, maxSize)
  }

  override fun memoryCache(
    time: Long,
    timeUnit: TimeUnit,
    maxSize: Int
  ): RepoBuilder<T> {
    return memoryCache(MemoryCacheImpl(debug, time, timeUnit, maxSize))
  }

  override fun debug(debug: Boolean): RepoBuilder<T> {
    this.debug = debug
    return this
  }

  override fun fetcher(fetcher: Fetcher<T>): RepoBuilder<T> {
    this.fetcher = fetcher
    return this
  }

  override fun memoryCache(cache: MemoryCache<T>): RepoBuilder<T> {
    this.memoryCache = cache
    return this
  }

  override fun scheduler(scheduler: Scheduler): RepoBuilder<T> {
    this.scheduler = scheduler
    return this
  }

  override fun buildObservable(): ObservableRepo<T> {
    return ObservableRepoImpl(
        fetcher ?: FetcherImpl(debug),
        memoryCache ?: NoopCache(),
        persister ?: NoopPersister(),
        scheduler ?: Schedulers.io(),
        debug
    )
  }

  override fun buildSingle(): SingleRepo<T> {
    return SingleRepoImpl(
        fetcher ?: FetcherImpl(debug),
        memoryCache ?: NoopCache(),
        persister ?: NoopPersister(),
        scheduler ?: Schedulers.io(),
        debug
    )
  }

  companion object {

    private const val DEFAULT_TIME: Long = 30
    private val DEFAULT_UNIT: TimeUnit = TimeUnit.SECONDS
    private const val DEFAULT_MAX_SIZE: Int = 8
  }

}

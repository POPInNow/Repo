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

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.RepoBuilder
import com.popinnow.android.repo.impl.noop.NoopCache
import com.popinnow.android.repo.impl.noop.NoopPersister
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

internal class RepoBuilderImpl<T : Any> internal constructor(
  private var fetcher: Fetcher<T>? = null,
  private var persister: Persister<T>? = null,
  private var scheduler: Scheduler? = null,
  private var debug: String = ""
) : RepoBuilder<T> {

  private var cacheBuilder = MemoryCacheBuilder<T>(
      enabled = false,
      time = DEFAULT_TIME,
      timeUnit = DEFAULT_UNIT,
      custom = null
  )

  override fun debug(debug: String): RepoBuilder<T> {
    this.debug = debug
    return this
  }

  override fun memoryCache(): RepoBuilder<T> {
    return memoryCache(DEFAULT_TIME, DEFAULT_UNIT)
  }

  override fun memoryCache(
    time: Long,
    timeUnit: TimeUnit
  ): RepoBuilder<T> {
    this.cacheBuilder.also {
      it.enabled = true
      it.time = time
      it.timeUnit = timeUnit
      it.custom = null
    }
    return this
  }

  override fun memoryCache(cache: MemoryCache<T>): RepoBuilder<T> {
    this.cacheBuilder.also {
      it.enabled = true
      it.time = DEFAULT_TIME
      it.timeUnit = DEFAULT_UNIT
      it.custom = cache
    }
    return this
  }

  override fun fetcher(fetcher: Fetcher<T>): RepoBuilder<T> {
    this.fetcher = fetcher
    return this
  }

  override fun scheduler(scheduler: () -> Scheduler): RepoBuilder<T> {
    return scheduler(scheduler())
  }

  override fun scheduler(scheduler: Scheduler): RepoBuilder<T> {
    this.scheduler = scheduler
    return this
  }

  @CheckResult
  private fun cacheBuilderToCache(): MemoryCache<T> {
    val cache: MemoryCache<T>
    if (this.cacheBuilder.enabled) {
      val customCache = this.cacheBuilder.custom
      if (customCache == null) {
        cache = MemoryCacheImpl(
            debug,
            this.cacheBuilder.time,
            this.cacheBuilder.timeUnit
        )
      } else {
        cache = customCache
      }
    } else {
      cache = NoopCache.typedInstance()
    }

    return cache
  }

  override fun build(): Repo<T> {
    return RepoImpl(
        fetcher ?: FetcherImpl(debug),
        cacheBuilderToCache(),
        persister ?: NoopPersister.typedInstance(),
        scheduler ?: Schedulers.io(),
        debug
    )
  }

  internal data class MemoryCacheBuilder<T : Any> internal constructor(
    internal var enabled: Boolean,
    internal var time: Long,
    internal var timeUnit: TimeUnit,
    internal var custom: MemoryCache<T>?
  )

  companion object {

    private const val DEFAULT_TIME: Long = 30
    private val DEFAULT_UNIT: TimeUnit = TimeUnit.SECONDS
  }

}

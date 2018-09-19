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
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.RepoBuilder
import com.popinnow.android.repo.noop.NoopCache
import com.popinnow.android.repo.noop.NoopPersister
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

internal class RepoBuilderImpl internal constructor(
  private var fetcher: Fetcher? = null,
  private var persister: Persister? = null,
  private var scheduler: Scheduler? = null,
  private var debug: String = ""
) : RepoBuilder {

  private var cacheBuilder = MemoryCacheBuilder(
      enabled = false,
      time = DEFAULT_TIME,
      timeUnit = DEFAULT_UNIT,
      maxSize = DEFAULT_MAX_SIZE,
      custom = null
  )

  override fun debug(debug: String): RepoBuilder {
    this.debug = debug
    return this
  }

  override fun memoryCache(): RepoBuilder {
    return memoryCache(DEFAULT_TIME, DEFAULT_UNIT)
  }

  override fun memoryCache(
    time: Long,
    timeUnit: TimeUnit
  ): RepoBuilder {
    return memoryCache(time, timeUnit, DEFAULT_MAX_SIZE)
  }

  override fun memoryCache(maxSize: Int): RepoBuilder {
    return memoryCache(DEFAULT_TIME, DEFAULT_UNIT, maxSize)
  }

  override fun memoryCache(
    time: Long,
    timeUnit: TimeUnit,
    maxSize: Int
  ): RepoBuilder {
    this.cacheBuilder.also {
      it.enabled = true
      it.time = time
      it.timeUnit = timeUnit
      it.maxSize = maxSize
      it.custom = null
    }
    return this
  }

  override fun memoryCache(cache: MemoryCache): RepoBuilder {
    this.cacheBuilder.also {
      it.enabled = true
      it.time = DEFAULT_TIME
      it.timeUnit = DEFAULT_UNIT
      it.maxSize = DEFAULT_MAX_SIZE
      it.custom = cache
    }
    return this
  }

  override fun fetcher(fetcher: Fetcher): RepoBuilder {
    this.fetcher = fetcher
    return this
  }

  override fun scheduler(scheduler: () -> Scheduler): RepoBuilder {
    return scheduler(scheduler())
  }

  override fun scheduler(scheduler: Scheduler): RepoBuilder {
    this.scheduler = scheduler
    return this
  }

  @CheckResult
  private fun cacheBuilderToCache(): MemoryCache {
    val cache: MemoryCache
    if (this.cacheBuilder.enabled) {
      val customCache = this.cacheBuilder.custom
      if (customCache == null) {
        cache = MemoryCacheImpl(
            debug,
            this.cacheBuilder.time,
            this.cacheBuilder.timeUnit,
            this.cacheBuilder.maxSize
        )
      } else {
        cache = customCache
      }
    } else {
      cache = NoopCache
    }

    return cache
  }

  override fun build(): Repo {
    return RepoImpl(
        fetcher ?: FetcherImpl(debug),
        cacheBuilderToCache(),
        persister ?: NoopPersister,
        scheduler ?: Schedulers.io(),
        debug
    )
  }

  internal data class MemoryCacheBuilder internal constructor(
    internal var enabled: Boolean,
    internal var time: Long,
    internal var timeUnit: TimeUnit,
    internal var maxSize: Int,
    internal var custom: MemoryCache?
  )

  companion object {

    private const val DEFAULT_TIME: Long = 30
    private val DEFAULT_UNIT: TimeUnit = TimeUnit.SECONDS
    private const val DEFAULT_MAX_SIZE: Int = 8
  }

}

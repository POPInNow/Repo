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

package com.popinnow.android.repo.impl

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.RepoBuilder
import com.popinnow.android.repo.impl.noop.NoopCache
import com.popinnow.android.repo.impl.noop.NoopPersister
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.TimeUnit

internal class RepoBuilderImpl<T : Any> internal constructor(
  private var fetcher: Fetcher<T>? = null,
  private var scheduler: Scheduler? = null,
  private var debug: String = ""
) : RepoBuilder<T> {

  private var cacheBuilder = MemoryCacheBuilder<T>(
      enabled = false,
      time = DEFAULT_MEMORY_TIME,
      timeUnit = DEFAULT_MEMORY_UNIT,
      custom = null
  )

  private var persisterBuilder = PersisterBuilder<T>(
      enabled = false,
      time = DEFAULT_PERSISTER_TIME,
      timeUnit = DEFAULT_PERSISTER_UNIT,
      file = null,
      mapper = null,
      custom = null
  )

  override fun debug(debug: String): RepoBuilder<T> {
    this.debug = debug
    return this
  }

  override fun memoryCache(): RepoBuilder<T> {
    return memoryCache(DEFAULT_MEMORY_TIME, DEFAULT_MEMORY_UNIT)
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
      it.time = DEFAULT_MEMORY_TIME
      it.timeUnit = DEFAULT_MEMORY_UNIT
      it.custom = cache
    }
    return this
  }

  @CheckResult
  private fun mapperFromFunctions(
    serialize: (ArrayList<T>) -> String,
    parse: (String) -> ArrayList<T>
  ): PersisterMapper<T> {
    return object : PersisterMapper<T> {

      override fun serializeToString(data: ArrayList<T>): String {
        return serialize(data)
      }

      override fun parseToObjects(data: String): ArrayList<T> {
        return parse(data)
      }

    }
  }

  override fun persister(
    file: File,
    serialize: (ArrayList<T>) -> String,
    parse: (String) -> ArrayList<T>
  ): RepoBuilder<T> {
    return persister(file, mapperFromFunctions(serialize, parse))
  }

  override fun persister(
    file: File,
    mapper: PersisterMapper<T>
  ): RepoBuilder<T> {
    this.persisterBuilder.also {
      it.enabled = true
      it.time = DEFAULT_PERSISTER_TIME
      it.timeUnit = DEFAULT_PERSISTER_UNIT
      it.file = file
      it.mapper = mapper
      it.custom = null
    }
    return this
  }

  override fun persister(
    time: Long,
    timeUnit: TimeUnit,
    file: File,
    serialize: (ArrayList<T>) -> String,
    parse: (String) -> ArrayList<T>
  ): RepoBuilder<T> {
    return persister(time, timeUnit, file, mapperFromFunctions(serialize, parse))
  }

  override fun persister(
    time: Long,
    timeUnit: TimeUnit,
    file: File,
    mapper: PersisterMapper<T>
  ): RepoBuilder<T> {
    this.persisterBuilder.also {
      it.enabled = true
      it.time = time
      it.timeUnit = timeUnit
      it.file = file
      it.mapper = mapper
      it.custom = null
    }
    return this
  }

  override fun persister(persister: Persister<T>): RepoBuilder<T> {
    this.persisterBuilder.also {
      it.enabled = true
      it.time = DEFAULT_PERSISTER_TIME
      it.timeUnit = DEFAULT_PERSISTER_UNIT
      it.file = null
      it.mapper = null
      it.custom = persister
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
            Logger.create("MemoryCache[$debug]", debug.isNotBlank()),
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

  @CheckResult
  private fun persisterBuilderToPersister(): Persister<T> {
    val persister: Persister<T>
    if (this.persisterBuilder.enabled) {
      val customPersister = this.persisterBuilder.custom
      if (customPersister == null) {
        persister = PersisterImpl(
            Logger.create("Persister[$debug]", debug.isNotBlank()),
            this.persisterBuilder.time,
            this.persisterBuilder.timeUnit,
            scheduler ?: DEFAULT_SCHEDULER,
            // If we have gotten here, these variables should be non-null
            requireNotNull(this.persisterBuilder.file) { "Persister missing 'file'" },
            requireNotNull(this.persisterBuilder.mapper) { "Persister missing 'mapper'" }
        )
      } else {
        persister = customPersister
      }
    } else {
      persister = NoopPersister.typedInstance()
    }

    return persister
  }

  override fun build(): Repo<T> {
    return RepoImpl(
        fetcher ?: FetcherImpl(Logger.create("Fetcher[$debug]", debug.isNotBlank())),
        cacheBuilderToCache(),
        persisterBuilderToPersister(),
        scheduler ?: DEFAULT_SCHEDULER,
        Logger.create("Repo[$debug]", debug.isNotBlank())
    )
  }

  internal data class MemoryCacheBuilder<T : Any> internal constructor(
    internal var enabled: Boolean,
    internal var time: Long,
    internal var timeUnit: TimeUnit,
    internal var custom: MemoryCache<T>?
  )

  internal data class PersisterBuilder<T : Any> internal constructor(
    internal var enabled: Boolean,
    internal var time: Long,
    internal var timeUnit: TimeUnit,
    internal var file: File?,
    internal var mapper: PersisterMapper<T>?,
    internal var custom: Persister<T>?
  )

  companion object {

    private val DEFAULT_SCHEDULER: Scheduler = Schedulers.io()

    private const val DEFAULT_MEMORY_TIME: Long = 30
    private val DEFAULT_MEMORY_UNIT: TimeUnit = TimeUnit.SECONDS

    private const val DEFAULT_PERSISTER_TIME: Long = 10
    private val DEFAULT_PERSISTER_UNIT: TimeUnit = TimeUnit.MINUTES
  }

}

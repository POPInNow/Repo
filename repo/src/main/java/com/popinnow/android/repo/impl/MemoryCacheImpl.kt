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
import com.popinnow.android.repo.MemoryCache
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

internal class MemoryCacheImpl<T : Any> constructor(
  debug: String,
  time: Long,
  timeUnit: TimeUnit
) : MemoryCache<T> {

  private val ttl = timeUnit.toNanos(time)
  private val logger by lazy { Logger("MemoryCache[$debug]", debug.isNotBlank()) }

  // Data backing field
  private val lock = Any()
  @Volatile private var data: Entry<T>? = null

  init {
    logger.log { "Create with TTL: $ttl nano seconds" }
  }

  @CheckResult
  private fun hasCachedData(): Boolean {
    synchronized(lock) {
      val cached = data
      if (cached == null) {
        logger.log { "Cached data is null, do not return" }
        return false
      }

      if (cached.data.isEmpty()) {
        logger.log { "Cached data is empty, do not return" }
        return false
      }

      // If this is still true, then cached was not null, we unwrap with !!
      val cachedTime = cached.time
      val currentTime = System.nanoTime()
      if (cachedTime + ttl < currentTime) {
        logger.log { "Cached time is out of bounds. ${cachedTime + ttl} $currentTime" }
        return false
      } else {
        return true
      }
    }
  }

  override fun get(): Observable<T> {
    return Observable.defer {
      if (hasCachedData()) {
        synchronized(lock) {
          val list = ArrayList(requireNotNull(data).data)
          logger.log { "Memory cache return data: ${ArrayList(list)}" }
          return@defer Observable.fromIterable(list)
        }
      } else {
        logger.log { "Memory cache is empty" }
        clearAll()
        return@defer Observable.empty<T>()
      }
    }
  }

  override fun add(value: T) {
    addToCache { it.add(value) }
  }

  override fun addAll(values: List<T>) {
    addToCache { it.addAll(values) }
  }

  private inline fun addToCache(addToList: (ArrayList<T>) -> Unit) {
    synchronized(lock) {
      val list: ArrayList<T>
      val cached = data
      if (cached == null) {
        list = ArrayList(1)
      } else {
        list = cached.data
      }
      addToList(list)

      // Don't log list or its a ConcurrentModificationError. Wrap in a copy
      val currentTime = System.nanoTime()
      logger.log { "Put in memory cache: ($currentTime) ${ArrayList(list)}" }
      data = Entry(currentTime, list)
    }
  }

  override fun clearAll() {
    synchronized(lock) {
      logger.log { "Cleared" }
      data = null
    }
  }

  private data class Entry<T : Any> internal constructor(
    internal val time: Long,
    internal val data: ArrayList<T>
  )
}

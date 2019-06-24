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
import com.popinnow.android.repo.MemoryCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class MemoryCacheImpl<T : Any> constructor(
  private val logger: Logger,
  time: Long,
  timeUnit: TimeUnit
) : MemoryCache<T> {

  private val ttl = timeUnit.toNanos(time)

  // Data backing field
  private val data: AtomicReference<Entry<T>?> = AtomicReference(null)

  init {
    logger.log { "Create with TTL: $ttl nano seconds" }
  }

  @CheckResult
  private fun hasCachedData(): Boolean {
    val cached = data.get()
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

  @ExperimentalCoroutinesApi
  override suspend fun get(): Flow<T>? {
    if (hasCachedData()) {
      val list = ArrayList(requireNotNull(data.get()).data)
      logger.log { "Memory cache return data: ${ArrayList(list)}" }
      return flow { list.forEach { emit(it) } }
    } else {
      logger.log { "Memory cache is empty" }
      clear()
      return null
    }
  }

  override fun add(value: T) {
    addToCache { it.add(value) }
  }

  private inline fun addToCache(addToList: (ArrayList<T>) -> Unit) {
    val list: ArrayList<T>
    val cached = data.get()
    if (cached == null) {
      list = ArrayList(1)
    } else {
      list = cached.data
    }
    addToList(list)

    // Don't log list or its a ConcurrentModificationError. Wrap in a copy
    val currentTime = System.nanoTime()
    logger.log { "Put in memory cache: ($currentTime) ${ArrayList(list)}" }
    data.set(Entry(currentTime, list))
  }

  override fun clear() {
    logger.log { "Cleared" }
    data.set(null)
  }

  private data class Entry<T : Any> internal constructor(
    internal val time: Long,
    internal val data: ArrayList<T>
  )
}

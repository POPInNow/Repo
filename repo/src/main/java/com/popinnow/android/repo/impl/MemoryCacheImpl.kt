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
import android.support.v4.util.LruCache
import com.popinnow.android.repo.MemoryCache
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

internal class MemoryCacheImpl constructor(
  debug: Boolean,
  time: Long,
  timeUnit: TimeUnit,
  maxSize: Int
) : MemoryCache {

  private val ttl = timeUnit.toNanos(time)
  private val logger = Logger("MemoryCache", debug)
  private val cache = object : LruCache<String, Entry>(maxSize) {
    override fun entryRemoved(
      evicted: Boolean,
      key: String?,
      oldValue: Entry?,
      newValue: Entry?
    ) {
      super.entryRemoved(evicted, key, oldValue, newValue)
      if (evicted) {
        logger.log { "Entry evicted from cache!" }
        logger.log { "  Key: $key" }
        logger.log { "  Old Value: $oldValue" }
        logger.log { "  New Value: $newValue" }
      }
    }
  }

  init {
    logger.log { "Create with TTL: $ttl nano seconds" }
  }

  @CheckResult
  private fun hasCachedData(cached: Entry?): Boolean {
    if (cached == null) {
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

  override fun <T : Any> get(
    key: String,
    mapper: (Any) -> T
  ): Observable<T> {
    return Observable.defer {
      val cached: Entry? = cache.get(key)
      if (hasCachedData(cached)) {
        var list: List<T>?
        try {
          list = requireNotNull(cached).data.map(mapper)
        } catch (e: Exception) {
          logger.logError(e) { "Exception thrown when mapping cached data" }
          list = null
        }

        if (list == null || list.isEmpty()) {
          logger.log { "Memory cache holds bad data, invalidate and return empty" }
          invalidate(key)
          return@defer Observable.empty<T>()
        } else {
          logger.log { "Memory cache return data: ${ArrayList(list)}" }
          return@defer Observable.fromIterable(list)
        }
      } else {
        logger.log { "Memory cache is empty" }
        invalidate(key)
        return@defer Observable.empty<T>()
      }
    }
  }

  override fun add(
    key: String,
    value: Any
  ) {
    addToCache(key) { it.add(value) }
  }

  override fun addAll(
    key: String,
    values: List<Any>
  ) {
    addToCache(key) { it.addAll(values) }
  }

  private inline fun addToCache(
    key: String,
    addToList: (ArrayList<Any>) -> Unit
  ) {
    val cached = cache.get(key)
    val list: ArrayList<Any>
    if (cached == null) {
      list = ArrayList(1)
    } else {
      list = cached.data
    }
    addToList(list)

    // Don't log list or its a ConcurrentModificationError. Wrap in a copy
    val currentTime = System.nanoTime()
    logger.log { "Put in memory cache: ($currentTime) ${ArrayList(list)}" }
    cache.put(key, Entry(currentTime, list))
  }

  override fun invalidate(key: String) {
    logger.log { "Invalidated at: $key" }
    cache.remove(key)
  }

  override fun clearAll() {
    logger.log { "Cleared" }
    cache.evictAll()
  }

  override fun size(): Int {
    return cache.size()
  }

  override fun maxSize(): Int {
    return cache.maxSize()
  }

  override fun trimToSize(maxSize: Int) {
    logger.log { "Trimmed" }
    cache.trimToSize(maxSize)
  }

  private data class Entry internal constructor(
    internal val time: Long,
    internal val data: ArrayList<Any>
  )
}

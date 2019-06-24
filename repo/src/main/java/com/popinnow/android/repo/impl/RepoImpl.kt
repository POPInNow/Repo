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

import androidx.annotation.VisibleForTesting
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.Repo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class RepoImpl<T : Any> internal constructor(
  private val fetcher: Fetcher<T>,
  private val memoryCache: MemoryCache<T>,
  private val persister: Persister<T>,
  private val context: CoroutineContext,
  private val logger: Logger
) : Repo<T> {

  @ExperimentalCoroutinesApi
  private suspend fun fetch(
    fetchCacheAndUpstream: Boolean,
    bustCache: Boolean,
    upstream: () -> Flow<T>
  ): Flow<T> {
    return withContext(context) {
      if (bustCache) {
        justInvalidateBackingCaches()
      }

      if (fetchCacheAndUpstream) {
        logger.log { "Fetch from cache and persister and upstream" }
        return@withContext flow<T> {
          val memory = memoryCache.get()
          if (memory != null) {
            logger.log { "Emit from cache" }
            emitFromCached(this, memory)
          } else {
            val persist = persister.read()
            if (persist != null) {
              logger.log { "Emit from persister" }
              emitFromCached(this, persist)
            }
          }

          logger.log { "Emit from upstream" }
          emitFromUpstream(this, upstream)
        }
      } else {
        logger.log { "Fetch from cache or persister or upstream" }
        return@withContext flow<T> {
          val memory = memoryCache.get()
          if (memory != null) {
            logger.log { "Emit from cache" }
            emitFromCached(this, memory)
          } else {
            val persist = persister.read()
            if (persist != null) {
              logger.log { "Emit from persister" }
              emitFromCached(this, persist)
            } else {
              logger.log { "Emit from upstream" }
              emitFromUpstream(this, upstream)
            }
          }
        }
      }
    }
  }

  @ExperimentalCoroutinesApi
  private suspend fun emitFromUpstream(
    collector: FlowCollector<T>,
    upstream: () -> Flow<T>
  ) {
    val upstreamData = fetcher.fetch(upstream)

    justInvalidateBackingCaches()
    upstreamData.onEach { data ->
      internalPut(data)
      collector.emit(data)
    }
  }

  @ExperimentalCoroutinesApi
  private suspend fun emitFromCached(
    collector: FlowCollector<T>,
    cached: Flow<T>
  ) {
    collector.emit(cached.toCollection(arrayListOf()).last())
  }

  /**
   * Exposed as internal so that it can be tested.
   */
  @VisibleForTesting
  @ExperimentalCoroutinesApi
  internal suspend fun testingGet(
    bustCache: Boolean,
    upstream: () -> Flow<T>
  ): T {
    return fetch(false, bustCache, upstream).first()
  }

  /**
   * Exposed as internal so that it can be tested.
   */
  @VisibleForTesting
  @ExperimentalCoroutinesApi
  internal suspend fun testingObserve(
    bustCache: Boolean,
    upstream: () -> Flow<T>
  ): Flow<T> {
    return fetch(true, bustCache, upstream)
  }

  @ExperimentalCoroutinesApi
  override suspend fun get(
    bustCache: Boolean,
    upstream: () -> T
  ): T {
    val realUpstream = { flowOf(upstream()) }
    return fetch(false, bustCache, realUpstream).first()
  }

  @ExperimentalCoroutinesApi
  override suspend fun observe(
    bustCache: Boolean,
    upstream: () -> Flow<T>
  ): Flow<T> {
    return fetch(true, bustCache, upstream)
  }

  private fun justInvalidateBackingCaches() {
    memoryCache.clear()
    persister.clear()
  }

  private fun internalPut(value: T) {
    logger.log { "Put data: $value" }

    // Store data directly into caches
    memoryCache.add(value)
    persister.write(value)

    // Stop the cachd data but keep the actual underlying in-flighgt since we could be mid get()
    fetcher.clear()
  }

  override fun clear() {
    logger.log { "Clearing caches" }
    memoryCache.clear()
    persister.clear()
    fetcher.clear()
  }

  override fun shutdown() {
    logger.log { "Shutting down Fetcher" }
    fetcher.shutdown()

    clear()
  }

}

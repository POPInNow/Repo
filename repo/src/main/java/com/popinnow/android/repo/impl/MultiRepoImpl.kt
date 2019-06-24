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
import com.popinnow.android.repo.MultiRepo
import com.popinnow.android.repo.Repo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

internal class MultiRepoImpl<T : Any> internal constructor(
  private val repoGenerator: (String) -> Repo<T>
) : MultiRepo<T> {

  private val repoMap: MutableMap<String, Repo<T>> by lazy { ConcurrentHashMap<String, Repo<T>>() }
  private val lock = Any()

  @CheckResult
  private fun repoForKey(key: String): Repo<T> {
    synchronized(lock) {
      val stored: Repo<T>? = repoMap[key]
      if (stored == null) {
        val value = repoGenerator(key)
        repoMap[key] = value
        return value
      } else {
        return stored
      }
    }
  }

  @ExperimentalCoroutinesApi
  override suspend fun observe(
    key: String,
    bustCache: Boolean,
    upstream: () -> Flow<T>
  ): Flow<T> = repoForKey(key).observe(bustCache, upstream)

  @ExperimentalCoroutinesApi
  override suspend fun get(
    key: String,
    bustCache: Boolean,
    upstream: () -> T
  ): T = repoForKey(key).get(bustCache, upstream)

  override fun shutdown(key: String) {
    repoForKey(key).shutdown()
  }

  override fun shutdown() {
    synchronized(lock) {
      repoMap.values.forEach { it.shutdown() }

      repoMap.clear()
    }
  }

  override fun clear(key: String) {
    repoForKey(key).clear()
  }

  override fun clear() {
    synchronized(lock) {
      repoMap.values.forEach { it.clear() }

      repoMap.clear()
    }
  }

}

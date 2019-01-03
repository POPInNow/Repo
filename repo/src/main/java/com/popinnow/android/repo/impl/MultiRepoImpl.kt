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
import com.popinnow.android.repo.MultiRepo
import com.popinnow.android.repo.Repo
import io.reactivex.Observable
import io.reactivex.Single
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

  override fun observe(
    key: String,
    bustCache: Boolean,
    upstream: () -> Observable<T>
  ): Observable<T> = repoForKey(key).observe(bustCache, upstream)

  override fun get(
    key: String,
    bustCache: Boolean,
    upstream: () -> Single<T>
  ): Single<T> = repoForKey(key).get(bustCache, upstream)

  override fun replace(
    key: String,
    value: T
  ) {
    repoForKey(key).replace(value)
  }

  override fun replaceAll(
    key: String,
    values: List<T>
  ) {
    repoForKey(key).replaceAll(values)
  }

  override fun push(
    key: String,
    value: T
  ) {
    repoForKey(key).push(value)
  }

  override fun pushAll(
    key: String,
    values: List<T>
  ) {
    repoForKey(key).pushAll(values)
  }

  override fun invalidate(key: String) {
    repoForKey(key).clearAll()
    synchronized(lock) {
      repoMap.remove(key)
    }
  }

  override fun invalidateCaches(key: String) {
    repoForKey(key).clearCaches()
  }

  override fun clearCaches() {
    synchronized(lock) {
      val repos = repoMap.values
      for (repo in repos) {
        repo.clearCaches()
      }
    }
  }

  override fun clearAll() {
    synchronized(lock) {
      val repos = repoMap.values
      for (repo in repos) {
        repo.clearAll()
      }
      repoMap.clear()
    }
  }

}

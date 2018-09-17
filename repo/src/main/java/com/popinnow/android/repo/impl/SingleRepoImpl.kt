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

import android.support.annotation.VisibleForTesting
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.SingleRepo
import com.popinnow.android.repo.manager.MemoryCacheManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single

@Deprecated("Use RepoImpl<T>")
internal class SingleRepoImpl<T : Any> internal constructor(
  fetcher: Fetcher<T>,
  memoryCache: MemoryCache<T>,
  persister: Persister<T>,
  scheduler: Scheduler,
  debug: Boolean
) : SingleRepo<T> {

  private val delegate by lazy {
    RepoImpl(fetcher, memoryCache, persister, scheduler, debug, "SingleRepoImpl")
  }

  override fun get(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Single<T>
  ): Single<T> {
    return delegate.get(bustCache, key, upstream)
  }

  /**
   * Exposed as internal so that it can be tested.
   */
  @VisibleForTesting
  internal fun testingGet(
    bustCache: Boolean,
    key: String,
    upstream: (String) -> Observable<T>
  ): Single<T> {
    return delegate.testingGet(bustCache, key, upstream)
  }

  override fun memoryCache(): MemoryCacheManager<T> {
    return delegate.memoryCache()
  }

  override fun put(
    key: String,
    value: T
  ) {
    delegate.replace(key, value)
  }

  override fun invalidateCaches(key: String) {
    delegate.invalidateCaches(key)
  }

  override fun invalidate(key: String) {
    delegate.invalidate(key)
  }

  override fun clearCaches() {
    delegate.clearCaches()
  }

  override fun clearAll() {
    delegate.clearAll()
  }

}

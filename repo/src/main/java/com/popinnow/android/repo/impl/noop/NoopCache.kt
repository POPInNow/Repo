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

package com.popinnow.android.repo.impl.noop

import androidx.annotation.CheckResult
import com.popinnow.android.repo.MemoryCache
import io.reactivex.Observable

/**
 * A MemoryCache implementation that does nothing.
 */
internal object NoopCache : MemoryCache<Any> {

  override fun get(): Observable<Any> {
    return Observable.empty()
  }

  override fun add(value: Any) {
  }

  override fun addAll(values: List<Any>) {
  }

  override fun clearAll() {
  }

  /**
   * Return the Noop cache as a typed cache.
   */
  @CheckResult
  internal fun <T : Any> typedInstance(): MemoryCache<T> {
    @Suppress("UNCHECKED_CAST")
    return this as MemoryCache<T>
  }

}

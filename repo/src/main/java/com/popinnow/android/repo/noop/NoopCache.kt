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

package com.popinnow.android.repo.noop

import com.popinnow.android.repo.MemoryCache
import io.reactivex.Observable

/**
 * A MemoryCache implementation that does nothing.
 */
internal object NoopCache : MemoryCache {

  override fun <T : Any> get(
    key: String,
    mapper: (Any) -> T
  ): Observable<T> {
    return Observable.empty()
  }

  override fun add(
    key: String,
    value: Any
  ) {
  }

  override fun addAll(
    key: String,
    values: List<Any>
  ) {
  }

  override fun invalidate(key: String) {
  }

  override fun clearAll() {
  }

  override fun size(): Int {
    return 0
  }

  override fun maxSize(): Int {
    return 0
  }

  override fun trimToSize(maxSize: Int) {
  }

}

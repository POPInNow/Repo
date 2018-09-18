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

package com.popinnow.android.repo

import android.support.annotation.CheckResult
import com.popinnow.android.repo.internal.Invalidatable
import io.reactivex.Observable

/**
 * TODO: NOT A STABLE INTERFACE
 *
 * NOTE: This is not a stable interface. Do not use this.
 *
 * @see Invalidatable
 */
interface Persister : Invalidatable {

  @CheckResult
  fun <T : Any> read(
    key: String,
    mapper: (Any) -> T
  ): Observable<T>

  fun write(
    key: String,
    value: Any
  )

  fun writeAll(
    key: String,
    values: List<Any>
  )

}

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

package com.popinnow.android.repo

import androidx.annotation.CheckResult
import com.popinnow.android.repo.internal.Clearable
import io.reactivex.Observable

/**
 * Persister is an long-term cache for [Observable] which are retrieved from a [Fetcher]
 *
 * The default implementation caches items based on time from the point that the item is written
 * to the persister. The default implementation uses disk backed persistent storage.
 *
 * @see Clearable
 */
interface Persister<T : Any> : Clearable {

  /**
   * Retrieves persistent stored data.
   *
   * If there is no data stored in the persister, the persister will return an [Observable.empty]
   *
   * @return [Observable]
   */
  @CheckResult
  fun read(): Observable<T>

  /**
   * Adds data into the persister.
   *
   * If there is data in the persister already, new data will be appended.
   *
   * @param value The data to put into the persister.
   */
  fun write(value: T)

  /**
   * Clears the persister and deletes its backing storage
   */
  override fun clear()

  /**
   * The interface which defines the mapping of data from Objects to Strings in a consistent,
   * expected format.
   */
  interface PersisterMapper<T : Any> {

    /**
     * Serialize a list of data of arbitrary length into a String
     *
     * @param data List of data of whatever type is to be persisted
     * @return [String]
     */
    @CheckResult
    fun serializeToString(data: ArrayList<T>): String

    /**
     * Parse a String created by the [serializeToString] function back into a list of data
     *
     * @param data String created as a result of some call to [serializeToString]
     * @return [ArrayList]
     */
    @CheckResult
    fun parseToObjects(data: String): ArrayList<T>
  }

}

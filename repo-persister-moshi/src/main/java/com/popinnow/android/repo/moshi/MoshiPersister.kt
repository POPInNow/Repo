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

package com.popinnow.android.repo.moshi

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.RepoBuilder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Extension function for providing a Moshi backed Persister
 *
 * @param file File to persist to
 * @param moshi Moshi instance
 * @param type Data type to persist
 * @return [RepoBuilder]
 */
@CheckResult
fun <T : Any> RepoBuilder<T>.persister(
  file: File,
  moshi: Moshi,
  type: Class<T>
): RepoBuilder<T> {
  return this.persister(file, MoshiPersister.create(moshi, type))
}

/**
 * Extension function for providing a Moshi backed Persister
 *
 * @param time
 * @param timeUnit
 * @param file File to persist to
 * @param moshi Moshi instance
 * @param type Data type to persist
 * @return [RepoBuilder]
 */
@CheckResult
fun <T : Any> RepoBuilder<T>.persister(
  time: Long,
  timeUnit: TimeUnit,
  file: File,
  moshi: Moshi,
  type: Class<T>
): RepoBuilder<T> {
  return this.persister(time, timeUnit, file, MoshiPersister.create(moshi, type))
}

/**
 * PersisterMapper implementation which has its serialization of data powered by Moshi
 *
 * NOTE: The data created by Moshi serialization is not guaranteed to be interoperable with
 * persisted data created by any other [PersisterMapper] implementation.
 */
class MoshiPersister<T : Any> internal constructor(
  moshi: Moshi,
  type: Type
) : PersisterMapper<T> {

  private val adapter: JsonAdapter<List<T>>

  init {
    val token = Types.newParameterizedType(List::class.java, type)
    adapter = moshi.adapter(token)
  }

  /**
   * Serialize to string using Moshi
   *
   * @param data
   * @return [String]
   */
  override fun serializeToString(data: ArrayList<T>): String {
    return adapter.toJson(data) ?: ""
  }

  /**
   * Parse from string using Moshi
   *
   * @param data
   * @return [ArrayList]
   */
  override fun parseToObjects(data: String): ArrayList<T> {
    val list = adapter.fromJson(data) ?: emptyList()
    return ArrayList(list)
  }

  companion object {

    /**
     * Create a new MoshiPersister
     *
     * @param moshi Moshi instance
     * @param type Data type to persist
     * @return [PersisterMapper]
     */
    @JvmStatic
    @CheckResult
    fun <T : Any> create(
      moshi: Moshi,
      type: Class<T>
    ): PersisterMapper<T> {
      return MoshiPersister(moshi, type)
    }

    /**
     * Create a new MoshiPersister
     *
     * @param moshi Moshi instance
     * @param type Data type to persist
     * @return [PersisterMapper]
     */
    @JvmStatic
    @CheckResult
    fun <T : Any> create(
      moshi: Moshi,
      type: Type
    ): PersisterMapper<T> {
      return MoshiPersister(moshi, type)
    }
  }

}


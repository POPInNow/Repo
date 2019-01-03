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

package com.popinnow.android.repo.gson

import androidx.annotation.CheckResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.RepoBuilder
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Extension function for providing a GSON backed Persister
 *
 * @param file File to persist to
 * @param gson GSON instance
 * @param type Data type to persist
 * @return [RepoBuilder]
 */
@CheckResult
fun <T : Any> RepoBuilder<T>.persister(
  file: File,
  gson: Gson,
  type: Class<T>
): RepoBuilder<T> {
  return this.persister(file, GsonPersister.create(gson, type))
}

/**
 * Extension function for providing a GSON backed Persister
 *
 * @param time
 * @param timeUnit
 * @param file File to persist to
 * @param gson GSON instance
 * @param type Data type to persist
 * @return [RepoBuilder]
 */
@CheckResult
fun <T : Any> RepoBuilder<T>.persister(
  time: Long,
  timeUnit: TimeUnit,
  file: File,
  gson: Gson,
  type: Class<T>
): RepoBuilder<T> {
  return this.persister(time, timeUnit, file, GsonPersister.create(gson, type))
}

/**
 * PersisterMapper implementation which has its serialization of data powered by GSON
 *
 * NOTE: The data created by GSON serialization is not guaranteed to be interoperable with
 * persisted data created by any other [PersisterMapper] implementation.
 */
class GsonPersister<T : Any> internal constructor(
  private val gson: Gson,
  private val type: Type
) : PersisterMapper<T> {

  @CheckResult
  private fun typedList(type: Type): Type {
    val token = TypeToken.get(type)
        .type
    return TypeToken.getParameterized(ArrayList::class.java, token)
        .type
  }

  /**
   * Serialize to string using GSON
   *
   * @param data
   * @return [String]
   */
  override fun serializeToString(data: ArrayList<T>): String {
    return gson.toJson(data)
  }

  /**
   * Parse from string using GSON
   *
   * @param data
   * @return [ArrayList]
   */
  override fun parseToObjects(data: String): ArrayList<T> {
    return gson.fromJson(data, typedList(type))
  }

  companion object {

    /**
     * Create a new GsonPersister
     *
     * @param gson GSON instance
     * @param type Data type to persist
     * @return [PersisterMapper]
     */
    @JvmStatic
    @CheckResult
    fun <T : Any> create(
      gson: Gson,
      type: Class<T>
    ): PersisterMapper<T> {
      return GsonPersister(gson, type)
    }

    /**
     * Create a new GsonPersister
     *
     * @param gson GSON instance
     * @param type Data type to persist
     * @return [PersisterMapper]
     */
    @JvmStatic
    @CheckResult
    fun <T : Any> create(
      gson: Gson,
      type: Type
    ): PersisterMapper<T> {
      return GsonPersister(gson, type)
    }
  }

}


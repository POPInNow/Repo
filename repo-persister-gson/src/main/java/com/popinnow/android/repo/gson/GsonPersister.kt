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

package com.popinnow.android.repo.gson

import androidx.annotation.CheckResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.RepoBuilder
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

@CheckResult
fun <T : Any> RepoBuilder<T>.persister(
  file: File,
  gson: Gson,
  type: Class<T>
): RepoBuilder<T> {
  return this.persister(file, GsonPersister.create(gson, type))
}

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

  override fun serializeToString(data: ArrayList<T>): String {
    return gson.toJson(data)
  }

  override fun parseToObjects(data: String): ArrayList<T> {
    return gson.fromJson(data, typedList(type))
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun <T : Any> create(
      gson: Gson,
      type: Class<T>
    ): PersisterMapper<T> {
      return GsonPersister(gson, type)
    }

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


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

package com.popinnow.android.repo.moshi

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.RepoBuilder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File

fun <T : Any> RepoBuilder<T>.persister(
  moshi: Moshi,
  type: Class<T>,
  file: File
): RepoBuilder<T> {
  return this.persister(file, MoshiPersister.create(moshi, type))
}

class MoshiPersister<T : Any> internal constructor(
  moshi: Moshi,
  type: Class<T>
) : PersisterMapper<T> {

  private val adapter: JsonAdapter<ArrayList<T>>

  init {
    val token = Types.newParameterizedType(ArrayList::class.java, type)
    adapter = moshi.adapter(token)
  }

  override fun serializeToString(data: ArrayList<T>): String {
    return adapter.toJson(data) ?: ""
  }

  override fun parseToObjects(data: String): ArrayList<T> {
    return adapter.fromJson(data) ?: arrayListOf()
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun <T : Any> create(
      moshi: Moshi,
      type: Class<T>
    ): PersisterMapper<T> {
      return MoshiPersister(moshi, type)
    }
  }

}


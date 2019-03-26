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

import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.behavior.RepoBehaviorTest
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

class MoshiRepoBehaviorTest : RepoBehaviorTest() {

  override fun provideObserveMapper(): PersisterMapper<String> {
    return provideMapper(String::class.java)
  }

  override fun provideGetMapper(): PersisterMapper<List<String>> {
    val type = Types.newParameterizedType(List::class.java, String::class.java)
    return provideMapper(type)
  }

  private fun <T : Any> provideMapper(type: Type): PersisterMapper<T> {
    val moshi = Moshi.Builder()
        .build()
    return MoshiPersister.create(moshi, type)
  }

}

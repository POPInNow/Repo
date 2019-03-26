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

@file:JvmName("Repos")

package com.popinnow.android.repo

import androidx.annotation.CheckResult
import com.popinnow.android.repo.impl.MultiRepoImpl
import com.popinnow.android.repo.impl.RepoBuilderImpl

/**
 * Create a new RepoBuilder instance
 *
 * @return [RepoBuilder]
 */
@JvmOverloads
@CheckResult
fun <T : Any> newRepoBuilder(
  debug: String = "",
  logger: RepoLogger? = null
): RepoBuilder<T> {
  return RepoBuilderImpl<T>().debug(debug, logger)
}

/**
 * Create a new Repo instance with all defaults
 *
 * @return [Repo]
 */
@JvmOverloads
@CheckResult
fun <T : Any> newRepo(
  debug: String = "",
  logger: RepoLogger? = null
): Repo<T> {
  return newRepoBuilder<T>()
      .debug(debug, logger)
      .memoryCache()
      .build()
}

/**
 * Create a new MultiRepo instance
 *
 * Lazy creates [Repo] instances as they are needed per key.
 * The [generator] function is passed the key argument and is expected to either
 * return a new [Repo] instance or throw an exception.
 *
 * @param generator Repo generator function
 * @return [MultiRepo]
 */
@CheckResult
fun <T : Any> newMultiRepo(generator: (String) -> Repo<T>): MultiRepo<T> {
  return MultiRepoImpl(generator)
}

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

@file:JvmName("Repos")

package com.popinnow.android.repo

import android.support.annotation.CheckResult
import com.popinnow.android.repo.impl.MultiRepoImpl
import com.popinnow.android.repo.impl.RepoBuilderImpl

/**
 * Create a new RepoBuilder instance
 *
 * @return [RepoBuilder]
 */
@CheckResult
fun <T : Any> newRepoBuilder(): RepoBuilder<T> {
  return RepoBuilderImpl()
}

/**
 * Create a new Repo instance with all defaults
 *
 * @return [Repo]
 */
@CheckResult
fun <T : Any> newRepo(): Repo<T> {
  return newRepoBuilder<T>()
      .memoryCache()
      .build()
}

/**
 * Create a new MultiRepo instance
 *
 * @return [MultiRepo]
 */
@CheckResult
fun <T : Any> newMultiRepo(generator: () -> Repo<T>): MultiRepo<T> {
  return MultiRepoImpl(generator)
}

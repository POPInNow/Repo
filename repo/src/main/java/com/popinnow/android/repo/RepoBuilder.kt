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
import io.reactivex.Scheduler
import java.util.concurrent.TimeUnit

/**
 * Builder to construct Repo objects
 */
interface RepoBuilder {

  @CheckResult
  fun debug(debug: Boolean): RepoBuilder

  @CheckResult
  fun fetcher(fetcher: Fetcher): RepoBuilder

  @CheckResult
  fun scheduler(scheduler: Scheduler): RepoBuilder

  @CheckResult
  fun scheduler(scheduler: () -> Scheduler): RepoBuilder

  @CheckResult
  fun memoryCache(): RepoBuilder

  @CheckResult
  fun memoryCache(
    time: Long,
    timeUnit: TimeUnit
  ): RepoBuilder

  @CheckResult
  fun memoryCache(maxSize: Int): RepoBuilder

  @CheckResult
  fun memoryCache(
    time: Long,
    timeUnit: TimeUnit,
    maxSize: Int
  ): RepoBuilder

  @CheckResult
  fun memoryCache(cache: MemoryCache): RepoBuilder

  /**
   * TODO: Expose this method once we have decided how the default Persister implementation will work.
   */
//  @CheckResult
//  fun persister(TODO): RepoBuilder<T, Built>

  @Deprecated("Use build() instead")
  @CheckResult
  fun <T : Any> buildSingle(): SingleRepo<T>

  @Deprecated("Use build() instead")
  @CheckResult
  fun <T : Any> buildObservable(): ObservableRepo<T>

  @CheckResult
  fun build(): Repo

}

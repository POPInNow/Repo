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

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Persister.PersisterMapper
import io.reactivex.Scheduler
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Builder to construct Repo objects
 */
interface RepoBuilder<T : Any> {

  /**
   * Turn on debugging messages by setting a log tag
   *
   * @param debug log tag
   * @return [RepoBuilder]
   */
  @CheckResult
  fun debug(debug: String): RepoBuilder<T>

  /**
   * Provide a custom Fetcher implementation
   *
   * @param fetcher Custom implementation
   * @return [RepoBuilder]
   */
  @CheckResult
  fun fetcher(fetcher: Fetcher<T>): RepoBuilder<T>

  /**
   * Provide a custom scheduler for Fetcher operations
   *
   * @param scheduler Custom implementation
   * @return [RepoBuilder]
   */
  @CheckResult
  fun scheduler(scheduler: Scheduler): RepoBuilder<T>

  /**
   * Provide a custom scheduler for Fetcher operations
   *
   * @param scheduler Custom implementation
   * @return [RepoBuilder]
   */
  @CheckResult
  fun scheduler(scheduler: () -> Scheduler): RepoBuilder<T>

  /**
   * Enable memory caching
   *
   * @return [RepoBuilder]
   */
  @CheckResult
  fun memoryCache(): RepoBuilder<T>

  /**
   * Enable memory caching with a custom timeout
   *
   * @param time
   * @param timeUnit
   * @return [RepoBuilder]
   */
  @CheckResult
  fun memoryCache(
    time: Long,
    timeUnit: TimeUnit
  ): RepoBuilder<T>

  /**
   * Enable memory caching using a custom implementation
   *
   * @param cache Custom implementation
   * @return [RepoBuilder]
   */
  @CheckResult
  fun memoryCache(cache: MemoryCache<T>): RepoBuilder<T>

  /**
   * Enable disk persistence
   *
   * @param file File to save data to
   * @param serialize Mapper function to turn a list of data objects into a String
   * @param parse Mapper function to turn a String into a list of data objects
   * @return [RepoBuilder]
   */
  @CheckResult
  fun persister(
    file: File,
    serialize: (ArrayList<T>) -> String,
    parse: (String) -> ArrayList<T>
  ): RepoBuilder<T>

  /**
   * Enable disk persistence
   *
   * @param file File to save data to
   * @param mapper PersisterMapper interface to convert objects to string representations
   * @return [RepoBuilder]
   */
  @CheckResult
  fun persister(
    file: File,
    mapper: PersisterMapper<T>
  ): RepoBuilder<T>

  /**
   * Enable disk persistence
   *
   * @param time
   * @param timeUnit
   * @param file File to save data to
   * @param serialize Mapper function to turn a list of data objects into a String
   * @param parse Mapper function to turn a String into a list of data objects
   * @return [RepoBuilder]
   */
  @CheckResult
  fun persister(
    time: Long,
    timeUnit: TimeUnit,
    file: File,
    serialize: (ArrayList<T>) -> String,
    parse: (String) -> ArrayList<T>
  ): RepoBuilder<T>

  /**
   * Enable disk persistence
   *
   * @param time
   * @param timeUnit
   * @param file File to save data to
   * @param mapper PersisterMapper interface to convert objects to string representations
   * @return [RepoBuilder]
   */
  @CheckResult
  fun persister(
    time: Long,
    timeUnit: TimeUnit,
    file: File,
    mapper: PersisterMapper<T>
  ): RepoBuilder<T>

  /**
   * Enable disk persistence using a custom implementation
   *
   * @param persister Custom implementation
   * @return [RepoBuilder]
   */
  @CheckResult
  fun persister(persister: Persister<T>): RepoBuilder<T>

  /**
   * Create a new Repo instance using the configured [RepoBuilder]
   * @return [Repo]
   */
  @CheckResult
  fun build(): Repo<T>

}

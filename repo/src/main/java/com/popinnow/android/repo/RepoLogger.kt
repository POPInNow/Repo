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

import com.popinnow.android.repo.impl.Logger

/**
 * Public facing API which default Repo implementations use for logging
 *
 * Logging can potentially be expensive, and should not be relied upon for logic.
 */
interface RepoLogger {

  /**
   * Log a standard debug message
   *
   * @param tag Log tag
   * @param message Log message
   */
  fun log(
    tag: String,
    message: String
  )

  /**
   * Log an error message
   *
   * @param throwable Error object
   * @param tag Log tag
   * @param message Log message
   */
  fun error(
    throwable: Throwable,
    tag: String,
    message: String
  )

  companion object {

    /**
     * Set the implementation of the Logger used
     *
     * @param logger Logger implementation
     */
    fun set(logger: RepoLogger) {
      Logger.setLogger(logger)
    }
  }
}

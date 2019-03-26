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

package com.popinnow.android.repo.impl

import androidx.annotation.CheckResult
import com.popinnow.android.repo.RepoLogger
import com.popinnow.android.repo.logger.TimberLogger

internal class Logger private constructor(
  private val tag: String,
  private val debug: Boolean,
  private val logger: RepoLogger
) {

  inline fun log(lazyMessage: () -> String) {
    if (debug) {
      logger.log(tag, lazyMessage())
    }
  }

  inline fun error(
    throwable: Throwable,
    lazyMessage: () -> String
  ) {
    if (debug) {
      logger.error(throwable, tag, lazyMessage())
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun create(
      tag: String,
      debug: Boolean,
      logger: RepoLogger?
    ): Logger {
      return Logger(tag, debug, logger ?: TimberLogger)
    }

  }
}

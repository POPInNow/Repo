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

package com.popinnow.android.repo.behavior

import com.popinnow.android.repo.MultiRepo
import com.popinnow.android.repo.logger.SystemLogger
import com.popinnow.android.repo.newMultiRepo
import com.popinnow.android.repo.newRepoBuilder
import com.popinnow.android.repo.startNow
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test

class MultiRepoBehaviorTest : BaseBehaviorTest() {

  private var _repo: MultiRepo<String>? = null
  private val repo: MultiRepo<String>
    get() = requireNotNull(_repo)

  private fun shutdown() {
    _repo?.clear()
    _repo = null
  }

  @Before
  fun before() {
    shutdown()
  }

  @After
  fun after() {
    shutdown()
  }

  private fun createRepo(tag: String) {
    _repo = newMultiRepo {
      newRepoBuilder<String>(tag, SystemLogger)
          .scheduler(DEFAULT_SCHEDULER)
          .build()
    }
  }

  @Test
  fun `MultiRepoApi keeps different keys`() {
    createRepo("keeps different keys")
    var upstreamVisited = 0

    repo.get("key1", false) {
      ++upstreamVisited
      return@get Single.just("value1")
    }
        .startNow()
        .test()
        .assertNoErrors()
        .assertValue("value1")
        .assertComplete()

    repo.get("key2", false) {
      ++upstreamVisited
      return@get Single.just("value2")
    }
        .startNow()
        .test()
        .assertNoErrors()
        .assertValue("value2")
        .assertComplete()

    assert(upstreamVisited == 2) { "Upstream should be visited twice" }
  }

  companion object {

    private val DEFAULT_SCHEDULER = Schedulers.trampoline()

  }
}


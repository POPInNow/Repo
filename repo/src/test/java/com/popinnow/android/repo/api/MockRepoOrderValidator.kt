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

package com.popinnow.android.repo.api

import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import io.reactivex.Observable
import io.reactivex.Scheduler

internal class MockRepoOrderValidator<T : Any> internal constructor(
  private val memoryCache: MemoryCache<T>,
  private val persister: Persister<T>,
  private val fetcher: Fetcher<T>
) {

  internal var memoryVisited = false
  internal var persisterVisited = false
  internal var upstreamVisited = false

  internal fun onVisitMemoryReturn(observable: Observable<T>) {
    Mocks.whenever(memoryCache.get())
        .then {
          // Memory should be visited first
          if (!memoryVisited) {
            memoryVisited = true
          } else {
            if (memoryVisited) {
              throw AssertionError("Memory visited twice")
            } else {
              throw AssertionError("Memory should be visited before Persister or Upstream")
            }
          }
          return@then observable
        }
  }

  internal fun onVisitPersisterReturn(observable: Observable<T>) {
    Mocks.whenever(persister.read())
        .then {
          if (memoryVisited && !persisterVisited && !upstreamVisited) {
            persisterVisited = true
          } else {
            if (persisterVisited) {
              throw AssertionError("Persister visited twice")
            } else {
              throw AssertionError(
                  "Persister should be visited after Memory before Upstream"
              )
            }
          }
          return@then observable
        }
  }

  internal fun onVisitUpstreamReturn(
    observable: Observable<T>,
    scheduler: Scheduler,
    upstream: () -> Observable<T>
  ) {
    Mocks.whenever(fetcher.fetch(scheduler, upstream))
        .then {
          // Caching should be visited first
          if ((memoryVisited && !upstreamVisited) || (persisterVisited && !upstreamVisited)) {
            upstreamVisited = true
          } else {
            if (upstreamVisited) {
              throw AssertionError("Upstream visited twice")
            } else {
              throw AssertionError("Upstream should be visited after Persister and Memory")
            }
          }
          return@then observable
        }
  }

}


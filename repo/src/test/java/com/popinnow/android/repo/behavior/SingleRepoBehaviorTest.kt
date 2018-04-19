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

package com.popinnow.android.repo.behavior

import android.support.annotation.CheckResult
import com.popinnow.android.repo.RepoBuilder
import com.popinnow.android.repo.impl.MemoryCacheImpl
import com.popinnow.android.repo.newRepoBuilder
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(MockitoJUnitRunner::class)
class SingleRepoBehaviorTest {

  @CheckResult
  private fun builder(): RepoBuilder<List<String>> {
    return newRepoBuilder<List<String>>().debug(true)
        .scheduler(DEFAULT_SCHEDULER)
  }

  @Test
  fun `SingleRepoBehavior no-cache simple get`() {
    val repo = builder()
        .buildSingle()

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache or upstream
        .assertValue(DEFAULT_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  @Test
  fun `SingleRepoBehavior memory cache simple get`() {
    val memoryCache = MemoryCacheImpl<List<String>>(true, 30, SECONDS, 10)
    val repo = builder()
        .memoryCache(memoryCache)
        .buildSingle()

    // Juice the memory cache
    memoryCache.put(DEFAULT_KEY, DEFAULT_CACHE_EXPECT)

    repo.get(false, DEFAULT_KEY) { throw AssertionError("Upstream should be avoided") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache or upstream
        .assertValue(DEFAULT_CACHE_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  @Test
  fun `SingleRepoBehavior get fills caches`() {
    val repo = builder()
        .memoryCache()
        .buildSingle()

    repo.get(false, DEFAULT_KEY, DEFAULT_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValue(DEFAULT_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()

    repo.get(false, DEFAULT_KEY) { throw AssertionError("Upstream should be avoided") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValue(DEFAULT_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  companion object {

    private const val DEFAULT_KEY = "example-key"
    private val DEFAULT_SCHEDULER = Schedulers.trampoline()
    private val DEFAULT_CACHE_EXPECT = listOf("Hello", "World")
    private val DEFAULT_PERSIST_EXPECT = listOf("Persister", "Defaults")
    private val DEFAULT_FETCH_EXPECT = listOf("Upstream", "Payload")
    private val DEFAULT_UPSTREAM = { _: String -> Single.just(DEFAULT_FETCH_EXPECT) }
  }
}


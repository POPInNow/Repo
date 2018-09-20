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

package com.popinnow.android.repo.api

import android.support.annotation.CheckResult
import com.popinnow.android.repo.Fetcher
import com.popinnow.android.repo.MemoryCache
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.impl.MultiRepoImpl
import com.popinnow.android.repo.impl.RepoImpl
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MultiRepoApiTest {

  @Mock lateinit var fetcher: Fetcher<String>
  @Mock lateinit var memoryCache: MemoryCache<String>
  @Mock lateinit var persister: Persister<String>
  private lateinit var validator: MockRepoOrderValidator<String>

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    validator = MockRepoOrderValidator(memoryCache, persister, fetcher)
  }

  @CheckResult
  private fun createRepo(debug: String): MultiRepoImpl<String> {
    return MultiRepoImpl { RepoImpl(fetcher, memoryCache, persister, DEFAULT_SCHEDULER, debug) }
  }

  @Test
  fun `MultiRepoApi TODO NEED TESTS`() {
    val repo = createRepo("NEED TESTS")
    assert(false)
  }

  companion object {

    private val DEFAULT_SCHEDULER = Schedulers.trampoline()

  }
}


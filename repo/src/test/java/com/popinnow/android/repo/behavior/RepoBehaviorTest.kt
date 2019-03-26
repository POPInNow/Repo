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

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.RepoBuilder
import com.popinnow.android.repo.impl.Logger
import com.popinnow.android.repo.impl.MemoryCacheImpl
import com.popinnow.android.repo.impl.PersisterImpl
import com.popinnow.android.repo.logger.SystemLogger
import com.popinnow.android.repo.newRepoBuilder
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

abstract class RepoBehaviorTest : FileBehaviorTests() {

  private var _repo: Repo<String>? = null
  private val repo: Repo<String>
    get() = requireNotNull(_repo)

  private var _listRepo: Repo<List<String>>? = null
  private val listRepo: Repo<List<String>>
    get() = requireNotNull(_listRepo)

  private fun shutdown() {
    _repo?.shutdown()
    _repo = null

    _listRepo?.shutdown()
    _listRepo = null
  }

  @Before
  fun before() {
    shutdown()
  }

  @After
  fun after() {
    shutdown()
  }

  @CheckResult
  protected abstract fun provideObserveMapper(): PersisterMapper<String>

  @CheckResult
  protected abstract fun provideGetMapper(): PersisterMapper<List<String>>

  @CheckResult
  private fun <T : Any> builder(debug: String): RepoBuilder<T> {
    return newRepoBuilder<T>().debug(debug, SystemLogger)
        .scheduler(DEFAULT_SCHEDULER)
  }

  @CheckResult
  private fun <T : Any> createPersister(
    tag: String,
    time: Long,
    mapper: PersisterMapper<T>,
    file: File? = null
  ): PersisterImpl<T> {
    return PersisterImpl(
        Logger.create(tag, true, SystemLogger),
        time, SECONDS, Schedulers.trampoline(),
        file ?: randomFile(), mapper
    )
  }

  @Test
  fun `RepoBehavior Observable no-cache simple get`() {
    _repo = builder<String>("observable no-cache simple get").build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (no cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable memory cache simple get`() {
    val tag = "observable cache simple get"
    val memoryCache = MemoryCacheImpl<String>(
        Logger.create(tag, true, SystemLogger), 30, SECONDS
    )
    _repo = builder<String>(tag)
        .memoryCache(memoryCache)
        .build()

    // Juice the memory cache
    DEFAULT_OBSERVABLE_CACHE_EXPECT.forEach { memoryCache.add(it) }

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_CACHE_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable get fills caches`() {
    val tag = "observable get fills cache"
    _repo = builder<String>(tag)
        .memoryCache()
        .persister(createPersister(tag, 30, provideObserveMapper()))
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable cached results returned before upstream`() {
    val tag = "observable cache before upstream"
    _repo = builder<String>(tag)
        .memoryCache()
        .persister(createPersister(tag, 30, provideObserveMapper()))
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    repo.observe(false) { Observable.fromArray("New", "Data") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + arrayListOf("New", "Data"))
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Observable only previous cached result returned`() {
    val tag = "observable only previous cache returns"
    _repo = builder<String>(tag)
        .memoryCache()
        .persister(createPersister(tag, 30, provideObserveMapper()))
        .build()

    repo.observe(false, DEFAULT_OBSERVABLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT)
        .assertComplete()

    val data = arrayListOf(
        "New", "Data", "But", "I", "Wonder", "How", "Long", "Its", "Going", "To", "Be", "Before",
        "We", "Start", "Running", "Out", "of", "Memory"
    )
    repo.observe(false) { Observable.fromIterable(data) }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(DEFAULT_OBSERVABLE_FETCH_EXPECT + data)
        .assertComplete()

    repo.observe(false) { Observable.fromIterable(DEFAULT_OBSERVABLE_CACHE_EXPECT) }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream
        .assertValueSequence(data + DEFAULT_OBSERVABLE_CACHE_EXPECT)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Single no-cache simple get`() {
    _listRepo = builder<List<String>>("single no-cache simple get").build()

    listRepo.get(false, DEFAULT_SINGLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache or upstream
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Single memory cache simple get`() {
    val tag = "single cache simple get"
    val memoryCache = MemoryCacheImpl<List<String>>(
        Logger.create(tag, true, SystemLogger), 30, SECONDS
    )
    _listRepo = builder<List<String>>(tag)
        .memoryCache(memoryCache)
        .build()

    // Juice the memory cache
    memoryCache.add(DEFAULT_SINGLE_CACHE_EXPECT)

    listRepo.get(false) { throw AssertionError("Upstream should be avoided") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache or upstream
        .assertValue(DEFAULT_SINGLE_CACHE_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  @Test
  fun `RepoBehavior Single get fills caches`() {
    val tag = "single get fills caches"
    _listRepo = builder<List<String>>(tag)
        .memoryCache()
        .persister(createPersister(tag, 30, provideGetMapper()))
        .build()

    listRepo.get(false, DEFAULT_SINGLE_UPSTREAM)
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()

    listRepo.get(false) { throw AssertionError("Upstream should be avoided") }
        .startNow()
        .test()
        .assertNoErrors()
        // Cache then upstream (empty cache)
        .assertValue(DEFAULT_SINGLE_FETCH_EXPECT)
        .assertValueCount(1)
        .assertComplete()
  }

  companion object {

    private val DEFAULT_SCHEDULER = Schedulers.trampoline()

    private val DEFAULT_OBSERVABLE_CACHE_EXPECT = arrayListOf("Hello", "World")
    private val DEFAULT_OBSERVABLE_FETCH_EXPECT = arrayListOf("Upstream", "Payload")
    private val DEFAULT_OBSERVABLE_UPSTREAM = {
      Observable.fromIterable(DEFAULT_OBSERVABLE_FETCH_EXPECT)
    }

    private val DEFAULT_SINGLE_CACHE_EXPECT = listOf("Hello", "World")
    private val DEFAULT_SINGLE_FETCH_EXPECT = listOf("Upstream", "Payload")
    private val DEFAULT_SINGLE_UPSTREAM = { Single.just(DEFAULT_SINGLE_FETCH_EXPECT) }
  }
}


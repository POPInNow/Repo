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
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.Persister.PersisterMapper
import com.popinnow.android.repo.impl.PersisterImpl
import com.popinnow.android.repo.startNow
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okio.appendingSink
import okio.buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

abstract class PersisterBehaviorTest : BaseBehaviorTest() {

  @CheckResult
  abstract fun provideMapper(): PersisterMapper<String>

  private val tempFiles = LinkedHashSet<File>()

  @CheckResult
  private fun randomFile(): File {
    val filePrefix = UUID.randomUUID()
        .toString()
    val file = createTempFile("test$filePrefix")
    tempFiles.add(file)
    return file
  }

  @Before
  @After
  fun beforeTests() {
    tempFiles.forEach {
      if (it.exists()) {
        it.delete()
      }
    }
    tempFiles.clear()
  }

  @CheckResult
  private fun createPersister(
    debug: String,
    time: Long,
    file: File? = null
  ): PersisterImpl<String> {
    return PersisterImpl(
        debug, time, SECONDS, Schedulers.trampoline(),
        file ?: randomFile(), provideMapper()
    )
  }

  private fun assertPersisterIsEmpty(
    persister: Persister<String>
  ) {
    persister.read()
        .startNow()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete()
  }

  private fun assertPersisterSingleValue(
    persister: Persister<String>,
    value: String
  ) {
    persister.read()
        .startNow()
        .test()
        .assertValue(value)
        .assertNoErrors()
        .assertComplete()
  }

  private fun assertPersisterValues(
    persister: Persister<String>,
    vararg values: String
  ) {
    persister.read()
        .startNow()
        .test()
        .assertValueSequence(arrayListOf(*values))
        .assertNoErrors()
        .assertComplete()
  }

  private fun fillPersisterWithDummyData(
    persister: PersisterImpl<String>,
    size: Int,
    onComplete: () -> Unit
  ) {
    if (size == 0) {
      onComplete()
      return
    } else {
      persister.write("data$size") {
        fillPersisterWithDummyData(persister, size - 1, onComplete)
      }
    }
  }

  @CheckResult
  private fun populateFile(data: String): File {
    val file = randomFile()
    file.createNewFile()
    file.appendData(data)
    return file
  }

  private fun File.appendData(data: String) {
    appendingSink()
        .buffer()
        .use { it.writeUtf8(data) }
  }

  /**
   * Starts empty by default, assuming a file does not exist.
   * If nothing puts into the persister, it returns no data.
   */
  @Test
  fun `PersisterBehaviorTest new file is empty by default`() {
    val persister = createPersister("new file empty default", 30)
    assertPersisterIsEmpty(persister)
  }

  /**
   * If a file exists, the persister starts populated
   * If nothing puts into the persister, it returns no data.
   */
  @Test
  fun `PersisterBehaviorTest existing file persists data`() {
    // Populate a file with stuff
    val file = populateFile(FAKE_DATA_AS_STRING)

    val persister = createPersister("existing file persists data", 30, file)
    assertPersisterValues(persister, *FAKE_DATA_AS_OBJECT.toTypedArray())
  }

  /**
   * Data persists in the persister during the time TTL
   */
  @Test
  fun `PersisterBehaviorTest maintains persistence`() {
    val persister = createPersister("maintains persistence", 30)
    persister.writeAll(FAKE_DATA_AS_OBJECT) {
      assertPersisterValues(persister, *FAKE_DATA_AS_OBJECT.toTypedArray())
      assertPersisterValues(persister, *FAKE_DATA_AS_OBJECT.toTypedArray())
    }
  }

  /**
   * Data times out after TTL elapses
   */
  @Test
  fun `PersisterBehaviorTest times out`() {
    val persister = createPersister("times out", 1)
    persister.writeAll(FAKE_DATA_AS_OBJECT) {
      assertPersisterValues(persister, *FAKE_DATA_AS_OBJECT.toTypedArray())

      // After time out passes, persister does not return value
      Observable.just("")
          .delay(2, SECONDS)
          .startNow()
          .blockingSubscribe { assertPersisterIsEmpty(persister) }
    }

  }

  /**
   * When you invalidate the persister, the old data is gone immediately
   */
  @Test
  fun `PersisterBehaviorTest invalidates`() {
    val persister = createPersister("invalidates", 30)
    persister.write("Hello") {
      // So it returns data on query
      assertPersisterSingleValue(persister, "Hello")

      // Invalidating the persister then clears it
      persister.clear()

      // So it does not return
      assertPersisterIsEmpty(persister)
    }
  }

  /**
   * When you clear the persister, the old data is gone immediately
   */
  @Test
  fun `PersisterBehaviorTest clears`() {
    val persister = createPersister("clears", 30)
    persister.writeAll(FAKE_DATA_AS_OBJECT) {
      // So it returns data on query
      assertPersisterValues(persister, *FAKE_DATA_AS_OBJECT.toTypedArray())

      // Put some more data
      fillPersisterWithDummyData(persister, 5) {

        // Invalidating the persister then clears it
        persister.clear()

        // So it does not return
        assertPersisterIsEmpty(persister)
      }
    }
  }

  /**
   * Calling put in the middle of a stream from the persister does not modify the output of the stream
   */
  @Test
  fun `PersisterBehaviorTest write does not modify stream`() {
    val persister = createPersister("write does not modify", 30)
    val expect1 = "Testing"
    val expect2 = "Puts"

    fun persisterNextWrite() {
      persister.write(expect2) {
        persister.read()
            .startNow()
            .test()
            // The write happens after two puts but before the last
            .assertValueSequence(arrayListOf(expect1, expect2))
            .assertValueCount(2)
            .assertComplete()
      }
    }

    persister.write(expect1) {
      persister.read()
          .doOnSubscribe {
            persisterNextWrite()
          }
          .startNow()
          .test()
          // The next write happens after the persister is accessed, so it should not appear
          .assertValue(expect1)
          .assertValueCount(1)
          .assertComplete()
    }
  }

  companion object {

    private val FAKE_DATA_AS_OBJECT = listOf("Hello", "World")
    private const val FAKE_DATA_AS_STRING = "[\"Hello\", \"World\"]"

  }

}

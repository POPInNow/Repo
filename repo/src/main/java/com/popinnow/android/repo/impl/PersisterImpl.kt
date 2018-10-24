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

package com.popinnow.android.repo.impl

import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import com.popinnow.android.repo.Persister
import com.popinnow.android.repo.Persister.PersisterMapper
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.util.concurrent.TimeUnit

internal class PersisterImpl<T : Any> internal constructor(
  debug: String,
  time: Long,
  timeUnit: TimeUnit,
  private val scheduler: Scheduler,
  private val file: File,
  private val mapper: PersisterMapper<T>
) : Persister<T> {

  private val ttl = timeUnit.toMillis(time)
  private val logger by lazy { Logger("Persister[$debug]", debug.isNotBlank()) }

  private val lock = Any()

  init {
    logger.log { "Create with TTL: $ttl milli seconds" }
  }

  @CheckResult
  private fun isFileValid(): Boolean {
    synchronized(lock) {
      if (!file.exists()) {
        logger.log { "File ${file.absolutePath} does not exist, do not return" }
        return false
      }

      // Read last write time from file
      // Files can only go to millisecond accuracy
      val cachedTime = file.lastModified()
      val currentTime = System.currentTimeMillis()
      if (cachedTime + ttl < currentTime) {
        logger.log { "Cached time is out of bounds. ${cachedTime + ttl} $currentTime" }
        return false
      } else {
        return true
      }
    }
  }

  @CheckResult
  private fun readFromFile(): ArrayList<T> {
    synchronized(lock) {
      file.source()
          .buffer()
          .use {
            val allContents = it.readUtf8()
            logger.log { "Read all contents from cache file: '$allContents'" }

            if (allContents.isBlank()) {
              logger.log { "$file is empty, unable to map from json" }
              return arrayListOf()
            }

            try {
              val data = mapper.parseToObjects(allContents)
              logger.log { "Map json to data: $data" }
              return data
            } catch (e: Exception) {
              logger.error(e) { "Error mapping json to data" }
              return arrayListOf()
            }
          }
    }
  }

  override fun read(): Observable<T> {
    return Observable.defer {
      if (isFileValid()) {
        val data = readFromFile()
        if (data.isEmpty()) {
          logger.log { "Persister is empty" }
          clearAll()
          return@defer Observable.empty<T>()
        } else {
          logger.log { "Persister return data: ${ArrayList(data)}" }
          return@defer Observable.fromIterable(data)
        }
      } else {
        logger.log { "Persister is empty" }
        clearAll()
        return@defer Observable.empty<T>()
      }
    }
  }

  override fun write(value: T) {
    nonBlockingWriteList(listOf(value), append = true, onWriteComplete = EMPTY_WRITE_CALLBACK)
  }

  @VisibleForTesting
  internal fun write(
    value: T,
    onWriteComplete: (Boolean) -> Unit
  ) {
    nonBlockingWriteList(listOf(value), append = true, onWriteComplete = onWriteComplete)
  }

  override fun writeAll(values: List<T>) {
    nonBlockingWriteList(values, append = true, onWriteComplete = EMPTY_WRITE_CALLBACK)
  }

  @VisibleForTesting
  internal fun writeAll(
    values: List<T>,
    onWriteComplete: (Boolean) -> Unit
  ) {
    nonBlockingWriteList(values, append = true, onWriteComplete = onWriteComplete)
  }

  private fun nonBlockingWriteList(
    values: List<T>,
    append: Boolean,
    onWriteComplete: (Boolean) -> Unit
  ) {
    Completable.fromAction { writeList(values, append) }
        .subscribeOn(scheduler)
        .unsubscribeOn(scheduler)
        .observeOn(scheduler)
        .subscribe(object : CompletableObserver {
          override fun onComplete() {
            logger.log { "Wrote '$values' to $file" }
            onWriteComplete(true)
          }

          override fun onSubscribe(d: Disposable) {
            logger.log { "Writing '$values' to $file" }
          }

          override fun onError(e: Throwable) {
            logger.error(e) { "Failed to write '$values' to $file" }
            onWriteComplete(false)
          }
        })
  }

  private fun writeList(
    values: List<T>,
    append: Boolean
  ) {
    val existingData: ArrayList<T>
    if (append) {
      existingData = readFromFile()
    } else {
      existingData = arrayListOf()
    }

    existingData.addAll(values)
    val success = writeFile(existingData)
    if (!success) {
      clearAll()
    }
  }

  @CheckResult
  private fun writeFile(data: ArrayList<T>): Boolean {
    synchronized(lock) {
      if (!file.isFile || file.isDirectory) {
        logger.log { "File provided is a directory" }
        return false
      }

      if (!file.exists()) {
        if (file.createNewFile()) {
          logger.log { "Created new file: $file" }
        } else {
          logger.log { "Failed to create new file: $file" }
        }
      }

      file.sink()
          .buffer()
          .use {
            try {
              val json = mapper.serializeToString(data)
              logger.log { "Map data to json: $json" }
              it.writeUtf8(json)

              // Update last modified time which is used as the TTL
              // Files can only go to millisecond accuracy
              file.setLastModified(System.currentTimeMillis())
              return true
            } catch (e: Exception) {
              logger.error(e) { "Error mapping data to json" }
              return false
            }
          }
    }
  }

  override fun clearAll() {
    synchronized(lock) {
      if (file.exists()) {
        if (file.delete()) {
          logger.log { "Cleared" }
        }
      }
    }
  }

  companion object {

    private val EMPTY_WRITE_CALLBACK: (Boolean) -> Unit = {}
  }

}

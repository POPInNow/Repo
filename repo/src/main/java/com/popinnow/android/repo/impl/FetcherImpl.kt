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

import com.popinnow.android.repo.Fetcher
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of the SingleFetcher interface
 *
 * The SingleFetcher will memoryCache any in flight upstream requests until they have completed.
 */
internal class FetcherImpl<T : Any> internal constructor(
  private val logger: Logger
) : Fetcher<T> {

  private val activeTask = AtomicReference<Deferred<T>?>(null)

  override suspend fun fetch(
    context: CoroutineContext,
    upstream: () -> T
  ): T {
    // If we have an already active task, join to it
    activeTask.get()
        ?.let { task ->
          logger.log { "Joining fetch call to an already active task: $task" }
          return task.await()
        }

    // New scope to run our work in
    return coroutineScope {
      // Create a new lazy task which will run once await() is called for the first time.
      val newTask = async(context = context, start = LAZY) {
        logger.log { "Hit upstream to fetch new data" }
        return@async upstream()
      }.apply {
        // Once this task has completed, clear out the activeTask if this is the current one running.
        invokeOnCompletion {
          logger.log { "Fetcher completed, clear out activeTask if possible" }
          activeTask.compareAndSet(this, null)
        }
      }

      // Loop until we can confidently either launch a newTask or attach to the activeTask
      val result: T
      while (true) {
        if (!activeTask.compareAndSet(null, newTask)) {
          // If we do have a current task, we can join it
          val currentTask = activeTask.get()
          if (currentTask != null) {
            // Cancel the new task in case it is running, we don't need it since we are attaching
            newTask.cancel()
            logger.log { "Attach to currently running task and await completion" }
            result = currentTask.await()
            break
          } else {
            // Retry the loop after yielding the context to any other waiting actors
            yield()
          }
        } else {
          logger.log { "Launch new task and await completion" }
          result = newTask.await()
          break
        }
      }

      return@coroutineScope result
    }
  }

  override fun shutdown() {
    // Cancel an active task if it exists
    Timber.d("Shutdown Fetcher")
    activeTask.get()
        ?.cancel()

    // Clear the activeTask state
    clearActiveTask()
  }

  override fun clear() {
    // Set the currently tracked task to null
    //
    // This will make the activeTask available to be set again and fire a new request
    // but it will not cancel the existing task if one is running.
    //
    // You will lose the ability to shutdown a running task by clearing the Fetcher.
    Timber.d("Clear Fetcher")
    clearActiveTask()
  }

  private fun clearActiveTask() {
    activeTask.set(null)
  }

}

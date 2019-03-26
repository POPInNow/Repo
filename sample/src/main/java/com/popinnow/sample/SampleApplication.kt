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

package com.popinnow.sample

import android.app.Application
import android.content.Context
import androidx.annotation.CheckResult
import com.popinnow.android.repo.newRepo
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Repo instances are scoped to the component they are created in.
 * A Repo instance can only cache data within the scope it is created in.
 * Repo instances are not inherently Singleton - but can be if they are
 * created in a Singleton scope such as the Application context.
 */
class SampleApplication : Application() {

  private val observableRepo = newRepo<Int>()
  private val singleRepo = newRepo<String>()

  private val mockDataSourceString = SampleMockDataSourceString()
  private val mockDataSourceInt = SampleMockDataSourceInt()

  fun shutdownRepos() {
    observableRepo.shutdown()
    singleRepo.shutdown()
  }

  @CheckResult
  fun getWithObservableRepo(bustCache: Boolean): Observable<Int> {
    return observableRepo.observe(bustCache) {
      Observable.just(mockDataSourceInt.getCount(bustCache))
          .doOnSubscribe {
            Logger.debug(
                "ObservableApplication Source Subscribe on Thread: ${Thread.currentThread().name}"
            )
          }
          .doOnNext {
            Logger.debug(
                "ObservableApplication Source Emit on Thread: ${Thread.currentThread().name}"
            )
          }
    }
  }

  @CheckResult
  fun getWithSingleRepo(bustCache: Boolean): Single<String> {
    return singleRepo.get(bustCache) {
      Single.just(mockDataSourceString.getCharacter(bustCache))
          .doOnSubscribe {
            Logger.debug(
                "SingleApplication Source Subscribe on Thread: ${Thread.currentThread().name}"
            )
          }
          .doOnSuccess {
            Logger.debug("SingleApplication Source Emit on Thread: ${Thread.currentThread().name}")
          }
    }
  }

}

@CheckResult
fun Context.getSampleApplication(): SampleApplication {
  if (this is Application) {
    if (this is SampleApplication) {
      return this
    } else {
      throw ClassCastException("Application is not SampleApplication")
    }
  } else {
    return applicationContext.getSampleApplication()
  }
}

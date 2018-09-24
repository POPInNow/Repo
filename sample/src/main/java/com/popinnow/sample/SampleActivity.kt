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

package com.popinnow.sample

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.popinnow.android.repo.newRepoBuilder
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers

/**
 * Repo instances are scoped to the component they are created in.
 *
 * A Repo instance scoped to an Activity will be garbage collected only when the subscription to
 * the Repo upstream is terminated. In order to avoid leaking memory in the event that an upstream
 * fetch takes a long time - be sure to call
 *
 * [com.popinnow.android.repo.internal.Invalidatable.invalidate]
 * or
 * [com.popinnow.android.repo.internal.Clearable.clearAll]
 *
 * when tearing down the Activity.
 */
class SampleActivity : AppCompatActivity() {

  private lateinit var applicationLevelDisposeSwitch: SwitchCompat
  private var applicationLevelDispose = false

  private lateinit var observableMockSwitch: SwitchCompat
  private lateinit var observableApplicationSwitch: SwitchCompat
  private lateinit var singleMockSwitch: SwitchCompat
  private lateinit var singleApplicationSwitch: SwitchCompat

  private lateinit var observableMockButton: Button
  private lateinit var observableApplicationButton: Button
  private lateinit var singleMockButton: Button
  private lateinit var singleApplicationButton: Button

  private var bustCacheObservableMock = false
  private var bustCacheObservableApplication = false
  private var bustCacheSingleMock = false
  private var bustCacheSingleApplication = false

  private var observableMockDisposable = Disposables.empty()
  private var observableApplicationDisposable = Disposables.empty()
  private var singleMockDisposable = Disposables.empty()
  private var singleApplicationDisposable = Disposables.empty()

  private val mockDataSourceString = SampleMockDataSourceString()
  private val mockDataSourceInt = SampleMockDataSourceInt()

  private val observableRepo = newRepoBuilder<String>()
      .memoryCache()
      .build()

  private val singleRepo = newRepoBuilder<Int>()
      .memoryCache()
      .build()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    applicationLevelDisposeSwitch = findViewById(R.id.application_level_dispose)
    observableMockSwitch = findViewById(R.id.observable_mock_switch)
    observableApplicationSwitch = findViewById(R.id.observable_application_switch)
    singleMockSwitch = findViewById(R.id.single_mock_switch)
    singleApplicationSwitch = findViewById(R.id.single_application_switch)

    observableMockButton = findViewById(R.id.observable_mock)
    observableApplicationButton = findViewById(R.id.observable_application)
    singleMockButton = findViewById(R.id.single_mock)
    singleApplicationButton = findViewById(R.id.single_application)

    applicationLevelDisposeSwitch.setOnCheckedChangeListener { _, isChecked ->
      applicationLevelDispose = isChecked
    }
    observableMockSwitch.setOnCheckedChangeListener { _, isChecked ->
      bustCacheObservableMock = isChecked
    }
    observableApplicationSwitch.setOnCheckedChangeListener { _, isChecked ->
      bustCacheObservableApplication = isChecked
    }
    singleMockSwitch.setOnCheckedChangeListener { _, isChecked ->
      bustCacheSingleMock = isChecked
    }
    singleApplicationSwitch.setOnCheckedChangeListener { _, isChecked ->
      bustCacheSingleApplication = isChecked
    }

    setupObservableMockButton()
    setupObservableApplicationButton()
    setupSingleMockButton()
    setupSingleApplication()
  }

  private fun setupObservableMockButton() {
    observableMockButton.setOnClickListener { _ ->
      // Cancel the request before launching a new one
      observableMockDisposable.dispose()
      observableRepo.clearAll()

      // Even though the subscription was disposed, if the cache is not busted you'll see the
      // original data and then see the new counter data
      observableMockDisposable =
          observableRepo.observe(bustCacheObservableMock) {
            Observable.just(mockDataSourceString.getCharacter(bustCacheObservableMock))
                .doOnSubscribe {
                  Logger.debug(
                      "ObservableMock Source Subscribe on Thread: ${Thread.currentThread().name}"
                  )
                }
                .doOnNext {
                  Logger.debug(
                      "ObservableMock Source Emit on Thread: ${Thread.currentThread().name}"
                  )
                }
          }
              .subscribeOn(Schedulers.computation())
              .observeOn(AndroidSchedulers.mainThread())
              .doOnSubscribe {
                Logger.debug(
                    "ObservableMock Observer Subscribe on Thread: ${Thread.currentThread().name}"
                )
              }
              .doOnNext {
                Logger.debug(
                    "ObservableMock Observer Emit on Thread: ${Thread.currentThread().name}"
                )
              }
              .subscribe({
                Logger.info("ObservableMock subscribed: $it")
              }, {
                Logger.error(it, "ObservableMock error")
              }, {
                Logger.info("ObservableMock complete")
              })
    }
  }

  private fun setupObservableApplicationButton() {
    observableApplicationButton.setOnClickListener { v ->
      // Cancel the disposable but not the upstream request
      observableApplicationDisposable.dispose()

      // Even though the subscription was disposed, if the cache is not busted you'll see the
      // original data and then see the new counter data
      //
      // Since this repo lives in Application scope, it will keep its cached state even if you
      // close and re-open the application. Upstream requests will not be cancelled unless you
      // stop them at the Application exit point.
      observableApplicationDisposable = v.context.getSampleApplication()
          .getWithObservableRepo(bustCacheObservableApplication)
          .subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSubscribe {
            Logger.debug(
                "ObservableApplication Observer Subscribe on Thread: ${Thread.currentThread().name}"
            )
          }
          .doOnNext {
            Logger.debug(
                "ObservableApplication Observer Emit on Thread: ${Thread.currentThread().name}"
            )
          }
          .subscribe(
              { Logger.info("ObservableApplication subscribed: $it") },
              { Logger.error(it, "ObservableApplication error") },
              { Logger.info("ObservableApplication complete") }
          )
    }
  }

  private fun setupSingleMockButton() {
    singleMockButton.setOnClickListener { _ ->
      // Cancel the request before launching a new one
      singleMockDisposable.dispose()
      singleRepo.clearAll()

      // Even though the subscription was disposed, if the cache is not busted you'll see the
      // original data instead of the new counter data
      singleMockDisposable = singleRepo.get(bustCacheSingleMock) {
        Single.just(mockDataSourceInt.getCount(bustCacheSingleMock))
            .doOnSubscribe {
              Logger.debug("SingleMock Source Subscribe on Thread: ${Thread.currentThread().name}")
            }
            .doOnSuccess {
              Logger.debug("SingleMock Source Emit on Thread: ${Thread.currentThread().name}")
            }
      }
          .subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSubscribe {
            Logger.debug("SingleMock Observer Subscribe on Thread: ${Thread.currentThread().name}")
          }
          .doOnSuccess {
            Logger.debug("SingleMock Observer Emit on Thread: ${Thread.currentThread().name}")
          }
          .subscribe(
              { Logger.info("SingleMock subscribed: $it") },
              { Logger.error(it, "SingleMock error") }
          )
    }
  }

  private fun setupSingleApplication() {
    singleApplicationButton.setOnClickListener { v ->
      // Cancel disposable but not the upstream request
      singleApplicationDisposable.dispose()

      // Even though the subscription was disposed, if the cache is not busted you'll see the
      // original data instead of the new counter data
      //
      // Since this repo lives in Application scope, it will keep its cached state even if you
      // close and re-open the application. Upstream requests will not be cancelled unless you
      // stop them at the Application exit point.
      singleApplicationDisposable = v.context.getSampleApplication()
          .getWithSingleRepo(bustCacheSingleApplication)
          .subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSubscribe {
            Logger.debug(
                "SingleApplication Observer Subscribe on Thread: ${Thread.currentThread().name}"
            )
          }
          .doOnSuccess {
            Logger.debug(
                "SingleApplication Observer Emit on Thread: ${Thread.currentThread().name}"
            )
          }
          .subscribe(
              { Logger.info("SingleApplication subscribed: $it") },
              { Logger.error(it, "SingleApplication error") }
          )
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    // Make sure to both dispose of the Disposable that the Observer is subscribed to, as well as
    // invalidating or clearing the Repo to fully stop operation.
    observableMockDisposable.dispose()
    singleMockDisposable.dispose()

    observableRepo.clearAll()
    singleRepo.clearAll()

    observableApplicationDisposable.dispose()
    singleApplicationDisposable.dispose()

    // If you want to tear down all Application level Repos as well, set this flag.
    if (applicationLevelDispose) {
      this.getSampleApplication()
          .clearRepos()
    }
  }
}

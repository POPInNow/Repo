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

import androidx.annotation.CheckResult
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing

object Mocks {

  @JvmStatic
  @CheckResult
  fun <T : Any> whenever(methodCall: T?): OngoingStubbing<T> {
    return Mockito.`when`(methodCall)!!
  }

//  @JvmStatic
//  @CheckResult
//  fun <T : Any> answerWhen(
//    methodCall: T?,
//    answer: (InvocationOnMock) -> Any
//  ): T {
//    return Mockito.doAnswer(answer).`when`(methodCall)!!
//  }

//  @JvmStatic
//  @CheckResult
//  fun <T : Any> throwWhen(
//    methodCall: T?,
//    throwable: Throwable
//  ): T {
//    return Mockito.doThrow(throwable).`when`(methodCall)!!
//  }

}

# Repo

[Repo](https://github.com/POPinNow/Repo) is a simple library which provides a basic implementation
of the client side
[Repository pattern](https://msdn.microsoft.com/en-us/library/ff649690.aspx) for Android development.
It is heavily inspired by similar existing libraries such as [Store](https://github.com/nytimes/store).

## Install

In your `build.gradle`

```gradle
dependencies {
  implementation "com.popinnow.android.repo:repo:0.0.8"
}
```

## Why

Repo was built both as an educational exercise and with the goal of providing easy support for
in flight requests to an abstract upstream data source using a reactive stream. Repo is built on
top of the RxJava implementation of the reactive streams specification for JVM languages.

The Repo library is used internally in the [POPin Android application.](https://play.google.com/store/apps/details?id=com.popinnow.gandalf)

## Goals

The Repo library was built as an education project with two goals in mind.

- Provide a simple transparent implementation of in flight upstream request caching with the option
  for light memory caching.
- Be easy to adopt with very little code change for a project which already receives data using
  a reactive stream, but may not yet implement a repository pattern to receive that data.

## How

There are two different ways to create a new Repo via the RepoBuilder interface. Depending on if
you are accessing Repo via `Java` or `Kotlin`, the entry point looks a little different.

Kotlin:
```kotlin
fun main(args: Array<String>) {
  
  val repo = newRepoBuilder()
    // Enable memory level caching
    .memoryCache()
    // Build Repo instance
    .build()
}
```

Java:
```java
class MyClass {
  
  public static void main(String... args) {
    final Repo repo = Repos.newRepoBuilder()
      // Enable memory level caching
      .memoryCache()
      // Build Repo instance
      .build();
  }
  
}
```

Applying `Repo` to your existing architecture is simple and rewarding. `Repo` is most useful for  
data operations where your application requests data from an upstream source and does not do any  
kind of caching already.

Let us assume you have, for example, an upstream network source using
[Retrofit](https://github.com/square/retrofit) that is fetching a `Single`

```kotlin
interface MyService {
  
  @GET("/some-url")
  fun fetchDataFromUpstream(key: String) : Single<String>
  
}
  
class MyClass {
  
  private val myService = createService(MyService::class.java)
  
  fun test () {
    val key = "myservice"
    
    // Fetches from upstream every time
    myService.fetchDataFromUpstream(key)
      .map { transformData(it) }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe()
  }
  
}


```

You can add simple memory caching as well as making sure the repeated requests fire only a single  
network call by wrapping the call to `MyService.fetchDataFromUpstream` with a `Repo` instance:

```kotlin
interface MyService {
  
  @GET("/some-url")
  fun fetchDataFromUpstream(key: String) : Single<String>
  
}
  
class MyClass {
  
  private val repo = newRepoBuilder()
    .memoryCache()
    .build()
    
  private val myService = createService(MyService::class.java)
  
  fun test () {
    // Fetches from upstream once, and then from the cache each time after
    repo.get(bustCache = false, key = "my-service") { key -> myService.fetchDataFromUpstream(key) }
      .map { transformData(it) }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe()
  }
  
}

```

#### Data Format

`Repo` instances map upstreams to keys, which are represented as a simple string key.

```kotlin
fun test() {
  val repo = newRepoBuilder().memoryCache().build()
  
  val arg1 = "Hello"
  val arg2 = "World"
  repo.get(bustCache = false, key = "some unique string") { key -> upstream(arg1, arg2) }
    .subscribeOn(Schedulers.io())
    .subscribeOn(AndroidSchedulers.mainThread())
    .subscribe()
}
```

#### In Flight Caching

Once an upstream request is fired off, the active upstream request will be cached for the duration  
of its lifetime. This means that subsequent calls to the same data source will not fire additional  
requests to the upstream data source. Any new calls for the same upstream data will be attached  
to the existing call, and data will be delivered to all subscribers once the upstream call  
resolves. At the point of resolution, any new upstreams calls to the data source will fire off a  
new upstream request with the same in-flight caching behavior.

#### In Memory Caching

A common problem in mobile development is the persistence of data during configuration changes.
`Repo` provides a simple way to store data at whatever scoped level you prefer by building your
`Repo` instance with the `memoryCache()` method. By default, this will attach a memory cache
to the instance which caches requests on write for the next 30 seconds. Any request made to the
Repo during this period that does not bust the cache will return data from the memory cache.

A call to `Repo.get()` will cache the returned data from upstream Single sources and return either
the cached data or fresh data from the upstream source. A call to `Repo.get()` will either subscribe  
the caller to the original upstream source, or subscribe the caller to a stream of cached
data from the upstream source.

A call to `Repo.observe()` will cache the returned data from upstream Observable sources and return  
the latest cached data, and then fresh data from the upstream source. A call to `Repo.observe()`  
will always subscribe the caller to the original upstream source, but will always emit cached data  
first.

Any side effects directly on the upstream source will not be
replayed to the caller if the caller is subsribed to the cached observable, but all side effects
on the subscribed Single or Observable instance will be emitted in both cases.

```kotlin
fun test() {
  val repo = newRepoBuilder()
    .memoryCache()
    .build()
  
  // Logging will only happen on fresh requests from the upstream
  // Upstream request will only happen if there is no valid cached data
  repo.get(bustCache, key) { key -> upstreamSingle().doOnSuccess { logUpstream(it) } }
    .subscribeOn(Schedulers.io())
    .subscribeOn(AndroidSchedulers.mainThread())
    .subscribe()
  
  // Logging will happen on every call to the Repo instance whether it fetches data
  // from the upstream source or from the cache interface
  // Upstream request will only happen if there is no valid cached data
  repo.get(bustCache, key) { key -> upstreamSingle() }
    .doOnNext { logEverytime(it) }
    .subscribeOn(Schedulers.io())
    .subscribeOn(AndroidSchedulers.mainThread())
    .subscribe()
    
  // Logging will only happen on fresh requests from the upstream
  // Upstream request will always happen, and will be delivered after cached data if it exists
  repo.observe(bustCache, key) { key -> upstreamObservable().doOnNext { logUpstream(it) } }
    .subscribeOn(Schedulers.io())
    .subscribeOn(AndroidSchedulers.mainThread())
    .subscribe()
    
  // Logging will happen on every call to the Repo instance whether it fetches data
  // from the upstream source or from the cache interface
  // Upstream request will always happen, and will be delivered after cached data if it exists
  repo.observe(bustCache, key) { key -> upstreamObservable() }
    .doOnNext { logEverytime(it) }
    .subscribeOn(Schedulers.io())
    .subscribeOn(AndroidSchedulers.mainThread())
    .subscribe()
}
```

#### On Disk Caching

On disk caching is not supported yet but will exposed in the future through a `Persister` interface
which is added to the builder via one of the two Builder entry points.

#### Invalidating Requests and Caches

There are various levels of fine grained control you can use to stop requests or invalidate caches.

When you `get` from a Repo instance, you will receive a normal RxJava `Disposable` which should be  
disposed of as normal. Note that disposing the returned Observable will not stop the actual `upstream`  
subscription, so any network call will continue to happen in the background.

To actually stop the upstream call, you can use the `Repo.invalidate(String)` method, which will  
stop the upstream request if any exists and clear any caches for the provided `key`.

To just clear any caches but keep any upstream requests alive, you can call `Repo.invalidateCaches(String)`.

To operate on every key in the Repo, you can use `Repo.clearAll()` which performs similarly to  
`Repo.invalidate(String)` except that it invalidates everything, or you can use `Repo.clearCaches()`  
which I hope at this point is also rather self-explanatory.

Please keep in mind that due to the hands off nature of the Repo library, calling `dispose()` on an  
Observable will not call `invalidate()`, and calling `invalidate()` will not call `dispose()`.

For a complete clean up of `Repo` resources once you are done using them, you would do something like:
```kotlin
fun test() {
  val repo = newRepoBuilder()
    .memoryCache()
    .build()
  
  // Do stuff with repo ...
  val disposable = repo.get(false, "my-key") { upstream() }
    .map { it.transform() }
    .subscibe()
  
  // Stop just a single managed request
  repo.invalidate("my-key")
  
  // Or stop everything
  repo.clearAll()
  
  // Make sure to dispose as well!
  disposable.dispose()
}
```

## Credits

This library is primarily built and maintained by [Peter Yamanaka](https://github.com/pyamsoft)
at [POPin](https://github.com/POPinNow).  
The Repo library is used internally in the
[POPin Android application.](https://play.google.com/store/apps/details?id=com.popinnow.gandalf)


# Support

Please feel free to make an issue on GitHub, leave as much detail as possible regarding  
the question or the problem you may be experiencing.


# Contributions

Contributions are welcome and encouraged. The project is written entirely in Kotlin and  
follows the [Square Code Style](https://github.com/square/java-code-styles) for `SquareAndroid`.


## License

Apache 2

```
Copyright (C) 2018 POP Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

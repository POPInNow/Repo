# Repo

[Repo](https://github.com/POPinNow/Repo) is a simple library which provides a basic implementation
of the client side
[Repository pattern](https://msdn.microsoft.com/en-us/library/ff649690.aspx) for Android development.
It is heavily inspired by similar existing libraries such as [Store](https://github.com/nytimes/store).

## Install

In your `build.gradle`

```gradle
dependencies {
  def latestVersion = "0.2.1"

  implementation "com.popinnow.android.repo:repo:$latestVersion"

  // GSON powered persister
  implementation "com.popinnow.android.repo:repo-persister-gson:$latestVersion"

  // Moshi powered persister
  implementation "com.popinnow.android.repo:repo-persister-moshi:$latestVersion"
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

## Quick Start

Applying `Repo` to your existing architecture is simple and rewarding. `Repo` is most useful for  
data operations where your application requests data from an upstream source and does not do any  
kind of caching already.

Let us assume you have, for example, an upstream network source using
[Retrofit](https://github.com/square/retrofit) that is fetching a `Single`

Before:
```kotlin
interface MyService {
  
  @GET("/some-url")
  fun fetchDataFromUpstream(key: String) : Single<String>
  
}
  
class MyClass {
  
  private val myService = createService(MyService::class.java)
  
  fun test() {
    // Fetches from upstream every time
    myService.fetchDataFromUpstream(key)
      .map { transformData(it) }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe()
  }
  
}
```

After:
```kotlin
interface MyService {
  
  @GET("/some-url")
  fun fetchDataFromUpstream(key: String) : Single<String>
  
}
  
class MyClass {
    
  private val myService = createService(MyService::class.java)
  
  // Add a Repo which will cache the latest results for arbitrary String data
  private val repo = newRepoBuilder<String>()
    .memoryCache()
    .build()
  
  fun test() {
    // Fetches from upstream once, and then from the cache each time after
    repo.get(bustCache = false) { myService.fetchDataFromUpstream(key) }
      .map { transformData(it) }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe()
  }
  
}
```

## What is It

`Repo` is an implementation of the client side repository pattern, and is implemented in three  
layers.

`Fetcher` which interacts with and tracks an upstream data source.  
`MemoryCache` which caches data from the `Fetcher` in short term memory storage.  
`Persister` which caches data from the `Fetcher` in long term disk storage.  

## Basics

Creating a new instance of a `Repo` object is through a couple of entry points:
```kotlin
class MyClass {
  
  fun test() {
    
    // A new Repo created through a customized RepoBuilder
    val customRepo = newRepoBuilder<String>()
      // Enable debugging with a custom log tag
      .debug("my log tag")
      // Enable in-memory caching
      .memoryCache()
      // Run the upstream requests on a custom Scheduler
      .scheduler(Schedulers.computation())
      // Build the Repo!
      .build()
      
    // A new default Repo instance - debugging off and memoryCaching on
    val defaultRepo = newRepo<Int>() 
  }
}
```

`Repo` instances are intentionally simple, and they only track and manage against a single object.  
This helps to keep `Repo` very lightweight and fits most general use cases - which is that the  
developer wants caching of results from a single endpoint or a database table.

For cases where multiple different but similar pieces of data need to be managed, such as caching  
different Notes or Events based on a Note `id` or Event `id`, a `MultiRepo` interface is provided  
which effectively provides a Map abstraction over multiple `Repo` instances.

```kotlin
class MyMultiClass {
  
  fun test() {
    // Create a new MultiRepo - needs a generator function which will lazily create Repo instances
    // as needed
    val noteMultiRepo = newMultiRepo { newRepo<Note>() }
    
    noteMultiRepo.get("note-id", bustCache = false) { noteApi.getNote("note-id") }
      .map { transformNote(it) }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe()
      
    // Create a new MultiRepo - needs a generator function which will lazily create Repo instances
    // as needed
    val eventMultiRepo = newMultiRepo { 
      newRepoBuilder<Event>()
        .memoryCache(30, TimeUnit.MINUTES)
        .build()
    }
    
    eventMultiRepo.get("event-id", bustCache = false) { eventDatabase.getEvent("event-id") }
      .map { broadcastEvent(it) }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe()
  }
}
```

### Repo Layers

A `Repo` instance will always have a `Fetcher` implementation, and can optionally have a  
`MemoryCache` or a `Persister`.

`Repo` does not care what the data format it is interacting with looks like, it only cares that  
the data comes bundled in either an `RxJava` `Single` or `Observable`.

The default `Fetcher` implementation provides in-flight upstream request debouncing - meaning that  
if `fetch()` is called while a previous `fetch` call is still attempting to finish, a second call  
to the upstream will not be made. Instead, the caller will wait for the first `fetch` to finish, and  
will receive those results. 

The default `MemoryCache` implementation will cache any data put into it for a period of 30 seconds  
by default. The `MemoryCache` preserves ordering, but is backed by an unbounded data structure - so  
potentially endless emissions can make the cache extremely large or can in some extreme cases cause  
out of memory errors. If one is using `MemoryCache` to observe against an endless upstream source,  
one may need to periodically clear out or replace the cache in order to stay within memory sane  
constraints.

The default `Persister` implementation will cache any data put into it for a period of 10 minutes  
by default. The `Persister` preserves ordering and is backed by a flat file on disk. The `Persister`  
saves information to disk by serializing data model objects to a `String`. While there is no  
serializer enforced by default, the library ships with two basic implementations of a serializers,  
one powered by [GSON](https://github.com/google/gson) and one powered by
[Moshi](https://github.com/square/moshi).  

### Getting Data from Repo Instances

`Repo` instances are interacted with in two different ways - the `get()` and `observe()` functions.  

`get()` is used for one time operations - such as a REST API call. Data returned from the upstream  
should be in the form of an Rx `Single`. If there is already data cached in the `Repo`, the latest  
data will be returned following the "cache-or-upstream" pattern - if cache exists, it will be  
returned and the upstream will never be hit, else the upstream will be hit and the results cached.

`observe()` is used for potentially long running operations - such as watching for live updates to  
a database table. Data returned from the upstream should be in the form of an Rx `Observable`. If  
there is already data cached in the `Repo`, all valid data will be returned following the  
"cache-then-upstream" pattern - if cache exists, it will be returned first, and the upstream will  
be hit once the cache is returned.

`MultiRepo` follows the same API but expects an additional `key` argument to identify which `Repo`  
instance it is interacting with.

### Adding Data into Repo Instances

`Repo` instances manipulated in two different ways - the `push()` and `replace()` functions.

`push()` is used to append a single piece of data into the `Repo` instance - it will become the  
latest valid data and will reset any cache timeouts. A variant exists to push a list of data, named  
`pushAll()`.

`replace()` is used to replace a single piece of data into the `Repo` instance - it will erase any  
already cached data, reset cache timeouts, and then be inserted, becoming the latest valid data.  
A variant exists to replace an entire list of data, named `replaceAll()`.

`MultiRepo` follows the same API but expects an additional `key` argument to identify which `Repo`  
instance it is interacting with.

### Removing Data in Repo Instances

Data in `Repo` instances can removed in two different ways - the `canel()` and `clear()` functions.  

`clear()` will only clear data from the `Repo` instance's caching layer. Any stored data will  
be cleared out, but any currently active requests through a `Fetcher` to an upstream data source  
will not be cancelled. This frees the memory up to be garbage collected, but will not stop requests.

`cancel()` will clear out data from the `Repo`, and cancel any currently active requests through  
a `Fetcher` to an upstream data source.

`MultiRepo` follows the same API, but will operate on all of it's held `Repo` instances. To operate  
on an individual `Repo` held within a `MultiRepo`, the `clear(String)` and `cancel(String)`  
functions are provided - which operate similarly to calling `clear()` or `cancel()` on the  
`Repo` instance directly.

## Community

The `Repo` library welcomes contributions of all kinds - it does not claim to be perfect code.  
Any improvements that can be made to the usability or the efficiency of the project will be greatly  
appreciated.

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
Copyright (C) 2019 POP Inc.

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

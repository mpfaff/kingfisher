---
---

# Kingfisher

![](https://upload.wikimedia.org/wikipedia/commons/0/0c/BeltedKingfisherJG_Male.jpg)

A super-duper spectacular multi-language web server.

## Scripts

Scripts are placed in the `scripts` directory.

Supported languages:
- JavaScript
- Python

### State

Scripts cannot maintain state in global variables. Instead, they must call `getState(initializerFunction)` at most once.

The initializer function can return any type of value, though there are some caveats:
- `getState` returns the value, so if the value is a primitive (like a number or a string), there will be no way to modify it
- The value must be [sharable](#sharing-data).

### Sharing data

Often you will want to maintain state across executions of your scripts, or will want to send data between scripts. Unfortunately, not all data can be used this way. Data that can be used this way is considered **sharable**.

Sharable data may be accessed by multiple [script threads](#execution-model) simultaneously. Despite this, sharable data *may* not be [thread-safe](#thread-safety).

Primitives such as booleans, numbers, and strings[^1] are sharable in all (if not all, I consider that a bug) supported languages.

### Execution model

Every invocation of a script, including every invocation of a handler registered by a script, is executed on a new **script thread**.

### Thread safety

Thread safety covers the ability for multiple threads to access some data or perform some operation simultaneously, safely. That is, without causing corruption or losing information due to [data races](https://en.wikipedia.org/wiki/Race_condition#Data_race).

Immutable, sharable data is inherently thread-safe, because there is nothing that can be modified and cause any problems.

Kingfisher provides some data structures with varying levels of thread-safety. All the provided data structures support reads from multiple threads in the absence of any threads writing to the data structure.

#### `JArray`

Arrays are thread-safe[^2]. Getting and setting elements across multiple threads is completely safe.

#### `JList`

Lists are somewhat thread-safe[^2]. Getting and setting elements across multiple threads is completely safe in the absence of other types of mutations.

Other types of mutations, if not used with some kind of locking mechanism, will have data races.

#### `JObject`

Objects are thread-safe[^2]. Getting and setting fields (may be used as an object or a map) across multiple threads is completely safe.

#### `JMap`

Maps are somewhat thread-safe[^2]. However, any time that a `JMap`'s thread-safety will be sufficient, a `JObject` may be used instead with that safety properly enforced.

Any operations aside from reads and value writes are not thread-safe, will have data races, and may cause corruption.

#### `ConcurrentMap`

Concurrent maps are completely thread-safe. However, synchronization may still be necessary depending on your use case.

For example, you may want updates to be synchronized but allow reads to see the state as of the latest completed update. In that case, you could use a lock around the code where the updates are performed, but not the writes.

In another case, you may want reads and writes to be synchronized so that reads see only the very latest state, blocking if an update is in progress. For this, a `JMap` guarded by a `RWLock` would be more optimal.

### Server-side rendering

Server-side rendering is supported via scripting, and template rendering. The template engine used is [Pebble](https://pebbletemplates.io/).

To render a template, use the [`render(name, context)`]({{ '/api/javadoc/kingfisher/scripting/Api.html#render(java.lang.String,java.util.Map)' | relative_url }}) function.

Templates must be placed inside the `templates` directory.

Script code can be executed within templates using the `code | lang(args...)` syntax. Supported languages for embedding within templates are JavaScript (`js`) and Python (`py`). The code must be a string that evaluate to a function that accepts `args...`.

See the examples below for... examples!

### Examples

#### [Counter]({% link examples/counter.md %})

#### [Templating (trivial)]({% link examples/templating-trivial.md %})

### [API]({% link api/index.md %})

[^1]: Strings are only considered a primitive when they are immutable, which is always the case in Python and JavaScript.

[^2]: as "thread-safe" as the Java Memory Model provides for when using non-atomic loads and stores.

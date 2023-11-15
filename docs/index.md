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
- The value must be [thread-safe](#thread-safety).

### Thread safety

Primitives such as booleans, integers, numbers, and strings are thread-safe in most (if not all) supported languages.

Objects and lists, on the other hand, are not. If you need such data structures to be accessible from multiple threads at once (as is the case with script state), you must make use of Java types provided by the API such as `JMap`, `JObject`, and `ConcurrentMap` (TODO: implement `JList`) instead.

### Server-side rendering

Server-side rendering is supported via scripting, and template rendering. The template engine used is [Pebble](https://pebbletemplates.io/).

Templates must be placed inside the `templates` directory. Custom code can be executed within templates using the `code | lang(args...)` syntax. Supported languages for embedding within templates are JavaScript (`js`) and Python (`py`). The code must evaluate to a function that accepts `args...`.

See the examples below for... examples!

### Examples

#### [Counter]({% link examples/counter.md %})

#### [Templating (trivial)]({% link examples/templating-trivial.md %})

### [API]({% /api | relative_url %})

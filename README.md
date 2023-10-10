# \<name\>

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

### JavaScript

Here is a simple counter example in JavaScript:

```js
state = getState(() => ConcObject({counter: 0}));

addRoute(GET, "/count", req => {
    let count = state.counter++
    return respond().status(200).content(count.toString()).html().finish()
});
```

### Python

Here is a simple counter example in Python:

```py
state = getState(lambda: JObject({'counter': 0}))

def handle_counter(req, args):
    count = state.counter
    state.counter = count + 1
    return respond(200).content(str(count)).html().finish()

addRoute(GET, "/count", handle_counter)
```

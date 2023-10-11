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

Here is a simple counter example...

... in **Javascript**:

```js
state = getState(() => ConcObject({counter: 0}));

addRoute(GET, "/count", req => {
    let count = state.counter++
    return respond().status(200).content(count.toString()).html().finish()
});
```

... in **Python**:

```py
state = getState(lambda: JObject({'counter': 0}))

def handle_counter(req, args):
    count = state.counter
    state.counter = count + 1
    return respond(200).content(str(count)).html().finish()

addRoute(GET, "/count", handle_counter)
```

Here is a simple template rendering example...

... in **Javascript**:

```js
addRoute(GET, "/hello/(?<name>.+)", (req, args) => {
    return respond().status(200).content(render("hello_name.html", args)).html().finish()
})
```

... in **Python**:

```py
def handle_hello_name(req, args):
    return respond().status(200).content(render("hello_name.html", args)).html().finish()

addRoute(GET, "/hello/(?<name>.+)", handle_hello_name)
```

and the corresponding template:

```html
<h1>Hello, {{"name => name.toUpperCase()" | js(name) | escape}}!</h1>
```

This might look strange. Fear not! Let's break it down:

```
{{
```

This starts a template expression.

```
"name => name.toUpperCase()"
```

This is simply a string with some code. In this case, JavaScript.

```
| js(name)
```

This pipes (`|`) the previous expression (the string) through the filter (`js`), specified immediately after. In parentheses, we provide the argument `name` to the filter. The `js` filter evaluates the input string (the one before the `|`), then calls it with all the arguments that were passed to the filter.

```
| escape
```

This pipes the previous expression (the evaluation result) through the filter (`escape`). Parentheses are only required if arguments are passed.

This filter escapes the input value to prevent XSS vulnerabilities. You should use this filter whenever inserting user input into templates.

The `escape` filter accepts an optional `strategy` argument, which accepts the following values:

- `"html"`
- `"js"`
- `"css"`
- `"url_param"`
- `"json"`

The default strategy is `"html"`, so most of the time you will not need to specify a strategy.

```
}}
```

This ends a template expression.

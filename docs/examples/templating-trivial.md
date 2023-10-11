---
---

# Examples > Templating (trivial)

Here is an example of template rendering. This example is rather trivial, but it should give you an idea of how templates work.

In **Javascript**:

```js
addRoute(GET, "/hello/(?<name>.+)", (req, args) => {
    return respond().status(200).content(render("hello_name.html", args)).html().finish()
})
```

In **Python**:

```py
def handle_hello_name(req, args):
    return respond().status(200).content(render("hello_name.html", args)).html().finish()

addRoute(GET, "/hello/(?<name>.+)", handle_hello_name)
```

And the corresponding **template**:

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

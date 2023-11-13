---
render_with_liquid: false
---

# Examples > Counter

Here is an example of an endpoint where each request increments a counter and the value (pre-increment) is sent back to the client as the response body.

In **Javascript**:

```js
state = getState(() => JObject({counter: 0}));

addRoute(GET, "/count", req => {
    let count = state.counter++
    return respond().status(200).content(count.toString()).html().finish()
});
```

In **Python**:

```py
state = getState(lambda: JObject({'counter': 0}))

def handle_counter(req, args):
    count = state.counter
    state.counter = count + 1
    return respond(200).content(str(count)).html().finish()

addRoute(GET, "/count", handle_counter)
```

state = getState(() => ConcObject({counter: 0}));

addRoute(GET, "/foo", req => respond().status(200).content("Hello from JS!").html().finish());

addRoute(GET, "/count_with_js", req => {
    let count = state.counter++
    return respond().status(200).content(count.toString()).html().finish()
});

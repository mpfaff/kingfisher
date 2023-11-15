state = getState(() => new JObject({counter: 0}));

addRoute(GET, "/", req => respond().content("Hello, World!").html().finish());

addRoute(GET, "/hello_with_js", req => respond().content("Hello from JS!").html().finish());

addRoute(GET, "/count_with_js", req => {
    let count = state.counter++;
    return respond().content(count.toString()).html().finish();
});

addRoute(GET, "/hello_with_js/(?<name>.+)", (req, args) => {
    return respond().content(render("hello_name.html", args)).html().finish();
});

addRoute(GET, "/error", async req => {
    throw new Error();
});

state = getState(() => JObject({counter: 0}));

addRoute(GET, "/foo", req => respond().status(200).content("Hello from JS!").html().finish());

addRoute(GET, "/count_with_js", req => {
    let count = state.counter++
    return respond().status(200).content(count.toString()).html().finish()
});

addRoute(GET, "/hello_with_js/(?<name>.+)", (req, args) => {
    return respond().status(200).content(render("hello_name.html", args)).html().finish()
})

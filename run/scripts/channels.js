addRoute(GET, "/hello_polyglot_js/(?<name>.+)", async (req, args) => {
    print(`Hello, ${args.name}! I am JavaScript!`);
    await send("hello_py", args.name);
    return respond().finish();
});

addStatelessChannel("hello_js", name => print(`Hello, ${name}! I am JavaScript!`));

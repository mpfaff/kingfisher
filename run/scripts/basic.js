state = getState(() => JObject({counter: 0}));

addRoute(GET, "/", req => respond().status(200).content("Hello, World!").html().finish());

addRoute(GET, "/hello_with_js", req => respond().status(200).content("Hello from JS!").html().finish());

addRoute(GET, "/count_with_js", req => {
    let count = state.counter++;
    return respond().status(200).content(count.toString()).html().finish();
});

addRoute(GET, "/hello_with_js/(?<name>.+)", (req, args) => {
    return respond().status(200).content(render("hello_name.html", args)).html().finish();
});

addRoute(GET, "/fetch_test", async req => {
    print("got request");
    const texts = await Promise.all(Array.from(Array(10), async () => {
        return await (await fetch("https://baconipsum.com/api/?type=meat-and-filler")).text();
    }));
    print("got responses");
    return respond().status(200).content(Array.from(texts).join("\n\n---\n\n")).finish();
});

addRoute(GET, "/fetch_html_test", async req => {
    return respond()
        .status(200)
        .content(await (await fetch("https://example.com/")).text())
        .finish();
});

addRoute(GET, "/fetch_html_test_error", async req => {
    return respond()
        .status(200)
        .content(await (await fetch("https://examplqwnjdnjqwe.com/")).text())
        .finish();
});

addRoute(GET, "/fetch_post_text_body", async req => {
    return respond()
        .status(200)
        .content(await (await fetch("https://httpbin.org/post", {method: POST, body: "Hello, World!"})).text())
        .finish();
});

addRoute(GET, "/fetch_post_binary_body", async req => {
    const buf = new Uint32Array([0xDEADBEEF]);
    return respond()
        .status(200)
        .content(await (await fetch("https://httpbin.org/post", {method: POST, body: buf.buffer})).text())
        .finish();
});

async function loadTodos() {
    let json = "[]";
    try {
        json = await fs.readFile("data/todos.json", 'utf8');
    } catch (e) {
        print(`Caught exception while loading TODOs: ${e}`);
    }
    print(json);
    try {
        return JSON.parse(json);
    } catch (e) {
        print(`Caught exception while loading TODOs: ${e}`);
        throw e;
    }
}

addRoute(GET, "/todos", async req => {
    let todos = await loadTodos();
    try {
        return respond()
            .status(200)
            .content(render("todos.html", {todos}))
            .finish();
    } catch (e) {
        print(`Caught exception while rendering TODOs: ${e}`);
        throw e;
    }
});

addRoute(POST, "/todos", async req => {
    const content = req.bodyString;
    let todos = await loadTodos();
    try {
        todos.push({content, state: "todo"});
        await fs.mkdir("data");
        await fs.writeFile("data/todos.json", JSON.stringify(todos));
        return respond()
            .status(201)
            .content(`Added.`)
            .finish();
    } catch (e) {
        print(`Caught exception while adding TODO: ${e}`);
        throw e;
    }
});

addRoute(GET, "/error", async req => {
    throw new Error();
});

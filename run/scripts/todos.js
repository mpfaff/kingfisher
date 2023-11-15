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
        return respond(201)
            .content(`Added.`)
            .finish();
    } catch (e) {
        print(`Caught exception while adding TODO: ${e}`);
        throw e;
    }
});

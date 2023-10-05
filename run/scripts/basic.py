import time

state = getState(lambda: ConcObject({'counter': 0}))

addRoute(GET, "/", lambda req: respond().status(200).content("Hello, World!").html().finish())

def handle_hello_name(req):
    name = req.argument(0)
    return respond().status(200).content(render("hello_name.html", {'name': name})).html().finish()

addRoute(GET, "/hello/:name", handle_hello_name)

def handle_sleep(req):
    time.sleep(4)
    return respond().status(200).content("I have slept!").html().finish()

addRoute(GET, "/sleep", handle_sleep)

def handle_counter(req):
    count = state.counter
    state.counter = count + 1
    return respond(200).content(str(count)).html().finish()

addRoute(GET, "/count_with_py", handle_counter)

def handle_internal_error(req):
    raise Exception()

addRoute(GET, "/internal_error", handle_internal_error)

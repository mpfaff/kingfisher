import time

state = getState(lambda: JObject({'counter': 0}))

addRoute(GET, "/hello_with_py", lambda req, args: respond().content("Hello, World!").html().finish())

def handle_hello_name(req, args):
    return respond().content(render("hello_name.html", args)).html().finish()

addRoute(GET, "/hello_with_py/(?<name>.+)", handle_hello_name)

def handle_sleep(req, args):
    time.sleep(4)
    return respond().content("I have slept!").html().finish()

addRoute(GET, "/sleep", handle_sleep)

def handle_counter(req, args):
    count = state.counter
    state.counter = count + 1
    return respond(200).content(str(count)).html().finish()

addRoute(GET, "/count_with_py", handle_counter)

def handle_internal_error(req, args):
    raise Exception()

addRoute(GET, "/internal_error", handle_internal_error)

def handle(req, args):
    print(f"Hello, {args.name}! I am Python!")
    send("hello_js", args.name)
    return respond().finish()


addRoute(GET, "/hello_polyglot_py/(?<name>.+)", handle)

addStatelessChannel("hello_py", lambda name: print(f"Hello, {name}! I am Python!"))

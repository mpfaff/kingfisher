/*
 tests the Kingfisher APIs
 */

{
    print(`JObject: ${new JObject({
        'an_int': 32,
        'a_string': 'Hello',
        'a_float': 58.3,
        'a_bool': true,
        'an_array': [23],
        'an_object': {'foo': 'bar'}
    })}`);
}

{
    const arr = new JArray(10);
    for (let i = 0; i < arr.length; i++) {
        arr[i] = i * 2;
    }
    print(`JArray: ${arr}`);
}

{
    print(`JArray: ${new JArray(['foo', 'bar', 'baz'])}`);
}

{
    const list = new JList();
    for (let i = 0; i < 10; i++) {
        list.add(i * 2);
    }
    list[5] = 25;
    print(`JList: ${list}`);
}

const assert = (expected, actual) => {
    if (actual !== expected) throw new Error(`Expected ${expected}, found ${actual}`);
};

addRoute(GET, "/test/exec", async req => {
    const result = await childProcess.exec("/bin/echo", ["foo"], {encoding: Encoding.UTF_8});
    print(`got result: ${result}`);
    assert(0, result.status);
    assert("foo\n", result.stdout);
    assert("", result.stderr);
    return respond().content("exec worked!").finish();
});

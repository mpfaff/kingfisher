addRoute(GET, "/fetch_test", async req => {
    print("got request");
    const texts = await Promise.all(Array.from(Array(10), async () => {
        return await (await fetch("https://baconipsum.com/api/?type=meat-and-filler")).text();
    }));
    print("got responses");
    return respond().content(Array.from(texts).join("\n\n---\n\n")).finish();
});

addRoute(GET, "/fetch_html_test", async req => {
    return respond()
        .content(await (await fetch("https://mpfaff.github.io/kingfisher/")).text())
        .finish();
});

addRoute(GET, "/fetch_html_test_error", async req => {
    return respond()
        .content(await (await fetch("https://hopethisfailstoresolve.com/")).text())
        .finish();
});

addRoute(GET, "/fetch_post_text_body", async req => {
    return respond()
        .content(await (await fetch("https://httpbin.org/post", {method: POST, body: "Hello, World!"})).text())
        .finish();
});

addRoute(GET, "/fetch_post_binary_body", async req => {
    const buf = new Uint32Array([0xDEADBEEF]);
    return respond()
        .content(await (await fetch("https://httpbin.org/post", {method: POST, body: buf.buffer})).text())
        .finish();
});

package kingfisher.interop.js;

import kingfisher.constants.Method;
import kingfisher.scripting.EventLoop;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class JSApi {
	private final EventLoop eventLoop;
	public final JSNodeFS fs;

	public JSApi(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
		this.fs = new JSNodeFS(eventLoop);
	}

	public JPromise<JSFetchResponse> fetch(String url, Value options) {
		var builder = HttpRequest.newBuilder(URI.create(url));

		var method = options.hasMember("method") ? options.getMember("method").asString() : Method.GET;
		HttpRequest.BodyPublisher body;
		if (options.hasMember("body")) {
			body = Context.getCurrent()
					.getBindings("js")
					.getMember(JSImplementations.FETCH_BODY_TO_NATIVE_VALUE)
					.execute(options.getMember("body"))
					.as(HttpRequest.BodyPublisher.class);
		} else {
			body = HttpRequest.BodyPublishers.noBody();
		}

		if (options.hasMember("headers")) {
			var headersValue = options.getMember("headers");
			if (!headersValue.hasHashEntries()) {
				throw new IllegalArgumentException("Expected headers to be a map");
			}
			long size = headersValue.getHashSize() * 2;
			if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Too many headers");
			}
			var iter = headersValue.getHashEntriesIterator();
			while (iter.hasIteratorNextElement()) {
				var entry = iter.getIteratorNextElement();
				builder.header(entry.getArrayElement(0).asString(),
						entry.getArrayElement(1).asString());
			}
		}

		return JPromise.submitToExecutor(() -> {
			var res = eventLoop.engine.httpClient.send(builder.method(method, body).build(),
					HttpResponse.BodyHandlers.ofByteArray());
			return new JSFetchResponse(res, eventLoop);
		}, eventLoop.engine.httpClientExecutor, eventLoop);
	}

	public JPromise<JSFetchResponse> fetch(String url) {
		return fetch(url, Value.asValue(null));
	}
}

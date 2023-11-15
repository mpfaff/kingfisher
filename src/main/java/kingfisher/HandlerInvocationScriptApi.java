package kingfisher;

import dev.pfaff.log4truth.Logger;
import kingfisher.constants.Method;
import kingfisher.interop.JArray;
import kingfisher.interop.JObject;
import kingfisher.interop.js.JPromise;
import kingfisher.interop.Task;
import kingfisher.interop.js.JSFetchResponse;
import kingfisher.interop.js.JSImplementations;
import kingfisher.requests.ProxyRequest;
import kingfisher.requests.ScriptRequestHandler;
import kingfisher.requests.WrappedScriptRequestHandler;
import kingfisher.responses.BuiltResponse;
import kingfisher.scripting.Script;
import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Supplier;

import static dev.pfaff.log4truth.StandardTags.*;

public final class HandlerInvocationScriptApi extends ScriptApi {
	public final ScriptThread thread;
	private final Script script;
	private final Object state;
	private final int targetHandler;
	public WrappedScriptRequestHandler handler;

	public HandlerInvocationScriptApi(ScriptThread thread, Script script, Object state, int targetHandler) {
		super(thread.engine);
		this.thread = thread;
		this.script = script;
		this.state = state;
		this.targetHandler = targetHandler;
	}

	@Override
	protected Script script() {
		return script;
	}

	@Override
	public Object getState(Supplier<Object> initFunction) {
		return state;
	}

	@Override
	public void addRoute(String method, String path, Value handler) {
		if (nextHandlerId() == targetHandler) {
			// TODO: do we need to pin?
			handler.pin();
			this.handler = new WrappedScriptRequestHandler(thread, handler.as(ScriptRequestHandler.class));
		}
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
			var res = engine.httpClient.send(builder.method(method, body).build(),
					HttpResponse.BodyHandlers.ofByteArray());
			return new JSFetchResponse(res, thread);
		}, engine.httpClientExecutor, thread);
	}

	public JPromise fetch(String url) {
		return fetch(url, Value.asValue(null));
	}

	public Task<JArray> collectTasks(Value tasks) {
		if (!tasks.hasArrayElements()) throw new IllegalArgumentException("The provided argument is not an array");
		long size = tasks.getArraySize();
		if (size < Integer.MIN_VALUE || size > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"The length of the provided array of tasks is out of 32-bit integer range");
		}
		int sizeI = (int) size;
		return () -> {
			var results = new Object[sizeI];
			for (int i = 0; i < results.length; i++) {
				var task = tasks.getArrayElement(i);
				if (task == null || task.isNull()) {
					throw new NullPointerException("tasks array element " + i + "/" + sizeI + " is null");
				}
				results[i] = task.as(Task.class).join();
			}
			return new JArray(results);
		};
	}
}

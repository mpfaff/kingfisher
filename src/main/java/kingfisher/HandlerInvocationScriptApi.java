package kingfisher;

import dev.pfaff.log4truth.Logger;
import kingfisher.constants.Method;
import kingfisher.interop.JArray;
import kingfisher.interop.js.JPromise;
import kingfisher.interop.Task;
import kingfisher.interop.js.JSFetchResponse;
import kingfisher.interop.js.JSImplementations;
import kingfisher.requests.ScriptRequestHandler;
import kingfisher.responses.BuiltResponse;
import kingfisher.scripting.Script;
import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static dev.pfaff.log4truth.StandardTags.ERROR;

public final class HandlerInvocationScriptApi extends ScriptApi {
	public final ScriptThread thread;
	private final Script script;
	private final Object state;
	private final int targetHandler;
	public ScriptRequestHandler handler;

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
			this.handler = (request, arguments) -> {
				var result = handler.execute(request, arguments);
				if (result.canInvokeMember("then")) {
					var fut = new CompletableFuture<BuiltResponse>();
					Logger.log(() -> "registered then callback " + System.identityHashCode(fut));
					Consumer<BuiltResponse> resolveFn = value -> {
						Logger.log(() -> "resolved " + System.identityHashCode(fut));
						fut.complete(value);
					};
					Consumer<Value> rejectFn = e -> {
						Logger.log(() -> "rejected " + System.identityHashCode(fut));
						fut.completeExceptionally(new Exception("Promise rejected: " + e));
					};
					result.invokeMember("then", resolveFn, rejectFn);
//					result.invokeMember("then", resolveFn);
//					result.invokeMember("catch", rejectFn);
					while (!fut.isDone()) {
						if (thread.runMicrotask()) {
							Logger.log(() -> "ran microtask");
						} else {
							Thread.onSpinWait();
						}
					}
					Logger.log(() -> "completed");
					return fut.get();
				}
				return result.as(BuiltResponse.class);
			};
		}
	}

	public JPromise<JSFetchResponse> fetch(String url, Value options) {
		try {
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
					builder = builder.header(entry.getArrayElement(0).asString(),
							entry.getArrayElement(1).asString());
				}
			}

			return new JPromise<>(engine.httpClient.sendAsync(builder.method(method, body).build(),
					HttpResponse.BodyHandlers.ofByteArray()).thenApply(res -> new JSFetchResponse(res, thread)),
					thread);
		} catch (Throwable e) {
			Logger.log(() -> "Caught exception in fetch", e, List.of(ERROR));
			throw e;
//			return new JPromise<>(CompletableFuture.failedFuture(e), thread);
		}
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

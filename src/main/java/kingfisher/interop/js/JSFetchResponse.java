package kingfisher.interop.js;

import dev.pfaff.log4truth.Logger;
import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class JSFetchResponse {
	private final HttpResponse<byte[]> response;
	private final ScriptThread thread;

	public JSFetchResponse(HttpResponse<byte[]> response, ScriptThread thread) {
		this.response = response;
		this.thread = thread;
	}

	public Value getHeaders() {
		return Context.getCurrent()
				.getBindings("js")
				.getMember("Headers")
				.newInstance(response.headers()
						.map()
						.entrySet()
						.stream()
						.flatMap(entry -> entry.getValue().stream().map(value -> Map.entry(entry.getKey(), value)))
						.toArray());
	}

	public boolean getOk() {
		return getStatus() >= 200 && getStatus() <= 299;
	}

	public int getStatus() {
		return response.statusCode();
	}

	public String getStatusText() {
		return String.valueOf(getStatus());
	}

	public String getType() {
		return "basic";
	}

	public String getUrl() {
		return response.uri().toString();
	}

	// body accessors

	// TODO: implement the rest of them

//	public Task<String> text() {
//		var future = engine.httpClientExecutor.submit(() -> new String(response.body(), StandardCharsets.UTF_8));
//		return () -> {
//			try {
//				return future.get();
//			} catch (InterruptedException | ExecutionException e) {
//				throw throwUnchecked(e);
//			}
//		};
//	}

	public JPromise<String> text() {
		Logger.log(() -> "res.text");
		return JPromise.submitToExecutor(() -> new String(response.body(), StandardCharsets.UTF_8),
				thread.engine.httpClientExecutor,
				thread);
	}
}

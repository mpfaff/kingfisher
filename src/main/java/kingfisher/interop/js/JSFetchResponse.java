package kingfisher.interop.js;

import dev.pfaff.log4truth.Logger;
import kingfisher.scripting.EventLoop;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JSFetchResponse {
	private final HttpResponse<byte[]> response;
	private final EventLoop thread;

	public JSFetchResponse(HttpResponse<byte[]> response, EventLoop eventLoop) {
		this.response = response;
		this.thread = eventLoop;
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

	public JPromise<String> text(Encoding encoding) {
		return JPromise.submitToExecutor(() -> {
					return encoding.decode(response.body());
				},
				thread.engine.httpClientExecutor,
				thread);
	}

	public JPromise<String> text() {
		return text(Encoding.UTF_8);
	}
}

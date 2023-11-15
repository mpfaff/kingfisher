package kingfisher.requests;

import kingfisher.interop.js.JPromise;
import kingfisher.scripting.ScriptThread;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.LockSupport;

/**
 * A request from a client to the HTTP server.
 */
public final class HttpServerRequest {
	private final ScriptThread thread;
	private final Request request;

	public HttpServerRequest(ScriptThread thread, Request request) {
		this.thread = thread;
		this.request = request;
	}

	public String getMethod() {
		return request.getMethod();
	}

	public String header(String name) {
		return request.getHeaders().get(name);
	}

	public JPromise<String> bodyString() {
		return JPromise.submitToExecutor(() -> {
			var sb = new StringBuilder();
			while (true) {
				Content.Chunk chunk;
				while ((chunk = request.read()) == null) {
					var thread = Thread.currentThread();
					request.demand(() -> LockSupport.unpark(thread));
					LockSupport.park();
				}
				sb.append(StandardCharsets.UTF_8.decode(chunk.getByteBuffer()));
				if (chunk.isLast()) break;
			}
			return sb.toString();
		}, thread.engine.ioExecutor, thread.eventLoop);
	}

	public JPromise<byte[]> bodyBytes() {
		return JPromise.submitToExecutor(() -> {
			ByteBuffer buf;
			try {
				buf = Content.Source.asByteBuffer(request);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			var bytes = new byte[buf.remaining()];
			buf.get(bytes);
			return bytes;
		}, thread.engine.ioExecutor, thread.eventLoop);
	}
}

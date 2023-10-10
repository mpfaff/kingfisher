package westmount.codingclub.requests;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public final class ProxyRequest implements ProxyObject {
	private static final Set<String> FIELD_NAMES = Set.of("method", "header", "argument", "bodyString", "bodyBytes");
	private final Request request;

	public ProxyRequest(Request request) {
		this.request = request;
	}

	@Override
	public Object getMember(String key) {
		return switch (key) {
			case "method" -> request.getMethod();
			case "header" -> (Function<String, String>) request.getHeaders()::get;
			case "bodyString" -> {
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
				yield sb.toString();
			}
			case "bodyBytes" -> {
				ByteBuffer buf;
				try {
					buf = Content.Source.asByteBuffer(request);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				var bytes = new byte[buf.remaining()];
				buf.get(bytes);
				yield bytes;
			}
			default -> throw new IllegalArgumentException("No such field " + key + " on Request");
		};
	}

	@Override
	public Object getMemberKeys() {
		return FIELD_NAMES;
	}

	@Override
	public boolean hasMember(String key) {
		return FIELD_NAMES.contains(key);
	}

	@Override
	public void putMember(String key, Value value) {
		throw new UnsupportedOperationException();
	}
}

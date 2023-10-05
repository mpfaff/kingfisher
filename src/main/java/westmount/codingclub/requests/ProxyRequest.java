package westmount.codingclub.requests;

import name.martingeisse.grumpyrest.request.Request;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

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
			case "header" -> (Function<String, String>) request::getHeader;
			case "argument" -> (IntFunction<String>) i -> request.getPathArguments().get(i).getText();
			case "bodyString" -> request.parseBody(String.class);
			case "bodyBytes" -> request.parseBody(byte[].class);
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

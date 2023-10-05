package westmount.codingclub;

import name.martingeisse.grumpyrest.request.HttpMethod;
import org.graalvm.polyglot.Value;
import westmount.codingclub.responses.ResponseBuilder;
import westmount.codingclub.util.ConcurrentObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Provides a strongly typed api that will be provided to scripts. GraalVM Polyglot automatically makes available all
 * public members.
 */
public abstract class ScriptApi {
	protected final ScriptEngine engine;

	protected ScriptApi(ScriptEngine engine) {
		this.engine = engine;
	}

	public abstract void startNewScript(String name);

	/**
	 * Scripts are reevaluated for each request, so they need a way to share state.
	 * <p>
	 * For each script that calls this method, {@code initFunction} will be run once during initialization, and then
	 * the value will be stored and returned when the script is reevaluated on each request.
	 */
	public abstract Object getState(Supplier<Object> initFunction);

	public abstract void addRoute(HttpMethod method, String path, Value handler);

	public final ResponseBuilder respond() {
		return new ResponseBuilder();
	}

	public final ResponseBuilder respond(int status) {
		return respond().status(status);
	}

	public final ConcurrentMap<Object, Object> ConcMap(Object obj) {
		var map = new ConcurrentHashMap<>();
		switch (obj) {
			case Value pValue -> {
				if (!pValue.hasMembers()) throw new IllegalArgumentException("this value does not have members");
				pValue.getMemberKeys().forEach(key -> map.put(key, pValue.getMember(key)));
			}
			case Map<?, ?> mapIn -> map.putAll(mapIn);
			default -> throw new IllegalArgumentException("Cannot make a concurrent object from provided value: " + obj);
		}
		return map;
	}

	public final ConcurrentObject ConcObject(Object obj) {
		return new ConcurrentObject(obj);
	}

	public final String render(String templateName, Map<String, Object> context) throws IOException {
		var wtr = new StringWriter();
		engine.pebble.getTemplate(templateName).evaluate(wtr, context);
		return wtr.toString();
	}
}

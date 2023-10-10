package westmount.codingclub;

import org.graalvm.polyglot.Value;
import westmount.codingclub.responses.ResponseBuilder;
import westmount.codingclub.util.JObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
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
	private int nextHandlerId;

	protected ScriptApi(ScriptEngine engine) {
		this.engine = engine;
	}

	protected int nextHandlerId() {
		return nextHandlerId++;
	}

	/**
	 * Scripts are reevaluated for each request, so they need a way to share state.
	 * <p>
	 * For each script that calls this method, {@code initFunction} will be run once during initialization, and then
	 * the value will be stored and returned when the script is reevaluated on each request.
	 */
	public abstract Object getState(Supplier<Object> initFunction);

	public abstract void addRoute(String method, String path, Value handler);

	public final ResponseBuilder respond() {
		return new ResponseBuilder();
	}

	public final ResponseBuilder respond(int status) {
		return respond().status(status);
	}

	private Map<Object, Object> mapFromObject(Object obj, Supplier<Map<Object, Object>> mapConstructor) {
		var map = mapConstructor.get();
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

	/**
	 * Non-thread-safe map type. With proper synchronization, this may be used from multiple threads.
	 */
	public final Map<Object, Object> JMap(Object obj) {
		return mapFromObject(obj, HashMap::new);
	}

	/**
	 * Thread-safe map type.
	 */
	public final Map<Object, Object> ConcurrentMap(Object obj) {
		return mapFromObject(obj, ConcurrentHashMap::new);
	}

	/**
	 * @see JObject
	 */
	public final JObject JObject(Object obj) {
		return new JObject(obj);
	}

	public final String render(String templateName, Map<String, Object> context) throws IOException {
		var wtr = new StringWriter();
		engine.pebble.getTemplate(templateName).evaluate(wtr, context);
		return wtr.toString();
	}
}

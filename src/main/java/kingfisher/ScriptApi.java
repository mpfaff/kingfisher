package kingfisher;

import dev.pfaff.log4truth.Logger;
import kingfisher.responses.ResponseBuilder;
import kingfisher.scripting.Script;
import kingfisher.templating.ExecuteFilter;
import kingfisher.interop.JObject;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

	protected void resetHandlerId() {
		this.nextHandlerId = 0;
	}

	protected int nextHandlerId() {
		return nextHandlerId++;
	}

	protected abstract Script script();

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
		var tmpl = engine.pebble.getTemplate(templateName);
		try {
			tmpl.evaluate(wtr, context);
		} finally {
			var value = ExecuteFilter.CONTEXT.get();
			if (value != null) {
				try {
					// docs for ctx.close() say that ctx.leave() is called automatically when it is closed, but that does not
					// seem to happen.
					value.leave();
				} finally {
					value.close(true);
				}
			}
		}
		return wtr.toString();
	}

	public void print(Object o) {
		Logger.log("Script[" + script() + "]", () -> Objects.toString(o));
	}

	public String getMetaQualifiedName(Value value) {
		return value.getMetaObject().getMetaQualifiedName();
	}
}

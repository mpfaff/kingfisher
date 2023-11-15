package kingfisher.scripting;

import dev.pfaff.log4truth.Logger;
import kingfisher.ScriptEngine;
import kingfisher.constants.ContentType;
import kingfisher.constants.Header;
import kingfisher.constants.Method;
import kingfisher.interop.Exports;
import kingfisher.interop.JObject;
import kingfisher.interop.ProxyConstantTable;
import kingfisher.interop.js.JSApi;
import kingfisher.requests.ScriptRouteHandler;
import kingfisher.templating.ExecuteFilter;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyInstantiable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A script "thread". Each and every invocation of a script (for example, a route handler) gets its own thread.
 */
public abstract class ScriptThread {
	/**
	 * The global script engine.
	 */
	public final ScriptEngine engine;

	/**
	 * The thread's event loop.
	 */
	public final EventLoop eventLoop;

	private int nextHandlerId;

	public ScriptThread(ScriptEngine engine) {
		this.engine = engine;
		this.eventLoop = new EventLoop(engine);
	}

	public void exportApi(String lang, Value scope) {
		// these are commonly used enough to warrant them being unscoped...
		for (var method : ApiConstants.COMMON_METHODS) {
			scope.putMember(method, method);
		}

		scope.putMember("JObject", JObject.class);
		// Non-thread-safe map type. With proper synchronization, this may be used from multiple threads.
		scope.putMember("JMap", (ProxyInstantiable) arguments -> ApiConstants.mapFromObject(arguments[0], HashMap::new));
		// Thread-safe map type.
		scope.putMember("ConcurrentMap", (ProxyInstantiable) arguments -> ApiConstants.mapFromObject(arguments[0], ConcurrentHashMap::new));

		Exports.objectMembers(new Api()).export(scope);
		switch (lang) {
			case "js" -> Exports.objectMembers(new JSApi(eventLoop)).export(scope);
			default -> {}
		}
	}

	protected final void resetHandlerId() {
		this.nextHandlerId = 0;
	}

	protected final int nextHandlerId() {
		return nextHandlerId++;
	}

	public abstract Script script();

	private static final class ApiConstants {
		public static final List<String> COMMON_METHODS = List.of(Method.GET,
				Method.POST,
				Method.PUT,
				Method.POST,
				Method.DELETE,
				Method.HEAD);

		public static Map<Object, Object> mapFromObject(Object obj, Supplier<Map<Object, Object>> mapConstructor) {
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
	}

	public final class Api {
		private static final ProxyConstantTable CONTENT_TYPE = new ProxyConstantTable(ContentType.class);
		private static final ProxyConstantTable HEADER = new ProxyConstantTable(Header.class);
		private static final ProxyConstantTable METHOD = new ProxyConstantTable(Method.class);

		private Api() {}

		public final ProxyConstantTable ContentType = CONTENT_TYPE;
		public final ProxyConstantTable Header = HEADER;
		public final ProxyConstantTable Method = METHOD;

		/**
		 * Scripts are reevaluated for each request, so they need a way to share state.
		 * <p>
		 * For each script that calls this method, {@code initFunction} will be run once during initialization, and then
		 * the value will be stored and returned when the script is reevaluated on each request.
		 */
		public Object getState(Supplier<Object> initFunction) {
			return script().getState(initFunction);
		}

		public String render(String templateName, Map<String, Object> context) throws IOException {
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

	/**
	 * The API available to the script.
	 */
	public abstract class BaseApi {
		protected BaseApi() {}

		/**
		 * Registers a handler for the specified route.
		 * @param method the HTTP method to match. See {@link Method}.
		 * @param path the url path pattern to match. This is a {@link java.util.regex.Pattern regular expression}.
		 * @param handler the handler for the route.
		 */
		public abstract void addRoute(String method, String path, ScriptRouteHandler handler);
	}
}

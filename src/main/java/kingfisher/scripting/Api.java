package kingfisher.scripting;

import dev.pfaff.log4truth.Logger;
import kingfisher.constants.ContentType;
import kingfisher.constants.Header;
import kingfisher.constants.Method;
import kingfisher.interop.JArray;
import kingfisher.templating.ExecuteFilter;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyInstantiable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Core API available to scripts regardless of context.
 */
public final class Api {
	private final ScriptThread thread;

	Api(ScriptThread thread) {
		this.thread = thread;
	}

	// constants

	/**
	 * Refer to {@link ContentType}.
	 */
	public final Class<kingfisher.constants.ContentType> ContentType = kingfisher.constants.ContentType.class;

	/**
	 * Refer to {@link Header}.
	 */
	public final Class<kingfisher.constants.Header> Header = kingfisher.constants.Header.class;

	/**
	 * Refer to {@link Method}.
	 */
	public final Class<kingfisher.constants.Method> Method = kingfisher.constants.Method.class;

	// these are commonly used enough to warrant them being unscoped...

	/**
	 * HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/GET">{@code GET}</a> method
	 */
	public final String GET = kingfisher.constants.Method.GET;

	/**
	 * HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST">{@code POST}</a> method
	 */
	public final String POST = kingfisher.constants.Method.POST;

	/**
	 * HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/PUT">{@code PUT}</a> method
	 */
	public final String PUT = kingfisher.constants.Method.PUT;

	/**
	 * HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/PATCH">{@code PATCH}</a> method
	 */
	public final String PATCH = kingfisher.constants.Method.PATCH;

	/**
	 * HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/DELETE">{@code DELETE}</a> method
	 */
	public final String DELETE = kingfisher.constants.Method.DELETE;

	/**
	 * HTTP <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/HEAD">{@code HEAD}</a> method
	 */
	public final String HEAD = kingfisher.constants.Method.HEAD;

	// data structures

	/**
	 * Refer to {@link kingfisher.interop.JObject}.
	 */
	public final Class<?> JObject = kingfisher.interop.JObject.class;

	/**
	 * Thread-safe, fixed-size array. Subject to the
	 * <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4">Java Memory Model</a>.
	 */
	public final Class<?> JArray = kingfisher.interop.JArray.class;

	/**
	 * @see ArrayList
	 */
	public final Class<?> JList = ArrayList.class;

	/**
	 * Non-thread-safe map type. With proper synchronization, this may be used from multiple threads.
	 *
	 * @see HashMap
	 */
	public final Class<HashMap> JMap = HashMap.class;

	/**
	 * Thread-safe map type.
	 *
	 * @see ConcurrentHashMap
	 */
	public final Class<ConcurrentHashMap> ConcurrentMap = ConcurrentHashMap.class;

	public static void registerTypes(HostAccess.Builder builder) {
		builder.targetTypeMapping(Value.class, Object[].class, v -> v.hasArrayElements(), v -> {
			long size = v.getArraySize();
			if (size < 0 || size > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Array value length out of bounds: " + size);
			}
			int sizeI = (int) size;
			var array = new Object[sizeI];
			for (int i = 0; i < sizeI; i++) {
				array[i] = v.getArrayElement(i);
			}
			return array;
		});
//		builder.targetTypeMapping(Value.class, Map.class, v -> v.hasHashEntries() || v.hasMembers(), v -> {
//			var map = new HashMap<>();
//			if (v.hasHashEntries()) {
//				var iter = v.getHashEntriesIterator();
//				while (iter.hasIteratorNextElement()) {
//					var entry = iter.getIteratorNextElement();
//					map.put(entry.getArrayElement(0), entry.getArrayElement(1));
//				}
//			} else if (v.hasMembers()) {
//				v.getMemberKeys().forEach(k -> map.put(k, v.getMember(k)));
//			}
//			return map;
//		});
	}

	// methods

	/**
	 * Scripts are reevaluated for each request, so they need a way to share state.
	 * <p>
	 * For each script that calls this method, {@code initFunction} will be run once during initialization, and
	 * then
	 * the value will be stored and returned when the script is reevaluated on each request.
	 */
	public Object getState(Supplier<Object> initFunction) {
		return thread.script.getState(initFunction);
	}

	/**
	 * Renders the specified template with the provided context.
	 *
	 * @param templateName the name of the template, including any path elements and file extension, relative to
	 *                     the configured template folder.
	 * @param context      key-value pairs that will be available to the template as variables
	 * @return the rendered template
	 */
	public String render(String templateName, Map<String, Object> context) throws IOException {
		var wtr = new StringWriter();
		var tmpl = thread.engine.pebble.getTemplate(templateName);
		try {
			tmpl.evaluate(wtr, context);
		} finally {
			var value = ExecuteFilter.CONTEXT.get();
			if (value != null) {
				//noinspection TryFinallyCanBeTryWithResources
				try {
					// docs for ctx.close() say that ctx.leave() is called automatically when it is closed, but
					// that does not
					// seem to happen.
					value.leave();
				} finally {
					value.close(true);
				}
			}
		}
		return wtr.toString();
	}

	/**
	 * Logs the provided object. The log record will be specified to originate in the thread's script.
	 */
	public void print(Object o) {
		Logger.log("Script[" + thread.script + "]", () -> Objects.toString(o));
	}

	public String getMetaQualifiedName(Value value) {
		return value.getMetaObject().getMetaQualifiedName();
	}
}

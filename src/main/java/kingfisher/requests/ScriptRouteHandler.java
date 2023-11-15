package kingfisher.requests;

import kingfisher.interop.JObject;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

/**
 * A route handler implemented by a script.
 */
@FunctionalInterface
public interface ScriptRouteHandler {
	Value handle(HttpServerRequest request, JObject arguments) throws Throwable;
}

package kingfisher.requests;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.regex.Pattern;

/**
 * Handles a request. The return value determines whether to continue invoking handlers for the request. If this
 * handler knows how to handle the request, it should usually return {@code false}.
 */
@FunctionalInterface
public interface RequestHandler {
	boolean handle(Request request, Response response, Callback callback) throws Exception;

	static RequestHandler regexRoute(String method, Pattern pattern, RouteHandler handler) {
		if (pattern.namedGroups().isEmpty()) {
			return new RegexRouteHandler(method, pattern, handler);
		} else {
			return new CapturingRegexRouteHandler(method, pattern, handler);
		}
	}
}

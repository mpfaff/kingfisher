package kingfisher.requests;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A request handler that matches a route using the {@link #method} and url {@link #pattern}, extracts the arguments
 * from the match groups, and invokes the route {@link #handler}.
 */
public record RegexRouteHandler(String method, Pattern pattern, RouteHandler handler) implements RequestHandler {
	public RegexRouteHandler {
		if (!pattern.namedGroups().isEmpty()) {
			throw new IllegalArgumentException("Use CapturingRegexRouteHandler instead");
		}
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		if (!request.getMethod().equals(method)) return false;
		var matcher = pattern.matcher(request.getHttpURI().getCanonicalPath());
		if (!matcher.matches()) return false;
		handler.handle(request,
				Map.of(),
				response,
				callback);
		return true;
	}
}

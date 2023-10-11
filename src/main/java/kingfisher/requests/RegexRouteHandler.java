package kingfisher.requests;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record RegexRouteHandler(String method, Pattern pattern, RequestHandler handler) implements MatchingRequestHandler {
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		var matcher = pattern.matcher(request.getHttpURI().getCanonicalPath());
		if (!matcher.matches()) return false;
		handler.handle(request,
				matcher.namedGroups().entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
						entry -> matcher.group(entry.getValue()))),
				response,
				callback);
		return true;
	}
}

package kingfisher.requests;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.Map;

@FunctionalInterface
public interface RouteHandler {
	void handle(Request request, Map<String, String> arguments, Response response, Callback callback) throws Exception;
}

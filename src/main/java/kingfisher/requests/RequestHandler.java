package kingfisher.requests;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

@FunctionalInterface
public interface MatchingRequestHandler {
	boolean handle(Request request, Response response, Callback callback) throws Exception;
}

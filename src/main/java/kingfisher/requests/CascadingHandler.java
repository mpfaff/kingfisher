package kingfisher.requests;

import kingfisher.Main;
import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import kingfisher.constants.Status;

import java.util.List;

public final class CascadingHandler extends Handler.Abstract {
	private final List<MatchingRequestHandler> handlers;

	public CascadingHandler(List<MatchingRequestHandler> handlers) {
		this.handlers = List.copyOf(handlers);
		Main.WEB_LOGGER.info("handlers: " + this.handlers);
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		Main.WEB_LOGGER.info(request.getMethod() + " " + request.getHttpURI().getCanonicalPath());
		for (MatchingRequestHandler handler : handlers) {
			if (handler.handle(request, response, callback)) return true;
		}
		throw new HttpException.RuntimeException(Status.NOT_FOUND);
	}
}

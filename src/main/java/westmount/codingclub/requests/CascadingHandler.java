package westmount.codingclub.requests;

import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import westmount.codingclub.constants.Status;

import java.util.List;

import static westmount.codingclub.Main.WEB_LOGGER;

public final class CascadingHandler extends Handler.Abstract {
	private final List<MatchingRequestHandler> handlers;

	public CascadingHandler(List<MatchingRequestHandler> handlers) {
		this.handlers = List.copyOf(handlers);
		WEB_LOGGER.info("handlers: " + this.handlers);
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		WEB_LOGGER.info(request.getMethod() + " " + request.getHttpURI().getCanonicalPath());
		for (MatchingRequestHandler handler : handlers) {
			if (handler.handle(request, response, callback)) return true;
		}
		throw new HttpException.RuntimeException(Status.NOT_FOUND);
	}
}

package kingfisher;

import dev.pfaff.log4truth.LogSink;
import dev.pfaff.log4truth.NamedLogger;
import kingfisher.constants.Status;
import kingfisher.requests.CallSiteHandler;
import kingfisher.responses.BuiltResponse;
import kingfisher.scripting.ScriptLoader;
import kingfisher.util.Errors;
import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;

import java.util.List;

import static dev.pfaff.log4truth.StandardTags.DEBUG;
import static dev.pfaff.log4truth.StandardTags.ERROR;
import static kingfisher.Config.BIND_PORT;
import static org.eclipse.jetty.server.handler.ErrorHandler.ERROR_EXCEPTION;

public final class Main {
	public static final NamedLogger WEB_LOGGER = new NamedLogger("Web Engine");
	public static final NamedLogger SCRIPT_LOGGER = new NamedLogger("Script Engine");

	public static void main(String[] args) {
		LogSink.addFilterToGlobal((source, tags, thread) -> {
			return !source.startsWith("org.eclipse.jetty.") || !tags.contains(DEBUG);
		});
		LogSink.addFilterToGlobal((source, tags, thread) -> {
			return !source.startsWith("io.pebbletemplates.pebble.") || !tags.contains("TRACE");
		});

		ScriptEngine engine = new ScriptEngine();

		new ScriptLoader(engine).launch();

		try {
			Server server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(BIND_PORT);
			server.setDynamic(false);
			server.setConnectors(new Connector[]{connector});
			var handler = new CallSiteHandler(engine.handler);
			server.setHandler(new Handler.Abstract() {
				@Override
				public boolean handle(Request request, Response response, Callback callback) {
					try {
						return handler.handle(request, response, callback);
					} catch (Throwable e) {
						return handleError(engine, response, callback, e);
					}
				}
			});
			server.setErrorHandler((request, response, callback) -> {
				Throwable cause = (Throwable) request.getAttribute(ERROR_EXCEPTION);
				return handleError(engine, response, callback, cause);
			});
			server.start();
			server.join();
		} catch (Throwable e) {
			WEB_LOGGER.log(() -> "Server crashed", e, List.of(ERROR));
		}
	}

	public static boolean handleError(ScriptEngine engine, Response response, Callback callback, Throwable cause) {
		String message;
		var status = Status.INTERNAL_SERVER_ERROR;
		cause = Errors.unwrapError(cause);
		if (cause instanceof HttpException httpException) {
			status = httpException.getCode();
			message = httpException.getReason() != null ? httpException.getReason() : Integer.toString(status);
		} else {
			message = "Internal server error";
		}
		WEB_LOGGER.log(() -> "Caught exception while handling request: " + message, cause, List.of(ERROR));
		BuiltResponse.error(engine, status, message).send(response, callback);
		return true;
	}
}

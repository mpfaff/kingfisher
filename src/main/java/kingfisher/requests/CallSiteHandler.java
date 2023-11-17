package kingfisher.requests;

import kingfisher.constants.Status;
import kingfisher.responses.ErrorResponseBuilder;
import kingfisher.scripting.ScriptEngine;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import static dev.pfaff.log4truth.StandardTags.DEBUG;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static kingfisher.Main.WEB_LOGGER;

public final class CallSiteHandler extends Handler.Abstract {
	public static final MethodType HANDLER_TYPE = methodType(boolean.class,
			Request.class,
			Response.class,
			Callback.class);

	private final CallSite callSite;

	public CallSiteHandler(CallSite callSite) {
		if (!callSite.type().equals(HANDLER_TYPE)) {
			throw new IllegalArgumentException("Expected a call-site of type " + HANDLER_TYPE + ", found " + callSite.type());
		}
		this.callSite = callSite;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		try {
			return (boolean) callSite.getTarget().invokeExact(request, response, callback);
		} catch (Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	// utilities (handler chaining, etc.)

	private static final MethodHandle MH_handle;
	private static final MethodHandle MH_handleLogging;
	private static final MethodHandle MH_handleFallback;

	static {
		try {
			MH_handleLogging = MethodHandles.lookup().findStatic(CallSiteHandler.class,
					"handleLogging",
					CallSiteHandler.HANDLER_TYPE);
			MH_handleFallback = MethodHandles.lookup().findStatic(CallSiteHandler.class,
					"handleFallback",
					CallSiteHandler.HANDLER_TYPE.insertParameterTypes(0, ScriptEngine.class));
			MH_handle = MethodHandles.lookup().findVirtual(RequestHandler.class,
					"handle",
					CallSiteHandler.HANDLER_TYPE);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Chains the provided handlers, in order, to produce a single short-circuiting handler.
	 *
	 * @param handlerType the type of the handler
	 * @param handlers    the handlers to chain
	 */
	private static MethodHandle chainHandlers(MethodType handlerType, List<MethodHandle> handlers) {
		var MH_true = dropArguments(constant(boolean.class, true), 0, handlerType.parameterList());
		var handler = empty(handlerType);
		for (int i = handlers.size(); i-- > 0; ) {
			var h = handlers.get(i);
			if (!h.type().equals(handlerType)) {
				throw new IllegalArgumentException("Expected a handler of type " + handlerType + ", found " + h);
			}
			handler = guardWithTest(h, MH_true, handler);
		}
		return handler;
	}

	/**
	 * Chains the provided handlers, in order, to produce a single short-circuiting handler.
	 *
	 * @param handlers the handlers to chain
	 */
	public static MethodHandle chainHandlers(ScriptEngine engine, List<RequestHandler> handlers) {
		var a = new ArrayList<MethodHandle>(handlers.size() + 1);
		a.add(MH_handleLogging);
		for (var handler : handlers) a.add(MH_handle.bindTo(handler));
		a.add(MH_handleFallback.bindTo(engine));
		return chainHandlers(CallSiteHandler.HANDLER_TYPE, a);
	}

	private static boolean handleLogging(Request request, Response response, Callback callback) throws Exception {
		WEB_LOGGER.log(() -> request.getMethod() + " " + request.getHttpURI().getCanonicalPath(),
				List.of("REQUEST"));
		return false;
	}

	private static boolean handleFallback(ScriptEngine engine, Request request, Response response, Callback callback) throws Exception {
		new ErrorResponseBuilder(engine, Status.NOT_FOUND, "404 Not Found")
				.diagnostic("There may be a route for the path, but not for the attempted method")
				.finish()
				.send(response, callback);
		return true;
	}
}

package kingfisher.requests;

import dev.pfaff.log4truth.Logger;
import kingfisher.interop.JObject;
import kingfisher.interop.js.PromiseRejectionException;
import kingfisher.responses.BuiltResponse;
import kingfisher.scripting.EventLoop;
import kingfisher.util.Errors;
import org.graalvm.polyglot.PolyglotException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static dev.pfaff.log4truth.StandardTags.DEBUG;
import static dev.pfaff.log4truth.StandardTags.ERROR;
import static kingfisher.Main.SCRIPT_LOGGER;

public final class WrappedScriptRequestHandler {
	private final EventLoop thread;
	private final ScriptRouteHandler handler;

	public WrappedScriptRequestHandler(EventLoop eventLoop, ScriptRouteHandler handler) {
		this.thread = eventLoop;
		this.handler = handler;
	}

	public BuiltResponse handle(HttpServerRequest request, JObject arguments) throws Exception {
		try {
			try {
				var result = handler.handle(request, arguments);
				if (result.canInvokeMember("then")) {
					var fut = new CompletableFuture<BuiltResponse>();
					Logger.log(() -> "registered then callback " + System.identityHashCode(fut),
							List.of(DEBUG));
					Consumer<BuiltResponse> resolveFn = value -> {
						Logger.log(() -> "resolved " + System.identityHashCode(fut), List.of(DEBUG));
						fut.complete(value);
					};
					Consumer<Object> rejectFn = e -> {
						Logger.log(() -> "rejected " + System.identityHashCode(fut), List.of(DEBUG));
						fut.completeExceptionally(new PromiseRejectionException(e));
					};
					result.invokeMember("then", resolveFn, rejectFn);
					//					result.invokeMember("then", resolveFn);
					//					result.invokeMember("catch", rejectFn);
					while (!fut.isDone()) {
						if (thread.runMicrotask()) {
							Logger.log(() -> "ran microtask", List.of(DEBUG));
						} else {
							Thread.onSpinWait();
						}
					}
					Logger.log(() -> "completed", List.of(DEBUG));
					return fut.get();
				}
				return result.as(BuiltResponse.class);
			} catch (Throwable e) {
				throw Errors.wrapError(Errors.unwrapError(e));
			}
		} catch (PolyglotException e) {
			SCRIPT_LOGGER.log(() -> "Caught synchronous exception from script", e, List.of(ERROR));
			return BuiltResponse.error(thread.engine, 500, "Script error");
		}
	}
}

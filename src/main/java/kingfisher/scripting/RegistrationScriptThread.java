package kingfisher.scripting;

import kingfisher.channel.ScriptChannelHandler;
import kingfisher.interop.Exports;
import kingfisher.interop.JObject;
import kingfisher.interop.ValueUtil;
import kingfisher.requests.*;
import kingfisher.responses.BuiltResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * The registration phase loads each script and evaluates them so that they can register their routes with the engine.
 */
public final class RegistrationScriptThread extends ScriptThread {
	public final Registrar registrar;

	public RegistrationScriptThread(ScriptEngine engine, Registrar registrar, Script script) {
		super(engine, script);
		this.registrar = registrar;
		lateInit();
	}

	@Override
	public void exportApi(String lang, Value scope) {
		super.exportApi(lang, scope);
		Exports.objectMembers(new RegistrationApi()).export(scope);
	}

	/**
	 * Refer to {@link kingfisher.scripting.RegistrationApi}.
	 */
	public final class RegistrationApi extends kingfisher.scripting.RegistrationApi {
		private RegistrationApi() {}

		@Override
		public void addRoute(String method, String path, ScriptRouteHandler handler) {
			var engine = RegistrationScriptThread.this.engine;
			var script = RegistrationScriptThread.this.script;
			var registrar = RegistrationScriptThread.this.registrar;

			registrar.checkState();

			int handlerId = nextHandlerId();
			var requestHandler = new RegexRouteHandler(method,
					Pattern.compile(path),
//					engine.patternCache.get(path),
					new WrappedScriptRouteHandler(engine, new HandlerRef(script, handlerId)));
			registrar.requestHandlers.add(requestHandler);
		}

		@Override
		public void addChannel(String name, ScriptChannelHandler handler) {
			var script = RegistrationScriptThread.this.script;
			var registrar = RegistrationScriptThread.this.registrar;

			registrar.checkState();

			int handlerId = nextHandlerId();
			registrar.channelHandlers.put(name, new HandlerRef(script, handlerId));
		}

		private static final class WrappedScriptRouteHandler implements RouteHandler {
			private final ScriptEngine engine;
			private final HandlerRef handler;

			public WrappedScriptRouteHandler(ScriptEngine engine, HandlerRef handler) {
				this.engine = engine;
				this.handler = handler;
			}

			@Override
			public String toString() {
				return handler.toString();
			}

			@Override
			public void handle(Request request, Map<String, String> arguments, Response response, Callback callback) throws Exception {
				engine.runWithScript(() -> new RequestScriptThread(engine, handler), "Script route handler", thread -> {
					BuiltResponse res;
					try {
						//noinspection unchecked,rawtypes
						var value = thread.handler.handle(new HttpServerRequest(thread, request),
								JObject.wrap((Map<String, Object>) (Map) arguments));
						var result = ValueUtil.resolveMaybePromise(value, thread.eventLoop);
						// The cast is unchecked, and intentionally so. If it fails, it will throw a ClassCastException for us.
						res = result instanceof Value v ? v.as(BuiltResponse.class) : (BuiltResponse) result;
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}

					if (res == null) {
						throw new NullPointerException("Script returned null response object");
					}

					return res;
				}).get().send(response, callback);
			}
		}
	}
}

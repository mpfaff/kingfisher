package kingfisher.scripting;

import kingfisher.interop.Exports;
import kingfisher.interop.JObject;
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
					new WrappedScriptRouteHandler(engine, script, handlerId));
			registrar.requestHandlers.add(requestHandler);
		}

		private static final class WrappedScriptRouteHandler implements RouteHandler {
			private final ScriptEngine engine;
			private final Script script;
			private final int handlerId;

			public WrappedScriptRouteHandler(ScriptEngine engine, Script script, int handlerId) {
				this.engine = engine;
				this.script = script;
				this.handlerId = handlerId;
			}

			@Override
			public String toString() {
				return "Handler[" + "script=" + script +
						", " + "handlerId=" + handlerId +
						']';
			}

			@Override
			public void handle(Request request, Map<String, String> arguments, Response response, Callback callback) throws Exception {
				engine.runWithScript(() -> new RequestScriptThread(engine, script, handlerId), "Script route handler", thread -> {
					BuiltResponse res;
					try {
						//noinspection unchecked,rawtypes
						res = thread.handler.handle(new HttpServerRequest(thread, request),
								JObject.wrap((Map<String, Object>) (Map) arguments));
					} catch (Exception e) {
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

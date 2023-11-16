package kingfisher.requests;

import kingfisher.scripting.ScriptEngine;
import kingfisher.interop.Exports;
import kingfisher.responses.ResponseBuilder;
import kingfisher.scripting.Script;
import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.Value;

public final class RequestScriptThread extends ScriptThread {
	private final Script script;
	private final int targetHandler;
	public WrappedScriptRequestHandler handler;

	public RequestScriptThread(ScriptEngine engine, Script script, int targetHandler) {
		super(engine);
		this.script = script;
		this.targetHandler = targetHandler;
	}

	@Override
	public void exportApi(String lang, Value scope) {
		super.exportApi(lang, scope);
		Exports.objectMembers(new RegistrationApi()).export(scope);
		Exports.objectMembers(new Api()).export(scope);
	}

	@Override
	public Script script() {
		return script;
	}

	/**
	 * Refer to {@link kingfisher.scripting.RegistrationApi}.
	 */
	public final class RegistrationApi extends kingfisher.scripting.RegistrationApi {
		private RegistrationApi() {}

		@Override
		public void addRoute(String method, String path, ScriptRouteHandler handler) {
			if (RequestScriptThread.this.handler != null) return;
			if (nextHandlerId() == targetHandler) {
				RequestScriptThread.this.handler = new WrappedScriptRequestHandler(RequestScriptThread.this.eventLoop, handler);
			}
		}
	}

	/**
	 * The API available to each script when it is executed to handle a request.
	 */
	public static final class Api {
		/**
		 * Returns a new {@link ResponseBuilder} with the default status code of {@value kingfisher.constants.Status#OK}.
		 */
		public ResponseBuilder respond() {
			return new ResponseBuilder();
		}

		/**
		 * Returns a new {@link ResponseBuilder} with the specified status code.
		 */
		public ResponseBuilder respond(int status) {
			return new ResponseBuilder(status);
		}
	}
}

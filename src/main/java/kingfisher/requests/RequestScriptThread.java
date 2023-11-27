package kingfisher.requests;

import kingfisher.channel.ScriptStatelessChannelHandler;
import kingfisher.channel.StatelessChannelScriptThread;
import kingfisher.interop.Exports;
import kingfisher.responses.ResponseBuilder;
import kingfisher.scripting.*;
import org.graalvm.polyglot.Value;

public final class RequestScriptThread extends ScriptThread {
	private final int targetHandler;
	public ScriptRouteHandler handler;

	public RequestScriptThread(ScriptEngine engine, HandlerRef handler) {
		super(engine, handler.script());
		this.targetHandler = handler.handlerId();
	}

	@Override
	public void exportApi(String lang, Value scope) {
		super.exportApi(lang, scope);
		Exports.objectMembers(new RegistrationApi()).export(scope);
		Exports.objectMembers(new Api(this)).export(scope);
	}

	/**
	 * Refer to {@link kingfisher.scripting.RegistrationApi}.
	 */
	public final class RegistrationApi extends LiveRegistrationApi {
		private RegistrationApi() {}

		@Override
		protected ScriptThread thread() {
			return RequestScriptThread.this;
		}

		@Override
		public void addRoute(String method, String path, ScriptRouteHandler handler) {
			if (RequestScriptThread.this.handler != null) return;
			if (nextHandlerId() == targetHandler) {
				RequestScriptThread.this.handler = handler;
			}
		}

		@Override
		public void addStatelessChannel(String name, ScriptStatelessChannelHandler handler) {
			nextHandlerId();
		}
	}

	/**
	 * The API available to each script when it is executed to handle a request.
	 */
	public static final class Api extends LiveApi {
		private Api(ScriptThread thread) {
			super(thread);
		}

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

package kingfisher.channel;

import kingfisher.interop.Exports;
import kingfisher.requests.ScriptRouteHandler;
import kingfisher.scripting.HandlerRef;
import kingfisher.scripting.LiveApi;
import kingfisher.scripting.ScriptEngine;
import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.Value;

public final class ChannelScriptThread extends ScriptThread {
	private final int targetHandler;
	public ScriptChannelHandler handler;

	public ChannelScriptThread(ScriptEngine engine, HandlerRef handler) {
		super(engine, handler.script());
		this.targetHandler = handler.handlerId();
		lateInit();
	}

	@Override
	public void exportApi(String lang, Value scope) {
		super.exportApi(lang, scope);
		Exports.objectMembers(new RegistrationApi()).export(scope);
		Exports.objectMembers(new Api(engine)).export(scope);
	}

	/**
	 * Refer to {@link kingfisher.scripting.RegistrationApi}.
	 */
	public final class RegistrationApi extends kingfisher.scripting.RegistrationApi {
		private RegistrationApi() {}

		@Override
		public void addRoute(String method, String path, ScriptRouteHandler handler) {
			nextHandlerId();
		}

		@Override
		public void addChannel(String name, ScriptChannelHandler handler) {
			if (ChannelScriptThread.this.handler != null) return;
			if (nextHandlerId() == targetHandler) {
				ChannelScriptThread.this.handler = handler;
			}
		}
	}

	public static final class Api extends LiveApi {
		private Api(ScriptEngine engine) {
			super(engine);
		}
	}
}

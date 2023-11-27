package kingfisher.channel;

import kingfisher.interop.Exports;
import kingfisher.requests.ScriptRouteHandler;
import kingfisher.scripting.*;
import org.graalvm.polyglot.Value;

public final class StatelessChannelScriptThread extends ScriptThread {
	private final int targetHandler;
	public ScriptStatelessChannelHandler handler;

	public StatelessChannelScriptThread(ScriptEngine engine, HandlerRef handler) {
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
			return StatelessChannelScriptThread.this;
		}

		@Override
		public void addRoute(String method, String path, ScriptRouteHandler handler) {
			nextHandlerId();
		}

		@Override
		public void addStatelessChannel(String name, ScriptStatelessChannelHandler handler) {
			if (StatelessChannelScriptThread.this.handler != null) return;
			if (nextHandlerId() == targetHandler) {
				StatelessChannelScriptThread.this.handler = handler;
			}
		}
	}

	public static final class Api extends LiveApi {
		private Api(ScriptThread thread) {
			super(thread);
		}
	}
}

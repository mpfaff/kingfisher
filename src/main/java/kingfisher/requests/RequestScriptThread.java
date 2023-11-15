package kingfisher.requests;

import kingfisher.ScriptEngine;
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
		Exports.objectMembers(new Api()).export(scope);
	}

	@Override
	public Script script() {
		return script;
	}

	public final class Api extends ScriptThread.Api {
		private Api() {}

		@Override
		public void addRoute(String method, String path, ScriptRequestHandler handler) {
			if (nextHandlerId() == targetHandler) {
				RequestScriptThread.this.handler = new WrappedScriptRequestHandler(RequestScriptThread.this.eventLoop, handler);
			}
		}

		public ResponseBuilder respond() {
			return new ResponseBuilder();
		}

		public ResponseBuilder respond(int status) {
			return respond().status(status);
		}
	}
}
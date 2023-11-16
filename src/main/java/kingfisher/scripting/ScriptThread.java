package kingfisher.scripting;

import kingfisher.interop.Exports;
import kingfisher.interop.js.JSApi;
import org.graalvm.polyglot.Value;

/**
 * A script "thread". Each and every invocation of a script (for example, a route handler) gets its own thread.
 */
public abstract class ScriptThread {
	/**
	 * The global script engine.
	 */
	public final ScriptEngine engine;

	/**
	 * The thread's event loop.
	 */
	public final EventLoop eventLoop;

	private int nextHandlerId;

	public ScriptThread(ScriptEngine engine) {
		this.engine = engine;
		this.eventLoop = new EventLoop(engine);
	}

	public void exportApi(String lang, Value scope) {
		Exports.objectMembers(new Api(this)).export(scope);
		switch (lang) {
			case "js" -> Exports.objectMembers(new JSApi(eventLoop)).export(scope);
			default -> {
			}
		}
	}

	protected final void resetHandlerId() {
		this.nextHandlerId = 0;
	}

	protected final int nextHandlerId() {
		return nextHandlerId++;
	}

	public abstract Script script();
}

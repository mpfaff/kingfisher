package kingfisher.scripting;

import kingfisher.interop.Exports;
import kingfisher.interop.js.JSApi;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * A script "thread". Each and every invocation of a script (for example, a route handler) gets its own thread.
 */
public abstract class ScriptThread implements AutoCloseable {
	/**
	 * The global script engine.
	 */
	public final ScriptEngine engine;

	/**
	 * The script being executed on the thread.
	 */
	public final Script script;

	// @Stable
	private Context context;

	/**
	 * The thread's event loop.
	 */
	public final EventLoop eventLoop;

	private int nextHandlerId;

	public ScriptThread(ScriptEngine engine, Script script) {
		this.engine = engine;
		this.script = script;
		this.eventLoop = new EventLoop(engine);
	}

	public void lateInit() {
		if (context != null) throw new IllegalStateException();
		context = engine.createScriptContext(this);
		context.enter();
		engine.loadScript(context, script);
	}

	public final Context context() {
		return context;
	}

	public void exportApi(String lang, Value scope) {
		Exports.objectMembers(new Api(this)).export(scope);
		switch (lang) {
			case "js" -> Exports.objectMembers(new JSApi(eventLoop)).export(scope);
			default -> {
			}
		}
	}

	protected final int nextHandlerId() {
		return nextHandlerId++;
	}

	@Override
	public void close() {
		try {
			context.leave();
		} finally {
			context.close();
		}
	}
}

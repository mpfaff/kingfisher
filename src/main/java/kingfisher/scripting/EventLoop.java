package kingfisher.scripting;

import kingfisher.ScriptEngine;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class EventLoop {
	public final ScriptEngine engine;
	private final ConcurrentLinkedQueue<Microtask> microtaskQueue = new ConcurrentLinkedQueue<>();

	public EventLoop(ScriptEngine engine) {
		this.engine = engine;
	}

	public void submitMicrotask(Microtask microtask) {
		microtaskQueue.add(microtask);
	}

	/**
	 * Runs a microtask.
	 *
	 * @return whether any microtask was available to run.
	 */
	public boolean runMicrotask() {
		Microtask task;
		if ((task = microtaskQueue.poll()) == null) return false;
		task.execute();
		return true;
	}
}

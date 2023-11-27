package kingfisher.scripting;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class EventLoop {
	public final ScriptEngine engine;
	private final BlockingQueue<Microtask> microtaskQueue = new LinkedBlockingQueue<>();

	public EventLoop(ScriptEngine engine) {
		this.engine = engine;
	}

	public int queuedMicrotaskCount() {
		return microtaskQueue.size();
	}

	public void submitMicrotask(Microtask microtask) {
		microtaskQueue.add(microtask);
	}

	/**
	 * Runs a microtask.
	 *
	 * @param wait whether to wait for a microtask to be added.
	 *
	 * @return whether any microtask was available to run.
	 */
	public boolean runMicrotask(boolean wait) {
		Microtask task;
		if (wait) {
			try {
				task = microtaskQueue.take();
			} catch (InterruptedException e) {
				return false;
			}
		} else {
			if ((task = microtaskQueue.poll()) == null) return false;
		}
		task.execute();
		return true;
	}
}

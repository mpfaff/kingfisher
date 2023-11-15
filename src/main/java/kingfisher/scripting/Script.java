package kingfisher.scripting;

import org.graalvm.polyglot.Source;

import java.util.function.Supplier;

public final class Script {
	private final Source source;
	private Object state;
	// @Stable
	private boolean initializedState = false;

	public Script(Source source) {
		this.source = source;
	}

	public Source source() {
		return source;
	}

	public String name() {
		return source.getName();
	}

	/**
	 * Scripts are re-evaluated for each request, so they need a way to share state.
	 * <p>
	 * For each script that calls this method, {@code initFunction} will be run once during initialization, and then
	 * the value will be stored and returned when the script is reevaluated on each request.
	 * <p>
	 * <strong>Note:</strong> This method is intended to be called at most once per script, and only the first
	 * invocation's {@code initFunction} will be used.
	 */
	public Object getState(Supplier<Object> initFunction) {
		if (initializedState) return state;
		initializedState = true;
		return state = initFunction.get();
	}

	@Override
	public String toString() {
		return name();
	}
}

package kingfisher.interop;

import java.util.function.Function;

/**
 * An interface for a potentially concurrent or parallel task that may be awaited by scripts.
 */
@FunctionalInterface
public interface Task<T> {
	/**
	 * Awaits the completion of the task and returns its result.
	 * <p>
	 * If the task completes with an error, that error will be directly thrown by join.
	 */
	T join();

	default <R> Task<R> then(Function<T, R> func) {
		return () -> func.apply(join());
	}

	default <R> Task<R> thenAsync(Function<T, Task<R>> func) {
		return () -> func.apply(join()).join();
	}
}

package kingfisher.interop.js;

import dev.pfaff.log4truth.Logger;
import kingfisher.scripting.ScriptThread;
import kingfisher.util.Errors;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.concurrent.*;

import static dev.pfaff.log4truth.StandardTags.DEBUG;
import static kingfisher.util.ThrowUnchecked.throwUnchecked;

/**
 * A Java {@link java.util.concurrent.Future} as a JavaScript Promise.
 * @param <T> the result type.
 */
public final class JPromise<T> {
	private final CompletionStage<T> future;
	private final ScriptThread thread;

	public JPromise(CompletionStage<T> future, ScriptThread thread) {
		this.future = future;
		this.thread = thread;
	}

	public static <T> JPromise<T> submitToExecutor(Callable<T> callable, ExecutorService executor, ScriptThread thread) {
		var fut = new CompletableFuture<T>();
		executor.submit(() -> {
			try {
				fut.complete(callable.call());
			} catch (Throwable e) {
				fut.completeExceptionally(e);
			}
		});
		return new JPromise<>(fut, thread);
	}

	public JPromise<?> then(Value onResolve) {
		return then(onResolve, null);
	}

	public JPromise<?> then(Value onResolve, Value onReject) {
		Logger.log(() -> "registering then callbacks on JPromise: " + onResolve + ", " + onReject, List.of(DEBUG));
		var fut = new CompletableFuture<Value>();
		future.whenComplete((ok, err) -> {
			err = Errors.unwrapError(err);
			Logger.log(() -> "completed", List.of(DEBUG));
			var err_ = err;
			thread.submitMicrotask(() -> {
				try {
					Value value;
					if (err_ == null) {
						value = onResolve.execute(ok);
					} else if (onReject != null) {
						value = onReject.execute(err_);
					} else {
						throw throwUnchecked(err_);
					}
					fut.complete(value);
				} catch (Throwable e) {
					fut.completeExceptionally(e);
				}
				Logger.log(() -> ".then callbacks completed: " + fut, List.of(DEBUG));
			});
		});
		return new JPromise<>(fut, thread);
	}
}

package kingfisher.interop.js;

import dev.pfaff.log4truth.Logger;
import kingfisher.scripting.EventLoop;
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
	private final EventLoop eventLoop;

	public JPromise(CompletionStage<T> future, EventLoop eventLoop) {
		this.future = future;
		this.eventLoop = eventLoop;
	}

	public static <T> JPromise<T> submitToExecutor(Callable<T> callable, ExecutorService executor, EventLoop eventLoop) {
		var fut = new CompletableFuture<T>();
		executor.submit(() -> {
			try {
				fut.complete(callable.call());
			} catch (Throwable e) {
				fut.completeExceptionally(e);
			}
		});
		return new JPromise<>(fut, eventLoop);
	}

	public JPromise<?> then(Value onResolve) {
		return then(onResolve, null);
	}

	public JPromise<?> then(Value onResolve, Value onReject) {
		if (onResolve == null || !onResolve.canExecute()) {
			throw new IllegalArgumentException("Expected an executable value for parameter 'onResolve'");
		}
		Logger.log(() -> "registering then callbacks on JPromise: " + onResolve + ", " + onReject, List.of(DEBUG));
		var fut = new CompletableFuture<Value>();
		future.whenComplete((ok, err) -> {
			{
				var err_ = err;
				Logger.log(() -> "completed: e=" + err_, List.of(DEBUG));
			}
			err = Errors.unwrapError(err);
			var err_ = err;
			eventLoop.submitMicrotask(() -> {
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
		return new JPromise<>(fut, eventLoop);
	}
}

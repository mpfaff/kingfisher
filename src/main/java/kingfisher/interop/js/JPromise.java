package kingfisher.interop.js;

import dev.pfaff.log4truth.Logger;
import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.Value;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

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
		Logger.log(() -> "registering then callbacks on JPromise: " + onResolve + ", " + onReject);
		var fut = new CompletableFuture<Value>();
		future.whenComplete((ok, err) -> {
			Logger.log(() -> "JPromise completed");
			thread.submitMicrotask(() -> {
				try {
					Value value;
					if (err == null) {
						value = onResolve.execute(ok);
					} else if (onReject != null) {
						value = onReject.execute(err);
					} else {
						throw throwUnchecked(err);
					}
					fut.complete(value);
				} catch (Throwable e) {
					fut.completeExceptionally(e);
				}
				Logger.log(() -> "JPromise.then callbacks completed: " + fut);
			});
		});
		return new JPromise<>(fut, thread);
	}
}

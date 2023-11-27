package kingfisher.util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class Errors {
	public static Throwable unwrapError(Throwable e) {
		if (e == null) return null;
		while (e instanceof ExecutionException || e instanceof CompletionException || e.getClass() == RuntimeException.class) {
			var cause = e.getCause();
			if (cause == null || cause == e) break;
			e = cause;
		}
		return e;
	}

	public static Exception wrapThrowable(Throwable e) {
		return e instanceof Exception ex ? ex : new Exception(e);
	}
}

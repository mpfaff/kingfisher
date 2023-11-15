package kingfisher.util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class Errors {
	public static Throwable unwrapError(Throwable e) {
		while (e instanceof ExecutionException || e instanceof CompletionException) {
			e = e.getCause();
		}
		return e;
	}

	public static Exception wrapError(Throwable e) {
		return e instanceof Exception ex ? ex : new Exception(e);
	}
}

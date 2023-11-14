package kingfisher.util;

public final class ThrowUnchecked {
	private static <T extends Throwable> RuntimeException throwGeneric(Throwable e) throws T {
		throw (T) e;
	}

	public static RuntimeException throwUnchecked(Throwable e) {
		throw throwGeneric(e);
	}
}

package kingfisher.util;

public final class Timing {
	public static String formatTime(long nanos) {
		return "%.2f ms".formatted(nanos / 1_000_000.0);
	}
}

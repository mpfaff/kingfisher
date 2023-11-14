package kingfisher.interop;

import org.graalvm.polyglot.Value;

public final class ValueUtil {
	public static <T> T asOrNull(Value value, Class<?> targetType) {
		try {
			return (T) value.as(targetType);
		} catch (Throwable e) {
			return null;
		}
	}
}

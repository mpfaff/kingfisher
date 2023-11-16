package kingfisher;

import dev.pfaff.log4truth.LogFilter;

import java.nio.file.Path;
import java.util.Objects;

import static dev.pfaff.log4truth.StandardTags.DEBUG;

public final class Config {
	private static String envVar(String name, String defaultValue) {
		return Objects.requireNonNullElse(System.getenv(name), defaultValue);
	}

	private static boolean parseBoolean(String value) {
		return !value.isEmpty();
	}

	public static final int BIND_PORT = Integer.parseInt(envVar("BIND_PORT", "8080"));
	public static final Path SCRIPTS_DIR = Path.of(envVar("SCRIPTS_DIR", "scripts"));
	public static final Path TEMPLATES_DIR = Path.of(envVar("TEMPLATES_DIR", "templates"));

	public static boolean shouldLog(String tag) {
		return parseBoolean(envVar("LOG_" + tag, ""));
	}
	public static LogFilter envLogFilter(String tag) {
		if (shouldLog(tag)) {
			return (source, tags, thread) -> true;
		} else {
			return (source, tags, thread) -> !tags.contains(tag);
		}
	}
}

package kingfisher;

import java.nio.file.Path;
import java.util.Objects;

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
}

package westmount.codingclub;

import name.martingeisse.grumpyrest_jetty_launcher.GrumpyrestJettyLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class Main {
	public static final Logger WEB_LOGGER = LoggerFactory.getLogger("Web Engine");
	public static final Logger SCRIPT_LOGGER = LoggerFactory.getLogger("Script Engine");

	private static String envVar(String name, String defaultValue) {
		return Objects.requireNonNullElse(System.getenv(name), defaultValue);
	}

	public static final Path SCRIPTS_DIR = Path.of(envVar("SCRIPTS_DIR", "scripts"));
	public static final Path TEMPLATES_DIR = Path.of(envVar("TEMPLATES_DIR", "templates"));

	public static void main(String[] args) {
		ScriptEngine engine;
		try {
			engine = new ScriptEngine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try (var ctx = engine.loadScripts(new InitScriptApi(engine))) {
			engine.api.seal();

			var launcher = new GrumpyrestJettyLauncher();
			try {
				launcher.launch(engine.api);
			} catch (Throwable e) {
				WEB_LOGGER.error("Server crashed", e);
			}
		}
	}
}

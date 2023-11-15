package kingfisher.scripting;

import kingfisher.ScriptEngine;
import kingfisher.requests.CallSiteHandler;
import org.graalvm.polyglot.Source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.Watchable;
import java.util.List;

import static dev.pfaff.log4truth.StandardTags.ERROR;
import static dev.pfaff.log4truth.StandardTags.WARN;
import static kingfisher.Config.SCRIPTS_DIR;
import static kingfisher.Main.SCRIPT_LOGGER;

/**
 * Handles loading (and hot reloading) scripts.
 */
public final class ScriptLoader {
	private static final boolean USE_WATCHER = false;

	private final ScriptEngine engine;

	public ScriptLoader(ScriptEngine engine) {
		this.engine = engine;
	}

	public void launch() {
		new Thread(() -> {
			try {
				activateScripts();
			} catch (Exception e) {
				SCRIPT_LOGGER.log(() -> "Caught exception while activating scripts", e, List.of(ERROR));
			}

			try {
				if (USE_WATCHER) {
					var watcher = SCRIPTS_DIR.getFileSystem().newWatchService();
					((Watchable) SCRIPTS_DIR).register(watcher,
							StandardWatchEventKinds.ENTRY_CREATE,
							StandardWatchEventKinds.ENTRY_MODIFY,
							StandardWatchEventKinds.ENTRY_DELETE,
							StandardWatchEventKinds.OVERFLOW);
					while (true) {
						var key = watcher.take();
						// we don't care what they are.
						key.pollEvents();
						hotReload();
					}
				} else {
					var is = System.in;
					int b;
					while ((b = is.read()) != -1) {
						if (b == 'r') hotReload();
					}
				}
			} catch (IOException e) {
				SCRIPT_LOGGER.log(() -> "Unable to launch hot reload service", e, List.of(ERROR));
			} catch (InterruptedException e) {
				SCRIPT_LOGGER.log(() -> "Hot reload service interrupted", e, List.of(ERROR));
			}
		}, "Script Loader").start();
	}

	private void hotReload() {
		SCRIPT_LOGGER.log(() -> "Change detected. Hot reloading scripts", List.of(WARN));
		try {
			activateScripts();
			engine.pebble.getTemplateCache().invalidateAll();
			SCRIPT_LOGGER.log(() -> "Hot reload complete!", List.of(WARN));
		} catch (Exception e) {
			SCRIPT_LOGGER.log(() -> "Caught exception while hot reloading scripts", e, List.of(ERROR));
		}
	}

	private void activateScripts() throws Exception {
		var staging = new Registrar();
		var api = new RegistrationScriptThread(engine, staging);
		try (var ctx = engine.createRegistrationScriptContext(api)) {
			for (var script : ScriptLoader.loadScripts()) {
				api.setScript(script);
				engine.loadScript(ctx, script);
			}
		}
		staging.complete();
		engine.handler.setTarget(CallSiteHandler.chainHandlers(staging.requestHandlers));
	}

	private static List<Script> loadScripts() throws IOException {
		try (var stream = Files.list(SCRIPTS_DIR)) {
			return stream.sorted().map(scriptPath -> {
						var file = scriptPath.toFile();
						try {
							var source = Source.newBuilder(Source.findLanguage(file),
									Files.readString(scriptPath),
									scriptPath.toString()).build();
							SCRIPT_LOGGER.log(() -> "Read script " + scriptPath);
							return new Script(source);
						} catch (Throwable e) {
							SCRIPT_LOGGER.log(() -> "Unable to read script " + scriptPath, e, List.of(ERROR));
							return null;
						}
					})
					.toList();
		} catch (Throwable e) {
			SCRIPT_LOGGER.log(() -> "Unable to read scripts", e, List.of(ERROR));
			throw e;
		}
	}
}

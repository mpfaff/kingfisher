package kingfisher.scripting;

import dev.pfaff.log4truth.Logger;
import kingfisher.requests.CallSiteHandler;
import kingfisher.requests.RequestHandler;
import org.graalvm.polyglot.Source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.Watchable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static dev.pfaff.log4truth.StandardTags.*;
import static kingfisher.Config.SCRIPTS_DIR;
import static kingfisher.Main.SCRIPT_LOGGER;
import static kingfisher.Main.WEB_LOGGER;

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

	public void hotReload() {
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
		var registrar = new Registrar();
		ScriptLoader.loadScripts().stream().map(script -> engine.runWithScript(
				() -> new RegistrationScriptThread(engine, new Registrar(), script),
				"Script registration",
				thread -> {
					thread.script.permissions = Set.copyOf(thread.script.permissions);
					thread.registrar.complete();
					Logger.log(() -> "Registered script " + script);
					return thread;
				}
		)).forEach(fut -> {
			try {
				var thread = fut.get();
				registrar.requestHandlers.addAll(thread.registrar.requestHandlers);
				registrar.channelHandlers.putAll(thread.registrar.channelHandlers);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		registrar.complete();
		WEB_LOGGER.log(() -> {
			var sb = new StringBuilder("request handlers:");
			for (var handler : registrar.requestHandlers) {
				sb.append("\n - ").append(handler);
			}
			return sb.toString();
		}, List.of(DEBUG));
		WEB_LOGGER.log(() -> {
			var sb = new StringBuilder("channel handlers:");
			registrar.channelHandlers.forEach((name, handler) -> {
				sb.append("\n - ").append(name).append(": ").append(handler);
			});
			return sb.toString();
		}, List.of(DEBUG));
		engine.requestHandler.setTarget(CallSiteHandler.chainHandlers(engine, registrar.requestHandlers));
		engine.channelHandlers = Map.copyOf(registrar.channelHandlers);
	}

	private static String detectLanguage(File file) throws IOException {
		return Source.findLanguage(file);
	}

	private static List<Script> loadScripts() throws IOException {
		try (var stream = Files.list(SCRIPTS_DIR)) {
			return stream.sorted().map(scriptPath -> {
						var file = scriptPath.toFile();
						try {
							var script = new Script(Source.newBuilder(detectLanguage(file), file)
									.name(scriptPath.toString())
									.build());
							SCRIPT_LOGGER.log(() -> "Read script " + script);
							return script;
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

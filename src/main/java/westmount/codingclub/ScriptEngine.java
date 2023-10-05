package westmount.codingclub;

import io.pebbletemplates.pebble.PebbleEngine;
import name.martingeisse.grumpyrest.RestApi;
import name.martingeisse.grumpyrest.request.HttpMethod;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import westmount.codingclub.constants.ContentType;
import westmount.codingclub.constants.Header;
import westmount.codingclub.templating.FileLoader;
import westmount.codingclub.templating.OurExtension;
import westmount.codingclub.util.ProxyConstantTable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static westmount.codingclub.Main.*;

public final class ScriptEngine {
	public static final Context.Builder CONTEXT_BUILDER = Context.newBuilder()
			.allowAllAccess(true)
			.engine(Engine.newBuilder("js", "python", "llvm", "wasm")
					.logHandler(new PolyglotLogHandler())
					.build());

	public final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofPlatform()
			.name("Script Executor #", 0).factory());

	public final PebbleEngine pebble = new PebbleEngine.Builder()
			.cacheActive(true)
			.autoEscaping(false)
			.executorService(executor)
			.strictVariables(true)
			.loader(new FileLoader(TEMPLATES_DIR))
			.extension(new OurExtension())
			.build();
	public final RestApi api = new RestApi();
	private final List<Source> sources;

	public ScriptEngine() throws IOException {
		try (var stream = Files.list(SCRIPTS_DIR)) {
			sources = stream.map(scriptPath -> {
						var file = scriptPath.toFile();
						try {
							var source = Source.newBuilder(Source.findLanguage(file),
									Files.readString(scriptPath),
									scriptPath.toString()).build();
							SCRIPT_LOGGER.info("Read script " + scriptPath);
							return source;
						} catch (Throwable e) {
							SCRIPT_LOGGER.error("Unable to read script " + scriptPath, e);
							return null;
						}
					})
					.toList();
		} catch (Throwable e) {
			SCRIPT_LOGGER.error("Unable to read scripts", e);
			throw e;
		}
	}

	public Context loadScripts(ScriptApi api) {
		var ctx = CONTEXT_BUILDER.build();
		try {
			var scriptApi = ctx.asValue(api);
			for (var lang : ctx.getEngine().getLanguages().keySet()) {
				var bindings = ctx.getBindings(lang);
				if (!bindings.hasMembers()) continue;
				if ("llvm".equals(lang)) continue;
				if ("wasm".equals(lang)) continue;

				try {
					for (var member : scriptApi.getMemberKeys()) {
						bindings.putMember(member, scriptApi.getMember(member));
					}

					// these are commonly used enough to warrant them being unscoped...
					for (var method : HttpMethod.values()) {
						bindings.putMember(method.name(), method);
					}

					// but these will be scoped.
					bindings.putMember("ContentType", new ProxyConstantTable(ContentType.class));
					bindings.putMember("Header", new ProxyConstantTable(Header.class));
				} catch (Throwable e) {
					SCRIPT_LOGGER.error("Caught exception while binding api for language " + lang, e);
					throw e;
				}
			}

			try {
				sources.forEach(source -> {
					api.startNewScript(source.getName());

					try {
						ctx.eval(source);
						SCRIPT_LOGGER.info("Loaded script " + source.getName());
					} catch (Throwable e) {
						SCRIPT_LOGGER.error("Unable to load script " + source.getName(), e);
					}
				});
			} catch (Throwable e) {
				SCRIPT_LOGGER.error("Unable to load scripts", e);
			}

			return ctx;
		} catch (Throwable e) {
			ctx.close(true);
			throw e;
		}
	}

	private static class PolyglotLogHandler extends Handler {
		private static final Logger LOGGER = LoggerFactory.getLogger("Polyglot Engine");

		@Override
		public void publish(LogRecord record) {
			var levelIn = record.getLevel();
			Level level;
			if (levelIn == java.util.logging.Level.OFF) return;
			else if (levelIn == java.util.logging.Level.SEVERE) level = Level.ERROR;
			else if (levelIn == java.util.logging.Level.WARNING) level = Level.WARN;
			else if (levelIn == java.util.logging.Level.INFO) level = Level.INFO;
			else if (levelIn == java.util.logging.Level.CONFIG) level = Level.DEBUG;
			else if (levelIn == java.util.logging.Level.FINE) level = Level.DEBUG;
			else if (levelIn == java.util.logging.Level.FINER) level = Level.DEBUG;
			else if (levelIn == java.util.logging.Level.FINEST) level = Level.DEBUG;
			else if (levelIn == java.util.logging.Level.ALL) return;
			else return;
			LOGGER.makeLoggingEventBuilder(level)
					.setCause(record.getThrown())
					.log(record.getMessage());
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	}
}

package kingfisher;

import io.pebbletemplates.pebble.PebbleEngine;
import kingfisher.constants.ContentType;
import kingfisher.constants.Header;
import kingfisher.constants.Method;
import kingfisher.requests.MatchingRequestHandler;
import kingfisher.requests.ScriptRequestHandler;
import kingfisher.templating.FileLoader;
import kingfisher.templating.OurExtension;
import kingfisher.util.ProxyConstantTable;
import org.graalvm.polyglot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import static kingfisher.Config.*;

public final class ScriptEngine {
	private final Engine engine = Engine.newBuilder("js", "python", "llvm", "wasm")
			.logHandler(new PolyglotLogHandler())
			.build();
	public final Map<String, Context.Builder> langContextBuilders = engine.getLanguages()
			.keySet()
			.stream()
			.collect(Collectors.toMap(Function.identity(), this::makeBuilder));
	public final Context.Builder allLangContextBuilder = makeBuilder(engine.getLanguages().keySet().toArray(String[]::new));

	private Context.Builder makeBuilder(String... languages) {
		return Context.newBuilder(languages)
				.allowAllAccess(true)
				.allowHostAccess(HostAccess.newBuilder()
						// TODO: figure out why it doesn't work when enabled
						.methodScoping(false)
						.allowPublicAccess(true)
						.allowImplementations(Supplier.class)
						.allowImplementations(ScriptRequestHandler.class)
						.build())
				.engine(engine);
	}

	private static final Map<String, Object> CONSTANT_BINDINGS = Map.of(
			"ContentType", new ProxyConstantTable(ContentType.class),
			"Header", new ProxyConstantTable(Header.class),
			"Method", new ProxyConstantTable(Method.class)
	);

	public final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofPlatform()
			.name("Script Executor #", 0).factory());

	public final PebbleEngine pebble = new PebbleEngine.Builder()
			.cacheActive(true)
			.autoEscaping(false)
			.executorService(executor)
			.strictVariables(true)
			.loader(new FileLoader(TEMPLATES_DIR))
			.extension(new OurExtension(this))
			.build();
	public final List<MatchingRequestHandler> handlers = new ArrayList<>();
	private final List<Source> sources;

	public ScriptEngine() throws IOException {
		try (var stream = Files.list(SCRIPTS_DIR)) {
			sources = stream.map(scriptPath -> {
						var file = scriptPath.toFile();
						try {
							var source = Source.newBuilder(Source.findLanguage(file),
									Files.readString(scriptPath),
									scriptPath.toString()).build();
							Main.SCRIPT_LOGGER.info("Read script " + scriptPath);
							return source;
						} catch (Throwable e) {
							Main.SCRIPT_LOGGER.error("Unable to read script " + scriptPath, e);
							return null;
						}
					})
					.toList();
		} catch (Throwable e) {
			Main.SCRIPT_LOGGER.error("Unable to read scripts", e);
			throw e;
		}
	}

	public int scriptCount() {
		return sources.size();
	}

	private void setupBindings(Context ctx, String lang, Value scriptApi) {
		var start = System.currentTimeMillis();
		long elapsed;

		// these don't support getBindings().putMember()
		if ("llvm".equals(lang)) return;
		if ("wasm".equals(lang)) return;

		// Profiling shows that this takes a long time because the language is lazily initialized and this is where it
		// happens. Specifically, it appears that libpython/importlib/_bootstrap.py#__import__ takes the vast majority
		// of the time. I suspect this is not because __import__ itself is slow, but rather the GraalPy interpreter,
		// combined with the size of the python standard library being imported.
		var bindings = ctx.getBindings(lang);

		if (TRACE_SCRIPT_ENGINE) {
			elapsed = System.currentTimeMillis() - start;
			Main.SCRIPT_LOGGER.info("Got bindings object for '" + lang + "' in " + elapsed + " ms");
		}

		if (!bindings.hasMembers()) return;

		try {
			for (var member : scriptApi.getMemberKeys()) {
				bindings.putMember(member, scriptApi.getMember(member));
			}

			// these are commonly used enough to warrant them being unscoped...
			for (var method : List.of(Method.GET, Method.POST, Method.PUT, Method.POST, Method.DELETE, Method.HEAD)) {
				bindings.putMember(method, method);
			}

			CONSTANT_BINDINGS.forEach(bindings::putMember);

			if (TRACE_SCRIPT_ENGINE) {
				elapsed = System.currentTimeMillis() - start;
				Main.SCRIPT_LOGGER.info("Setup bindings for '" + lang + "' in " + elapsed + " ms");
			}
		} catch (Throwable e) {
			Main.SCRIPT_LOGGER.error("Caught exception while binding api for language " + lang, e);
			throw e;
		}
	}

	public Context createScriptContext(ScriptApi api) {
		var ctx = allLangContextBuilder.build();

		var scriptApi = ctx.asValue(api);

		try {
			for (var lang : engine.getLanguages().keySet()) {
				setupBindings(ctx, lang, scriptApi);
			}

			return ctx;
		} catch (Throwable e) {
			ctx.close(true);
			throw e;
		}
	}

	public Context createScriptContext(ScriptApi api, int scriptIndex) {
		var lang = sources.get(scriptIndex).getLanguage();

		var ctx = langContextBuilders.get(lang).build();

		var scriptApi = ctx.asValue(api);

		try {
			setupBindings(ctx, lang, scriptApi);

			return ctx;
		} catch (Throwable e) {
			ctx.close(true);
			throw e;
		}
	}

	public void loadScript(Context ctx, int scriptIndex) {
		var start = System.currentTimeMillis();
		long elapsed;

		var source = sources.get(scriptIndex);

		try {
			ctx.eval(source);

			elapsed = System.currentTimeMillis() - start;
			Main.SCRIPT_LOGGER.info("Loaded script " + source.getName() + " in " + elapsed + " ms");
		} catch (Throwable e) {
			Main.SCRIPT_LOGGER.error("Unable to load script " + source.getName(), e);
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

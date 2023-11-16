package kingfisher.scripting;

import dev.pfaff.log4truth.NamedLogger;
import io.pebbletemplates.pebble.PebbleEngine;
import kingfisher.interop.js.JSApiNodeFS;
import kingfisher.interop.js.JSImplementations;
import kingfisher.requests.CallSiteHandler;
import kingfisher.requests.ScriptRouteHandler;
import kingfisher.templating.FileLoader;
import kingfisher.templating.OurExtension;
import kingfisher.util.BlockingOfferQueue;
import kingfisher.util.Cache;
import kingfisher.util.MapCache;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;

import java.lang.invoke.MutableCallSite;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.pfaff.log4truth.StandardTags.*;
import static kingfisher.Config.TEMPLATES_DIR;
import static kingfisher.Main.SCRIPT_LOGGER;
import static kingfisher.util.Timing.formatTime;

public final class ScriptEngine {
	private final Engine engine = Engine.newBuilder("js", "python", "llvm", "wasm")
			.logHandler(new PolyglotLogHandler())
			.option("js.unhandled-rejections", "throw")
			.build();
	public static final HostAccess hostAccess;
	static {
		var builder = HostAccess.newBuilder()
				// TODO: figure out why it doesn't work when enabled
				.methodScoping(false)
				.allowPublicAccess(true)
				.allowImplementations(Supplier.class)
				.allowImplementations(ScriptRouteHandler.class);
		Api.registerTypes(builder);
		JSApiNodeFS.registerTypes(builder);
		hostAccess = builder.build();
	}
	public final Map<String, Context.Builder> langContextBuilders = engine.getLanguages()
			.keySet()
			.stream()
			.collect(Collectors.toMap(Function.identity(), this::makeBuilder));

	// executors
	public final ExecutorService executor = new ThreadPoolExecutor(
			Runtime.getRuntime().availableProcessors(),
//			Runtime.getRuntime().availableProcessors() * 32,
			Runtime.getRuntime().availableProcessors(),
			60L,
			TimeUnit.SECONDS,
			new BlockingOfferQueue<>(new ArrayBlockingQueue<>(Runtime.getRuntime().availableProcessors())),
			Thread.ofPlatform()
				.name("Script Executor #", 0)
				.factory()
	);
	public final ExecutorService ioExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
			.name("I/O #", 0)
			.factory());
	public final ExecutorService httpClientExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
			.name("Http Client #", 0)
			.factory());
	public final HttpClient httpClient = HttpClient.newBuilder()
			.executor(httpClientExecutor)
			.build();

//	public final Cache<String, Pattern> patternCache = new MapCache<>(Pattern::compile);

	public ScriptEngine() {
		handler = new MutableCallSite(CallSiteHandler.chainHandlers(this, List.of()));
	}


	private Context.Builder makeBuilder(String... languages) {
		return Context.newBuilder(languages)
				.allowAllAccess(true)
				.allowHostAccess(hostAccess)
				.engine(engine);
	}

	public final PebbleEngine pebble = new PebbleEngine.Builder()
			.cacheActive(true)
			.autoEscaping(false)
			.executorService(executor)
			.strictVariables(true)
			.loader(new FileLoader(TEMPLATES_DIR))
			.extension(new OurExtension(this))
			.build();
	public final MutableCallSite handler;

	private void setupBindings(ScriptThread thread, Context ctx, String lang) {
		var start = System.nanoTime();

		// these don't support getBindings().putMember()
		switch (lang) {
			case "llvm", "wasm" -> {
				return;
			}
		}

		// Profiling shows that this takes a long time because the language is lazily initialized and this is where it
		// happens. Specifically, it appears that libpython/importlib/_bootstrap.py#__import__ takes the vast majority
		// of the time. I suspect this is not because __import__ itself is slow, but rather the GraalPy interpreter,
		// combined with the size of the python standard library being imported.
		var bindings = ctx.getBindings(lang);

		// TODO: refactor and remove this
		switch (lang) {
			case "js" -> {
				for (Source function : JSImplementations.functions) {
					bindings.putMember(function.getName(), ctx.eval(function));
				}
			}
		}

		{
			long now = System.nanoTime();
			long elapsed = now - start;
			SCRIPT_LOGGER.log(() -> "Got bindings object for '" + lang + "' in " + formatTime(elapsed), List.of("TIMING"));
			start = now;
		}

		if (!bindings.hasMembers()) return;

		try {
			thread.exportApi(lang, bindings);

			{
				long now = System.nanoTime();
				long elapsed = now - start;
				SCRIPT_LOGGER.log(() -> "Setup bindings for '" + lang + "' in " + formatTime(elapsed), List.of("TIMING"));
				start = now;
			}
		} catch (Throwable e) {
			SCRIPT_LOGGER.log(() -> "Caught exception while binding api for language " + lang, e, List.of(ERROR));
			throw e;
		}
	}

	Context createScriptContext(ScriptThread thread) {
		var lang = thread.script.source().getLanguage();

		var ctx = langContextBuilders.get(lang).build();

		try {
			setupBindings(thread, ctx, lang);

			return ctx;
		} catch (Throwable e) {
			ctx.close(true);
			throw e;
		}
	}

	void loadScript(Context ctx, Script script) {
		var start = System.nanoTime();
		long elapsed;

		try {
			var evalResult = ctx.eval(script.source());

//			SCRIPT_LOGGER.log(() -> "Script evaluated to " + evalResult);
//			SCRIPT_LOGGER.log(() -> "Exports: " + evalResult.getMemberKeys());
//
//			if (evalResult.canInvokeMember("_kingfisher_entrypoint")) {
//				SCRIPT_LOGGER.log(() -> "Script has an entrypoint function");
//				var entrypointResult = evalResult.invokeMember("_kingfisher_entrypoint");
//				SCRIPT_LOGGER.log(() -> "Evaluated entrypoint to " + entrypointResult);
//			}

			elapsed = System.nanoTime() - start;
			SCRIPT_LOGGER.log(() -> "Loaded script " + script.name() + " in " + formatTime(elapsed), List.of("TIMING"));
		} catch (Throwable e) {
			SCRIPT_LOGGER.log(() -> "Unable to load script " + script.name(), e, List.of(ERROR));
		}
	}

	public <T extends ScriptThread, R> Future<R> runWithScript(Supplier<T> scriptThreadSupplier, String description, Function<T, R> action) {
		return executor.submit(() -> {
			var start = System.nanoTime();
			long elapsed;
			try (var thread = scriptThreadSupplier.get()) {
				return action.apply(thread);
			} finally {
				elapsed = System.nanoTime() - start;
				SCRIPT_LOGGER.log(() -> description + " took " + formatTime(elapsed), List.of("TIMING"));
			}
		});
	}

	private static class PolyglotLogHandler extends Handler {
		private static final NamedLogger LOGGER = new NamedLogger("Polyglot Engine");

		@Override
		public void publish(LogRecord record) {
			var levelIn = record.getLevel();
			String level;
			if (levelIn == java.util.logging.Level.OFF) return;
			else if (levelIn == java.util.logging.Level.SEVERE) level = ERROR;
			else if (levelIn == java.util.logging.Level.WARNING) level = WARN;
			else if (levelIn == java.util.logging.Level.INFO) level = INFO;
			else if (levelIn == java.util.logging.Level.CONFIG) level = DEBUG;
			else if (levelIn == java.util.logging.Level.FINE) level = DEBUG;
			else if (levelIn == java.util.logging.Level.FINER) level = DEBUG;
			else if (levelIn == java.util.logging.Level.FINEST) level = DEBUG;
			else if (levelIn == java.util.logging.Level.ALL) return;
			else return;
			LOGGER.log(record::getMessage, record.getThrown(), List.of(level));
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	}
}

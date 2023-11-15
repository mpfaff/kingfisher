package kingfisher;

import dev.pfaff.log4truth.NamedLogger;
import io.pebbletemplates.pebble.PebbleEngine;
import kingfisher.constants.ContentType;
import kingfisher.constants.Header;
import kingfisher.constants.Method;
import kingfisher.interop.ProxyConstantTable;
import kingfisher.interop.js.JSImplementations;
import kingfisher.interop.js.JSNodeFS;
import kingfisher.requests.CallSiteHandler;
import kingfisher.requests.ScriptRequestHandler;
import kingfisher.scripting.Script;
import kingfisher.templating.FileLoader;
import kingfisher.templating.OurExtension;
import org.graalvm.polyglot.*;

import java.lang.invoke.MutableCallSite;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import static dev.pfaff.log4truth.StandardTags.*;
import static kingfisher.Config.TEMPLATES_DIR;
import static kingfisher.Config.TRACE_SCRIPT_ENGINE;
import static kingfisher.util.Timing.formatTime;

public final class ScriptEngine {
	private static final Map<String, Object> CONSTANT_BINDINGS = Map.of(
			"ContentType", new ProxyConstantTable(ContentType.class),
			"Header", new ProxyConstantTable(Header.class),
			"Method", new ProxyConstantTable(Method.class)
	);

	private final Engine engine = Engine.newBuilder("js", "python", "llvm", "wasm")
			.logHandler(new PolyglotLogHandler())
			.option("js.unhandled-rejections", "throw")
			.build();
	public final Map<String, Context.Builder> langContextBuilders = engine.getLanguages()
			.keySet()
			.stream()
			.collect(Collectors.toMap(Function.identity(), this::makeBuilder));
	public final Context.Builder allLangContextBuilder =
			makeBuilder(engine.getLanguages().keySet().toArray(String[]::new));

	// executors
	public final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofPlatform()
			.name("Script Executor #", 0)
			.factory());
	public final ExecutorService ioExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
			.name("I/O #", 0)
			.factory());
	public final ExecutorService httpClientExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
			.name("Http Client #", 0)
			.factory());
	public final HttpClient httpClient = HttpClient.newBuilder()
			.executor(httpClientExecutor)
			.build();


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

	public final PebbleEngine pebble = new PebbleEngine.Builder()
			.cacheActive(true)
			.autoEscaping(false)
			.executorService(executor)
			.strictVariables(true)
			.loader(new FileLoader(TEMPLATES_DIR))
			.extension(new OurExtension(this))
			.build();
	public final MutableCallSite handler = new MutableCallSite(CallSiteHandler.chainHandlers(List.of()));

	private void setupBindings(Context ctx, String lang, Value scriptApi, Map<String, Value> modules) {
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

		switch (lang) {
			case "js" -> {
				for (Source function : JSImplementations.functions) {
					bindings.putMember(function.getName(), ctx.eval(function));
				}
			}
		}

		if (TRACE_SCRIPT_ENGINE) {
			long now = System.nanoTime();
			long elapsed = now - start;
			Main.SCRIPT_LOGGER.log(() -> "Got bindings object for '" + lang + "' in " + formatTime(elapsed));
			start = now;
		}

		if (!bindings.hasMembers()) return;

		try {
			for (var member : scriptApi.getMemberKeys()) {
				bindings.putMember(member, scriptApi.getMember(member));
			}

			for (var module : modules.entrySet()) {
				bindings.putMember(module.getKey(), module.getValue());
			}

			// these are commonly used enough to warrant them being unscoped...
			for (var method : List.of(Method.GET, Method.POST, Method.PUT, Method.POST, Method.DELETE, Method.HEAD)) {
				bindings.putMember(method, method);
			}

			CONSTANT_BINDINGS.forEach(bindings::putMember);

			if (TRACE_SCRIPT_ENGINE) {
				long now = System.nanoTime();
				long elapsed = now - start;
				Main.SCRIPT_LOGGER.log(() -> "Setup bindings for '" + lang + "' in " + formatTime(elapsed));
				start = now;
			}
		} catch (Throwable e) {
			Main.SCRIPT_LOGGER.log(() -> "Caught exception while binding api for language " + lang, e, List.of(ERROR));
			throw e;
		}
	}

	public Context createScriptContext(InitScriptApi api) {
		var ctx = allLangContextBuilder.build();

		try {
			var scriptApi = ctx.asValue(api);

			for (var lang : engine.getLanguages().keySet()) {
				setupBindings(ctx, lang, scriptApi, Map.of());
			}

			return ctx;
		} catch (Throwable e) {
			ctx.close(true);
			throw e;
		}
	}

	public Context createScriptContext(HandlerInvocationScriptApi api) {
		var lang = api.script().source().getLanguage();

		var ctx = langContextBuilders.get(lang).build();

		try {
			Map<String, Value> modules = switch (lang) {
				case "js" -> Map.of("fs", ctx.asValue(new JSNodeFS(api.thread)));
				default -> Map.of();
			};
			setupBindings(ctx, lang, ctx.asValue(api), modules);

			return ctx;
		} catch (Throwable e) {
			ctx.close(true);
			throw e;
		}
	}

	public void loadScript(Context ctx, Script script) {
		var start = System.nanoTime();
		long elapsed;

		try {
			ctx.eval(script.source());

			elapsed = System.nanoTime() - start;
			Main.SCRIPT_LOGGER.log(() -> "Loaded script " + script.name() + " in " + formatTime(elapsed));
		} catch (Throwable e) {
			Main.SCRIPT_LOGGER.log(() -> "Unable to load script " + script.name(), e, List.of(ERROR));
		}
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

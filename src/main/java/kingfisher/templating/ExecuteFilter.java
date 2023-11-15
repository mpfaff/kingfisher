package kingfisher.templating;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.graalvm.polyglot.Context;
import kingfisher.scripting.ScriptEngine;

import java.util.List;
import java.util.Map;

public final class ExecuteFilter implements Filter {
	public static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

	private final ScriptEngine engine;
	private final String language;

	public ExecuteFilter(ScriptEngine engine, String language) {
		this.engine = engine;
		this.language = language;
	}

	@Override
	public List<String> getArgumentNames() {
		return null;
	}

	private static Context getContext(ScriptEngine engine, String language) {
		var ctx = CONTEXT.get();
		if (ctx == null) {
			ctx = engine.langContextBuilders.get(language).build();
			try {
				ctx.enter();
			} catch (Throwable e) {
				ctx.close(true);
			}
			CONTEXT.set(ctx);
		}
		return ctx;
	}

	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
		return getContext(engine, language).eval(language, (String) input).execute(args.values().toArray());
	}
}

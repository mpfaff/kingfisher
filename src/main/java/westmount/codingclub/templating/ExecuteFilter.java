package westmount.codingclub.templating;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.graalvm.polyglot.Context;

import java.util.List;
import java.util.Map;

public final class ExecuteFilter implements Filter {
	private final String language;

	public ExecuteFilter(String language) {
		this.language = language;
	}

	@Override
	public List<String> getArgumentNames() {
		return null;
	}

	@Override
	public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
		return Context.getCurrent().eval(language, (String) input).execute(args.values().toArray());
	}
}

package kingfisher.templating;

import io.pebbletemplates.pebble.attributes.AttributeResolver;
import io.pebbletemplates.pebble.extension.*;
import io.pebbletemplates.pebble.operator.BinaryOperator;
import io.pebbletemplates.pebble.operator.UnaryOperator;
import io.pebbletemplates.pebble.tokenParser.TokenParser;
import kingfisher.ScriptEngine;

import java.util.List;
import java.util.Map;

public final class OurExtension implements Extension {
	private final ScriptEngine engine;

	public OurExtension(ScriptEngine engine) {
		this.engine = engine;
	}

	@Override
	public Map<String, Filter> getFilters() {
		return Map.of(
				"js", new ExecuteFilter(engine, "js"),
				"py", new ExecuteFilter(engine, "python")
		);
	}

	@Override
	public Map<String, Test> getTests() {
		return Map.of();
	}

	@Override
	public Map<String, Function> getFunctions() {
		return Map.of();
	}

	@Override
	public List<TokenParser> getTokenParsers() {
		return List.of();
	}

	@Override
	public List<BinaryOperator> getBinaryOperators() {
		return List.of();
	}

	@Override
	public List<UnaryOperator> getUnaryOperators() {
		return List.of();
	}

	@Override
	public Map<String, Object> getGlobalVariables() {
		return Map.of();
	}

	@Override
	public List<NodeVisitorFactory> getNodeVisitors() {
		return List.of();
	}

	@Override
	public List<AttributeResolver> getAttributeResolver() {
		return List.of();
	}
}

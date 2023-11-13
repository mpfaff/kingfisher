package kingfisher.scripting;

import org.graalvm.polyglot.Source;

public record Script(Source source) {
	public String name() {
		return source.getName();
	}
}

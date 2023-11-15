package kingfisher.interop;

import org.graalvm.polyglot.Value;

@FunctionalInterface
public interface Exports {
	void export(Value scope);

	static Exports objectMembers(Object obj) {
		return scope -> {
			var value = scope.getContext().asValue(obj);
			for (var key : value.getMemberKeys()) {
				scope.putMember(key, value.getMember(key));
			}
		};
	}
}

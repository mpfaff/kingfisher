package kingfisher.channel;

import org.graalvm.polyglot.Value;

/**
 * A channel handler implemented by a script.
 */
@FunctionalInterface
public interface ScriptStatelessChannelHandler {
	Value handle(Object message) throws Throwable;
}

package kingfisher.interop.js;

import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.Value;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

/**
 * Implements the `node:fs/promises` API.
 */
public final class JSNodeFS {
	private final ScriptThread thread;

	public JSNodeFS(ScriptThread thread) {
		this.thread = thread;
	}

	public JPromise<byte[]> readFile(String path) {
		return JPromise.submitToExecutor(() -> {
			return Files.readAllBytes(Path.of(path));
		}, thread.engine.ioExecutor, thread);
	}

	public JPromise<String> readFileAsString(String path, String encoding) {
		var charset = encodingToCharset(encoding);
		return JPromise.submitToExecutor(() -> {
			return Files.readString(Path.of(path), charset);
		}, thread.engine.ioExecutor, thread);
	}

	public JPromise<String> readFile(String path, Value options) {
		if (options.isString()) {
			return readFileAsString(path, options.asString());
		}
		var encoding = options.hasMember("encoding") ? options.getMember("encoding").asString() : null;
		return readFileAsString(path, encoding);
	}

	public JPromise<Void> writeFile(String path, byte[] data) {
		return JPromise.submitToExecutor(() -> {
			Files.write(Path.of(path), data);
			return null;
		}, thread.engine.ioExecutor, thread);
	}

	public JPromise<Void> writeFile(String path, String data) {
		return writeFile(path, data, "utf8");
	}

	public JPromise<Void> writeFile(String path, String data, String encoding) {
		var charset = encodingToCharset(encoding);
		return JPromise.submitToExecutor(() -> {
			Files.writeString(Path.of(path), data, charset);
			return null;
		}, thread.engine.ioExecutor, thread);
	}

	public JPromise<Void> writeFile(String path, Object data, Value options) {
		var encoding = options.hasMember("encoding") ? options.getMember("encoding").asString() : null;
		if (encoding != null && !(data instanceof String)) {
			throw new IllegalArgumentException("Encoding makes no sense with non-string data");
		}
		return data instanceof String s ? writeFile(path, s, encoding) : writeFile(path, (byte[]) data);
	}

	public JPromise<Boolean> mkdir(String path) {
		return mkdir(path, Value.asValue(null));
	}

	public JPromise<Boolean> mkdir(String path, Value options) {
		var recursive = options.hasMember("recursive") && options.getMember("recursive").asBoolean();
		return JPromise.submitToExecutor(() -> {
			Path path_ = Path.of(path);
			if (recursive) {
				var exists = Files.exists(path_);
				Files.createDirectories(path_);
				return !exists;
			} else {
				try {
					Files.createDirectory(path_);
					return true;
				} catch (FileAlreadyExistsException e) {
					return false;
				}
			}
		}, thread.engine.ioExecutor, thread);
	}

	private static Charset encodingToCharset(String encoding) {
		return switch (encoding) {
			case null -> StandardCharsets.UTF_8;
			case "utf8" -> StandardCharsets.UTF_8;
			default -> throw new UnsupportedOperationException("Unsupported encoding: " + encoding);
		};
	}
}

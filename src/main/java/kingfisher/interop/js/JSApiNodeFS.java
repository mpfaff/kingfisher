package kingfisher.interop.js;

import kingfisher.interop.OptionalField;
import kingfisher.scripting.EventLoop;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static kingfisher.interop.ValueUtil.recordConverter;

/**
 * Implements the <a href="https://nodejs.org/docs/latest/api/fs.html#promises-api">{@code node:fs/promises}</a> API.
 */
public final class JSApiNodeFS {
	private final EventLoop eventLoop;

	public JSApiNodeFS(EventLoop eventLoop) {
		this.eventLoop = eventLoop;
	}

	public static void registerTypes(HostAccess.Builder builder) {
		builder.targetTypeMapping(Value.class,
				ReadFileOptions.class,
				v -> v.hasMembers() || v.hasHashEntries(),
				recordConverter(ReadFileOptions.class));
		builder.targetTypeMapping(Value.class,
				WriteFileOptions.class,
				v -> v.hasMembers() || v.hasHashEntries(),
				recordConverter(WriteFileOptions.class));
		builder.targetTypeMapping(Value.class,
				MkdirOptions.class,
				v -> v.hasMembers() || v.hasHashEntries(),
				recordConverter(MkdirOptions.class));
	}

	public JPromise<byte[]> readFile(String path) {
		return JPromise.submitToExecutor(() -> {
			return Files.readAllBytes(Path.of(path));
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<String> readFile(String path, String encoding) {
		var charset = encodingToCharset(encoding);
		return JPromise.submitToExecutor(() -> {
			return Files.readString(Path.of(path), charset);
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<String> readFile(String path, ReadFileOptions options) {
		var encoding = options != null ? options.encoding : null;
		return readFile(path, encoding);
	}

	public JPromise<Void> writeFile(String path, byte[] data) {
		return JPromise.submitToExecutor(() -> {
			Files.write(Path.of(path), data);
			return null;
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<Void> writeFile(String path, String data) {
		return writeFile(path, data, "utf8");
	}

	public JPromise<Void> writeFile(String path, String data, String encoding) {
		var charset = encodingToCharset(encoding);
		return JPromise.submitToExecutor(() -> {
			Files.writeString(Path.of(path), data, charset);
			return null;
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<Void> writeFile(String path, Object data, WriteFileOptions options) {
		var encoding = options != null ? options.encoding : null;
		if (encoding != null && !(data instanceof String)) {
			throw new IllegalArgumentException("Encoding makes no sense with non-string data");
		}
		return data instanceof String s ? writeFile(path, s, encoding) : writeFile(path, (byte[]) data);
	}

	public JPromise<Boolean> mkdir(String path) {
		return mkdir(path, null);
	}

	public JPromise<Boolean> mkdir(String path, MkdirOptions options) {
		var recursive = options != null && options.recursive != null ? options.recursive : false;
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
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	private static Charset encodingToCharset(String encoding) {
		return switch (encoding) {
			case null -> StandardCharsets.UTF_8;
			case "utf8" -> StandardCharsets.UTF_8;
			default -> throw new UnsupportedOperationException("Unsupported encoding: " + encoding);
		};
	}

	/**
	 * @param encoding if non-null, the file will be decoded to a {@link String} using this encoding (note that this
	 *                 uses JavaScript encoding names).
	 */
	public record ReadFileOptions(@OptionalField String encoding) {
	}

	/**
	 * @param encoding if non-null, the data will be encoded from a {@link String} using this encoding (note that this
	 *                 uses JavaScript encoding names). This parameter must be null if the data is not a
	 *                 {@link String}.
	 */
	public record WriteFileOptions(@OptionalField String encoding) {
	}

	/**
	 * @param recursive if {@code true}, parent directories will be created if they do not already exist. If {@code
	 *                  false}, such a case will throw an error.
	 */
	public record MkdirOptions(@OptionalField Boolean recursive) {
	}
}

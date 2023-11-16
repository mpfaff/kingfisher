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
	private static final String ENCODING_UTF8 = "utf8";
	private static final String DEFAULT_ENCODING = ENCODING_UTF8;

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
				WriteBinaryFileOptions.class,
				v -> v.hasMembers() || v.hasHashEntries(),
				recordConverter(WriteBinaryFileOptions.class));
		builder.targetTypeMapping(Value.class,
				WriteStringFileOptions.class,
				v -> v.hasMembers() || v.hasHashEntries(),
				recordConverter(WriteStringFileOptions.class));
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

	public JPromise<?> readFile(String path, String encoding) {
		if (encoding == null) return readFile(path);
		var charset = encodingToCharset(encoding);
		return JPromise.submitToExecutor(() -> {
			return Files.readString(Path.of(path), charset);
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<?> readFile(String path, ReadFileOptions options) {
		var encoding = options != null ? options.encoding : null;
		return readFile(path, encoding);
	}

	public JPromise<Void> writeFile(String path, byte[] data) {
		return writeFile(path, data, null);
	}

	public JPromise<Void> writeFile(String path, String data) {
		return writeFile(path, data, DEFAULT_ENCODING);
	}

	public JPromise<Void> writeFile(String path, String data, String encoding) {
		var charset = encodingToCharset(encoding);
		return JPromise.submitToExecutor(() -> {
			Files.writeString(Path.of(path), data, charset);
			return null;
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<Void> writeFile(String path, byte[] data, WriteBinaryFileOptions options) {
		return JPromise.submitToExecutor(() -> {
			Files.write(Path.of(path), data);
			return null;
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<Void> writeFile(String path, String data, WriteStringFileOptions options) {
		var encoding = options != null && options.encoding != null ? options.encoding : DEFAULT_ENCODING;
		return writeFile(path, data, encoding);
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
			case ENCODING_UTF8 -> StandardCharsets.UTF_8;
			default -> throw new UnsupportedOperationException("Unsupported encoding: " + encoding);
		};
	}

	/**
	 * @param encoding if non-null, the file will be decoded to a {@link String} using this encoding (note that this
	 *                 uses JavaScript encoding names).
	 */
	public record ReadFileOptions(@OptionalField String encoding) {
	}

	public record WriteBinaryFileOptions() {
	}

	/**
	 * @param encoding the name of the encoding to use to encode the data {@link String} (note that this
	 *                 uses JavaScript encoding names). Defaults to {@value DEFAULT_ENCODING}.
	 */
	public record WriteStringFileOptions(@OptionalField String encoding) {
	}

	/**
	 * @param recursive if {@code true}, parent directories will be created if they do not already exist. If {@code
	 *                  false}, such a case will throw an error.
	 */
	public record MkdirOptions(@OptionalField Boolean recursive) {
	}
}
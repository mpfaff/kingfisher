package kingfisher.interop.js;

import kingfisher.interop.OptionalField;
import kingfisher.scripting.EventLoop;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	public JPromise<Boolean> exists(String path) {
		return JPromise.submitToExecutor(() -> Files.exists(Path.of(path)), eventLoop.engine.ioExecutor, eventLoop);
	}

	/**
	 * Equivalent to NodeJS's
	 * <a href="https://nodejs.org/dist/latest-v21.x/docs/api/fs.html#fspromisesreaddirpath-options">{@code readdir}
	 * </a> function.
	 */
	public JPromise<List<String>> readDir(String path) {
		return (JPromise<List<String>>) (JPromise) readDir(path, null);
	}

	/**
	 * Equivalent to NodeJS's
	 * <a href="https://nodejs.org/dist/latest-v21.x/docs/api/fs.html#fspromisesreaddirpath-options">{@code readdir}
	 * </a> function.
	 */
	public JPromise<List<?>> readDir(String pathString, ReadDirOptions options) {
		var path = Path.of(pathString);
		return JPromise.submitToExecutor(() -> {
			boolean recursive = options != null && options.recursive != null && options.recursive;
			if (options != null && options.withFileTypes != null && options.withFileTypes) {
				var list = new ArrayList<JSDirEntry>();
				Files.walkFileTree(path,
						Set.of(),
						recursive ? Integer.MAX_VALUE : 1,
						new SimpleFileVisitor<>() {
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
								if (file != path) list.add(new JSDirEntry(file, attrs.isDirectory(), attrs.isRegularFile(), attrs.isSymbolicLink(), attrs.isOther()));
								return FileVisitResult.CONTINUE;
							}
						});
				return list;
			} else {
				var list = new ArrayList<String>();
				Files.walkFileTree(path,
						Set.of(),
						recursive ? Integer.MAX_VALUE : 1,
						new SimpleFileVisitor<>() {
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
								if (file != path) list.add(file.toString());
								return FileVisitResult.CONTINUE;
							}
						});
				return list;
			}
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<byte[]> readFile(String path) {
		return JPromise.submitToExecutor(() -> {
			return Files.readAllBytes(Path.of(path));
		}, eventLoop.engine.ioExecutor, eventLoop);
	}

	public JPromise<?> readFile(String path, Encoding encoding) {
		if (encoding == null) return readFile(path);
		return JPromise.submitToExecutor(() -> {
			return Files.readString(Path.of(path), encoding.charset);
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
		return writeFile(path, data, Encoding.DEFAULT);
	}

	public JPromise<Void> writeFile(String path, String data, Encoding encoding) {
		return JPromise.submitToExecutor(() -> {
			Files.writeString(Path.of(path), data, encoding.charset);
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
		var encoding = options != null && options.encoding != null ? options.encoding : Encoding.DEFAULT;
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

	/**
	 * @param encoding if non-null, the file will be decoded to a {@link String} using this encoding (note that this
	 *                 uses JavaScript encoding names).
	 */
	public record ReadFileOptions(@OptionalField Encoding encoding) {
	}

	public record WriteBinaryFileOptions() {
	}

	/**
	 * @param encoding the name of the encoding to use to encode the data {@link String} (note that this
	 *                 uses JavaScript encoding names). Defaults to {@link Encoding#DEFAULT}.
	 */
	public record WriteStringFileOptions(@OptionalField Encoding encoding) {
	}

	/**
	 * @param recursive if {@code true}, parent directories will be created if they do not already exist. If {@code
	 *                  false}, such a case will throw an error.
	 */
	public record MkdirOptions(@OptionalField Boolean recursive) {
	}

	/**
	 * @param withFileTypes if {@code true}, entries will be a {@link JSDirEntry} instance instead of a {@link String}.
	 * @param recursive     if {@code true}, the directory will be read recursively.
	 */
	public record ReadDirOptions(@OptionalField Boolean withFileTypes, @OptionalField Boolean recursive) {
	}
}

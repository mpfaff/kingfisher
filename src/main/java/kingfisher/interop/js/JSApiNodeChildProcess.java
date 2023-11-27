package kingfisher.interop.js;

import dev.pfaff.log4truth.Logger;
import kingfisher.interop.OptionalField;
import kingfisher.scripting.ScriptThread;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static kingfisher.interop.ValueUtil.recordConverter;

/**
 * Implements an API similar to the <a href="https://nodejs.org/docs/latest/api/child_process.html">{@code node:fs
 * /child_process}</a> API.
 */
public final class JSApiNodeChildProcess {
	private final ScriptThread thread;

	public JSApiNodeChildProcess(ScriptThread thread) {
		this.thread = thread;
	}

	public static void registerTypes(HostAccess.Builder builder) {
		builder.targetTypeMapping(Value.class,
				ExecOptions.class,
				v -> v.hasMembers() || v.hasHashEntries(),
				recordConverter(ExecOptions.class));
	}

	private Future<Void> asyncPipe(InputStream in,
								   OutputStream out) {
		return thread.engine.ioExecutor.submit(() -> {
			byte[] buf = new byte[4096];
			int n;
			while ((n = in.read(buf)) != -1) {
				out.write(buf, 0, n);
			}
			return null;
		});
	}

	private static Object applyEncoding(ByteArrayOutputStream out, ExecOptions options) {
		var bytes = out.toByteArray();
		if (options != null && options.encoding != null) {
			return options.encoding.decode(bytes);
		} else {
			return bytes;
		}
	}

	/**
	 * Executes the specified file with the provided arguments.
	 *
	 * @see #exec(String, List, ExecOptions)
	 */
	public JPromise<ExecResult> exec(@NonNull String file) {
		return exec(file, List.of());
	}

	/**
	 * Executes the specified file with the provided arguments.
	 *
	 * @see #exec(String, List, ExecOptions)
	 */
	public JPromise<ExecResult> exec(@NonNull String file, @NonNull List<String> args) {
		return exec(file, args, null);
	}

	/**
	 * Executes the specified file with the provided arguments.
	 *
	 * @param options modify the behaviour of this method.
	 */
	public JPromise<ExecResult> exec(@NonNull String file, @NonNull List<String> args, @Nullable ExecOptions options) {
		var builder = new ProcessBuilder().command(Stream.concat(Stream.of(file), args.stream()).toList());
		if (options != null && options.cwd != null) {
			builder.directory(new File(options.cwd));
		}
		if (options != null && options.env != null) {
			builder.environment().clear();
			builder.environment().putAll(options.env);
		}
		return JPromise.submitToExecutor(() -> {
			var proc = builder.start();
			// TODO: replace this with an unsynchronized implementation
			var stdout = new ByteArrayOutputStream();
			var stderr = new ByteArrayOutputStream();
			var a = asyncPipe(proc.getInputStream(), stdout);
			var b = asyncPipe(proc.getErrorStream(), stderr);
			var status = proc.waitFor();
			Logger.log(() -> "child process exited: " + status);
			a.get();
			b.get();
			Logger.log(() -> "output streams closed");
			var result = new ExecResult(status, applyEncoding(stdout, options), applyEncoding(stderr, options));
			Logger.log(() -> "result: " + result);
			return result;
		}, thread.engine.ioExecutor, thread.eventLoop);
	}

	/**
	 * @param encoding if non-null, the child process's output will be decoded to a {@link String} using this encoding
	 *                 (note that this uses JavaScript encoding names).
	 * @param cwd      the current working directory of the new process. If {@code null}, the process will inherit the
	 *                 parent's current working directory.
	 * @param env      the environment for the new process. If {@code null}, the process will inherit the parent's
	 *                 environment.
	 */
	public record ExecOptions(@OptionalField Encoding encoding, @OptionalField String cwd,
							  @OptionalField Map<String, String> env) {
	}

	public static final class ExecResult {
		public final int status;
		public final @NonNull Object stdout;
		public final @NonNull Object stderr;

		public ExecResult(int status, Object stdout, Object stderr) {
			this.status = status;
			this.stdout = stdout;
			this.stderr = stderr;
		}

		@Override
		public String toString() {
			return "ExecResult[" +
					"status=" + status + ", " +
					"stdout=" + stdout + ", " +
					"stderr=" + stderr + ']';
		}

	}
}

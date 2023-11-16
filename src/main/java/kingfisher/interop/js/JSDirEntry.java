package kingfisher.interop.js;

import java.nio.file.Path;

public record JSDirEntry(Path path, boolean isDirectory, boolean isFile, boolean isSymbolicLink, boolean isOther) {
	Path getPath() {
		return path;
	}

	String getName() {
		return path.getFileName().toString();
	}
}

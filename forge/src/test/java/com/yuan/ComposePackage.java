package com.yuan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Reads the Compose GUI package as one string.
 *
 * <p>The config screen used to be a single 4,520-line file ({@code YuanComposeTestScreen.kt}) and the
 * javaChecks asserted against that exact path. It was split into cohesive files — {@code V9Core}
 * (tokens, state holders, section tables), {@code V9Widgets} (leaf controls), {@code V9Panels} (the
 * five pages), {@code V9Chrome} (search/header/tabs/sidebar/preview/footer/dialog) and the entry
 * point — so a path-based assertion now fails for a reason that has nothing to do with the UI being
 * wrong.
 *
 * <p>These assertions are about the UI as a whole, not about a particular file. Reading the package
 * keeps every one of them exactly as strict (a symbol must still exist in the UI source) while
 * removing the accidental coupling to file boundaries, so a future split cannot break them again.
 *
 * <p>Files are concatenated in sorted order so the result is deterministic.
 */
final class ComposePackage {
    private static final Path DIR = Path.of("src/main/kotlin/com/yuan/client/gui");

    private ComposePackage() {
    }

    static String read() throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> stream = Files.list(DIR)) {
            for (Path file : stream.filter(p -> p.toString().endsWith(".kt")).sorted().toList()) {
                sb.append(Files.readString(file)).append('\n');
            }
        }
        return sb.toString();
    }
}

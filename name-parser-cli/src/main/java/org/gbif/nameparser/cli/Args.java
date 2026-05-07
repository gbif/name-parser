package org.gbif.nameparser.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny named-argument parser for CLI commands. Recognises:
 * <ul>
 *   <li>{@code --key=value} long options</li>
 *   <li>{@code --flag} boolean flags (set to {@code "true"})</li>
 *   <li>{@code -h} / {@code --help} aliases (mapped to the {@code help} flag)</li>
 *   <li>everything else is a positional argument, in input order</li>
 * </ul>
 *
 * <p>Intentionally minimal — no nested commands, no required-arg validation, no
 * type coercion beyond {@link #integer(String, int)}. Subcommands implement their
 * own help text so this class stays generic.
 */
public final class Args {
  private final Map<String, String> named = new LinkedHashMap<>();
  private final List<String> positional = new ArrayList<>();

  private Args() {}

  public static Args parse(String[] argv) {
    Args a = new Args();
    for (String s : argv) {
      if ("-h".equals(s) || "--help".equals(s)) {
        a.named.put("help", "true");
      } else if (s.startsWith("--")) {
        int eq = s.indexOf('=');
        if (eq < 0) {
          a.named.put(s.substring(2), "true");
        } else {
          a.named.put(s.substring(2, eq), s.substring(eq + 1));
        }
      } else {
        a.positional.add(s);
      }
    }
    return a;
  }

  public boolean flag(String key) {
    return "true".equalsIgnoreCase(named.get(key));
  }

  public String string(String key, String defaultValue) {
    String v = named.get(key);
    return v == null ? defaultValue : v;
  }

  public int integer(String key, int defaultValue) {
    String v = named.get(key);
    return v == null ? defaultValue : Integer.parseInt(v);
  }

  public Path path(String key, Path defaultValue) {
    String v = named.get(key);
    return v == null ? defaultValue : Paths.get(v);
  }

  public Path positionalPath(int index) {
    return index < positional.size() ? Paths.get(positional.get(index)) : null;
  }

  public List<String> positional() {
    return positional;
  }

  public boolean has(String key) {
    return named.containsKey(key);
  }

  /** True when {@code -h} / {@code --help} was passed, or when no args at all. */
  public boolean wantsHelp() {
    return flag("help");
  }
}

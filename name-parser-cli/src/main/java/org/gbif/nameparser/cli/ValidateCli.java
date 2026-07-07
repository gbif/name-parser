package org.gbif.nameparser.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.gbif.nameparser.NameParserImpl;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.nameparser.cli.io.NameInput;
import org.gbif.nameparser.cli.io.NameInputReader;
import org.gbif.nameparser.cli.llm.AnthropicClient;
import org.gbif.nameparser.cli.llm.Judge;
import org.gbif.nameparser.cli.llm.OpenAiClient;
import org.gbif.nameparser.cli.llm.ValidationPrompt;
import org.gbif.nameparser.cli.llm.Verdict;
import org.gbif.nameparser.cli.llm.VerdictCache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code validate} subcommand — an <b>offline</b> auditing tool that streams a large
 * name corpus through the parser and uses an LLM to flag parses that look wrong, so a
 * human can turn the confirmed ones into new regression tests and rule fixes. It never
 * touches the parser or its production behaviour.
 *
 * <p>Pipeline: stream + parse → select which parses to judge (a seeded, reproducible
 * mix of the suspicious tail plus a random baseline, excluding barcode/OTU codes) →
 * batch the selected results and ask the model to <b>judge</b> each against the
 * parser's documented conventions → write a JSONL report of flagged cases plus a
 * stderr summary. Verdicts are cached by content hash so re-runs are cheap.
 *
 * <p>Because LLM-checking all ~6.3M names is infeasible, coverage is bounded by
 * {@code --budget}; what was excluded or dropped is logged, never silently truncated.
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --input=PATH}      corpus (default {@code data/col-names.tsv})</li>
 *   <li>{@code --output=PATH}     JSONL report (default {@code validate-report.jsonl})</li>
 *   <li>{@code --model=ID}        Claude model (default {@code claude-opus-4-8})</li>
 *   <li>{@code --budget=N}        max names sent to the LLM (default 2000)</li>
 *   <li>{@code --sample-normal=N} of those, how many ordinary names as baseline (default 200)</li>
 *   <li>{@code --batch=N}         names per API request (default 25)</li>
 *   <li>{@code --seed=N}          selection seed for reproducibility (default 17)</li>
 *   <li>{@code --cache=PATH}      verdict cache file, or {@code none} to disable (default {@code validate-cache.jsonl})</li>
 *   <li>{@code --api-url=URL}     Anthropic-Messages-compatible endpoint (also {@code ANTHROPIC_BASE_URL})</li>
 *   <li>{@code --dry-run}         select + build batches but make no API calls</li>
 *   <li>{@code -h --help}         print usage and exit</li>
 * </ul>
 */
public final class ValidateCli {
  static final Path DEFAULT_INPUT = Paths.get("data/col-names.tsv");
  private static final long PROGRESS_EVERY = 500_000;
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  private ValidateCli() {}

  public static void main(String[] argv) throws Exception {
    Args a = Args.parse(argv);
    if (a.wantsHelp()) {
      printUsage();
      return;
    }
    String inputArg = a.string("input", DEFAULT_INPUT.toString());
    Path input = Paths.get(inputArg);
    String provider = a.string("provider", "anthropic").toLowerCase();
    if (provider.equals("local") || provider.equals("ollama")) provider = "openai";
    boolean local = provider.equals("openai");
    String model = a.string("model", null);
    if (model == null) model = local ? "qwen2.5:14b-instruct" : "claude-opus-4-8";
    int budget = a.integer("budget", 2000);
    int sampleNormal = Math.min(a.integer("sample-normal", 200), budget);
    int batchSize = Math.max(1, a.integer("batch", 25));
    long seed = a.integer("seed", 17);
    boolean dryRun = a.flag("dry-run");
    Path output = a.path("output", Paths.get("validate-report.jsonl"));
    String cacheArg = a.string("cache", "validate-cache.jsonl");
    String apiUrl = a.string("api-url", null);

    if (!Files.exists(input)) {
      System.err.println("Input not found: " + input.toAbsolutePath());
      System.err.println("col-names.tsv is a large, gitignored, user-supplied file — drop your "
          + "copy at " + DEFAULT_INPUT + " or pass --input=PATH.");
      System.exit(2);
      return;
    }

    PrintStream log = System.err;
    NameParser parser = new NameParserImpl();

    // -------- Phase 1: stream, parse, select --------
    Selection selection = select(parser, input, budget, sampleNormal, seed, log);
    log.printf("Scanned %,d names in %.1fs: %,d excluded (barcode/OTU), %,d interesting, "
            + "%,d ordinary. Selected %,d for validation (budget %,d).%n",
        selection.total, selection.scanSeconds, selection.excluded,
        selection.interestingSeen, selection.ordinarySeen, selection.chosen.size(), budget);

    // -------- Phase 2: judge + report --------
    VerdictCache cache = "none".equalsIgnoreCase(cacheArg)
        ? VerdictCache.disabled() : VerdictCache.open(Paths.get(cacheArg));
    Judge client = null;
    if (!dryRun) {
      client = local ? OpenAiClient.fromEnv(apiUrl, model) : AnthropicClient.fromEnv(apiUrl, model);
      log.printf("Judging with %s model '%s'%s%n", local ? "local" : "cloud", model,
          apiUrl != null ? " at " + apiUrl : "");
    }

    Summary summary = new Summary();
    int apiCalls = 0;
    int fromCache = 0;
    try (BufferedWriter report = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
      for (int from = 0; from < selection.chosen.size(); from += batchSize) {
        int to = Math.min(from + batchSize, selection.chosen.size());
        List<ParseResult> chunk = selection.chosen.subList(from, to);

        // partition into cache hits and the sub-batch that still needs judging
        List<ParseResult> uncached = new ArrayList<>();
        Map<ParseResult, Verdict> verdicts = new HashMap<>();
        for (ParseResult r : chunk) {
          Verdict cached = cache.get(cacheKey(model, r));
          if (cached != null) {
            verdicts.put(r, cached);
            fromCache++;
          } else {
            uncached.add(r);
          }
        }

        if (!uncached.isEmpty()) {
          if (dryRun) {
            for (ParseResult r : uncached) verdicts.put(r, null); // no verdict in dry-run
          } else {
            List<Verdict> judged = client.judge(ValidationPrompt.userMessage(uncached), uncached.size());
            apiCalls++;
            Map<Integer, Verdict> byIndex = new HashMap<>();
            for (Verdict v : judged) byIndex.put(v.index, v);
            for (int i = 0; i < uncached.size(); i++) {
              ParseResult r = uncached.get(i);
              Verdict v = byIndex.get(i);
              verdicts.put(r, v);
              if (v != null) cache.put(cacheKey(model, r), v);
            }
          }
        }

        for (ParseResult r : chunk) {
          Verdict v = verdicts.get(r);
          report.write(GSON.toJson(reportRow(r, v)));
          report.newLine();
          summary.record(r, v);
        }
        if (!dryRun && !uncached.isEmpty()) {
          log.printf("  judged %,d/%,d  (%,d from cache)%n", to, selection.chosen.size(), fromCache);
        }
      }
    } finally {
      cache.close();
    }

    if (dryRun) {
      log.printf("Dry run: built %,d batches for %,d names, no API calls made. Report → %s%n",
          (selection.chosen.size() + batchSize - 1) / batchSize, selection.chosen.size(),
          output.toAbsolutePath());
      dumpFirstBatch(selection.chosen, batchSize, log);
    } else {
      summary.print(log, apiCalls, fromCache, output);
    }
  }

  // -------------------- selection --------------------

  private static Selection select(NameParser parser, Path input, int budget, int sampleNormal,
                                  long seed, PrintStream log) throws IOException {
    int interestingCap = Math.max(0, budget - sampleNormal);
    Reservoir<ParseResult> interesting = new Reservoir<>(interestingCap, seed);
    Reservoir<ParseResult> ordinary = new Reservoir<>(sampleNormal, seed + 1);
    Selection sel = new Selection();

    long start = System.nanoTime();
    try (NameInputReader reader = NameInputReader.open(input)) {
      NameInput row;
      while ((row = reader.next()) != null) {
        // Barcode/OTU exclusion is a pre-parse regex on the raw input (UNITE SH,
        // BOLD BIN). We deliberately do NOT exclude by NameType.OTHER: OTU codes now
        // fall into OTHER, but so do many genuinely odd unparsable strings that are
        // exactly the tail worth reviewing.
        if (BarcodeOtuFilter.isBarcodeOtu(row.name())) {
          sel.excluded++;
          continue;
        }
        ParseResult r = new ParseResult();
        r.line = row.line();
        r.id = row.id();
        r.input = row.name();
        try {
          r.parsed = parser.parse(row.name(), row.authorship(), row.rank(), row.code());
        } catch (UnparsableNameException e) {
          r.error = new ParseResult.Err(e.getType(), e.getMessage());
        } catch (RuntimeException e) {
          r.error = new ParseResult.Err(null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        sel.total++;
        if (isInteresting(r)) {
          sel.interestingSeen++;
          interesting.offer(r);
        } else {
          sel.ordinarySeen++;
          ordinary.offer(r);
        }
        if (log != null && sel.total % PROGRESS_EVERY == 0) {
          log.printf("  scanned %,d…%n", sel.total);
        }
      }
    }
    sel.scanSeconds = (System.nanoTime() - start) / 1e9;

    List<ParseResult> chosen = new ArrayList<>(interesting.items());
    chosen.addAll(ordinary.items());
    chosen.sort(Comparator.comparingLong(r -> r.line));
    sel.chosen = chosen;
    return sel;
  }

  /**
   * A parse worth a closer look: it failed, carries warnings, is only partially
   * parsed, or is anything other than a plain scientific name. Everything else is
   * "ordinary" and only sampled for a baseline.
   */
  static boolean isInteresting(ParseResult r) {
    if (r.error != null) return true;
    ParsedName pn = r.parsed;
    if (pn == null) return true;
    if (pn.getWarnings() != null && !pn.getWarnings().isEmpty()) return true;
    if (pn.getState() != null && pn.getState() != ParsedName.State.COMPLETE) return true;
    return pn.getType() != null && pn.getType() != NameType.SCIENTIFIC;
  }

  private static String cacheKey(String model, ParseResult r) {
    String shape = r.parsed != null ? GSON.toJson(r.parsed)
        : (r.error == null ? "" : GSON.toJson(r.error));
    return VerdictCache.key(ValidationPrompt.VERSION, model, r.input, shape);
  }

  // -------------------- report row --------------------

  private static JsonObject reportRow(ParseResult r, Verdict v) {
    JsonObject o = new JsonObject();
    o.addProperty("line", r.line);
    o.addProperty("input", r.input);
    if (r.parsed != null) o.add("parsed", GSON.toJsonTree(r.parsed));
    if (r.error != null) o.add("error", GSON.toJsonTree(r.error));
    if (v != null) {
      o.addProperty("verdict", v.verdict);
      o.addProperty("confidence", v.confidence);
      if (v.note != null && !v.note.isBlank()) o.addProperty("note", v.note);
      if (v.fields != null && !v.fields.isEmpty()) {
        o.add("fields", GSON.toJsonTree(v.fields));
      }
    }
    return o;
  }

  private static void dumpFirstBatch(List<ParseResult> chosen, int batchSize, PrintStream log) {
    if (chosen.isEmpty()) return;
    List<ParseResult> first = chosen.subList(0, Math.min(batchSize, chosen.size()));
    log.println();
    log.println("--- first batch payload (dry run) ---");
    log.println(ValidationPrompt.userMessage(first));
  }

  // -------------------- helpers / holders --------------------

  private static final class Selection {
    long total;
    long excluded;
    long interestingSeen;
    long ordinarySeen;
    double scanSeconds;
    List<ParseResult> chosen = new ArrayList<>();
  }

  private static final class Summary {
    int ok, suspect, wrong, missing;
    final Map<String, Integer> byField = new TreeMap<>();

    void record(ParseResult r, Verdict v) {
      if (v == null) {
        missing++;
      } else if ("wrong".equalsIgnoreCase(v.verdict)) {
        wrong++;
      } else if ("suspect".equalsIgnoreCase(v.verdict)) {
        suspect++;
      } else {
        ok++;
      }
      if (v != null && v.fields != null) {
        for (Verdict.FieldIssue f : v.fields) {
          if (f.name != null) byField.merge(f.name, 1, Integer::sum);
        }
      }
    }

    void print(PrintStream log, int apiCalls, int fromCache, Path output) {
      log.println();
      log.printf("Validated %,d names in %,d API call(s), %,d from cache.%n",
          ok + suspect + wrong + missing, apiCalls, fromCache);
      log.printf("  ok=%,d  suspect=%,d  wrong=%,d%s%n",
          ok, suspect, wrong, missing > 0 ? "  (no verdict=" + missing + ")" : "");
      if (!byField.isEmpty()) {
        log.println("Most-flagged fields:");
        byField.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(15)
            .forEach(e -> log.printf("  %-32s %,d%n", e.getKey(), e.getValue()));
      }
      log.println("Report → " + output.toAbsolutePath()
          + "  (review 'verdict' != ok rows; jq '. | select(.verdict!=\"ok\")')");
    }
  }

  private static void printUsage() {
    System.out.println("Usage: name-parser-cli validate [options]");
    System.out.println();
    System.out.println("Offline audit: stream a corpus through the parser and use an LLM to flag");
    System.out.println("parses that look wrong. Never affects parsing behaviour.");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --provider=P       'anthropic' (cloud, default) or 'openai' (local:");
    System.out.println("                     Ollama / LM Studio / llama.cpp). 'local' is an alias.");
    System.out.println("  --input=PATH       corpus (default: data/col-names.tsv)");
    System.out.println("  --output=PATH      JSONL report (default: validate-report.jsonl)");
    System.out.println("  --model=ID         model id (default: claude-opus-4-8, or");
    System.out.println("                     qwen2.5:14b-instruct for --provider=openai)");
    System.out.println("  --budget=N         max names sent to the LLM (default: 2000)");
    System.out.println("  --sample-normal=N  of those, ordinary names as baseline (default: 200)");
    System.out.println("  --batch=N          names per request (default: 25)");
    System.out.println("  --seed=N           selection seed for reproducibility (default: 17)");
    System.out.println("  --cache=PATH       verdict cache, or 'none' (default: validate-cache.jsonl)");
    System.out.println("  --api-url=URL      endpoint override. anthropic: an Anthropic-Messages");
    System.out.println("                     endpoint (ANTHROPIC_BASE_URL). openai: the server root,");
    System.out.println("                     e.g. http://localhost:11434 (Ollama, default),");
    System.out.println("                     http://localhost:1234 (LM Studio), :8080 (llama.cpp).");
    System.out.println("  --dry-run          select + build batches but make no API calls");
    System.out.println("  -h --help          print this message and exit");
    System.out.println();
    System.out.println("Auth (cloud only): ANTHROPIC_API_KEY, or ANTHROPIC_AUTH_TOKEN with a bearer");
    System.out.println("  token (e.g. $(ant auth print-credentials --access-token)). Local needs none.");
    System.out.println();
    System.out.println("Local quick start:  ollama pull qwen2.5:14b-instruct");
    System.out.println("  name-parser-cli validate --provider=local --input=... --budget=100");
    System.out.println();
    System.out.println("UNITE SH and BOLD BIN / OTU codes are excluded automatically.");
  }
}

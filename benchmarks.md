# Benchmarks

results from a macbook pro M4 Pro via IntelliJ:

## Current DEV
```
Parsed names: 4763 (3307 failed)
Total:   482.02 ms
Average: 101.20 µs
Min:     17.92 µs
p50:     55.17 µs
p95:     224.38 µs
Max:     16.59 ms
```

## Step 1 — pre-filters, possessive quantifiers, dedup

8017-name benchmark-data.txt on JDK 17 / M-series mac, median of 3 runs:

| metric  | baseline | step 1  | delta  |
|---------|----------|---------|--------|
| Total   | 809 ms   | 793 ms  | -2.0 % |
| Average | 101.2 µs | 99.0 µs | -2.2 % |
| p50     | 85.3 µs  | 84.5 µs | -1.0 % |
| p95     | 184.7 µs | 180.1 µs| -2.5 % |

Changes: removed duplicate IS_CANDIDATUS_QUOTE_PATTERN call, dropped no-op REPL_ENCLOSING_QUOTE,
added cheap String-contains pre-filters before EXTRACT_SENSU/EXTRACT_NOMSTATUS/REPL_IN_REF/BAD_AUTHORSHIP,
made NORM_WHITESPACE / NORM_BRACKETS_*_STRONG / NO_Q_MARKS quantifiers possessive,
trivial PLACEHOLDER_NAME alternation cleanup.

## Step 1+2 — Step 1 plus outer-EPITHET migration

Median of 5 runs:

| metric  | baseline | step 1+2 | delta  |
|---------|----------|----------|--------|
| Total   | 809 ms   | 804 ms   | -0.6 % |
| Average | 101.2 µs | 100.3 µs | -0.9 % |
| p50     | 85.3 µs  | 85.0 µs  | -0.4 % |
| p95     | 184.7 µs | 180.6 µs | -2.2 % |

Step 2 introduces EPITHET_NO_LOOKBEHIND + the isValidEpithet() Java post-validator at three outer
call sites (STARTING_EPITHET, NORM_LOWERCASE_BINOMIAL, NORM_SUBGENUS). Throughput is roughly flat
vs Step 1 alone — these sites are anchored (^) or rarely matched, so the regex saving and the
post-validation cost cancel. The win is maintainability: three more patterns are now lookbehind-free.

## DEV with smaller patterns
```
Parsed names: 4763 (3308 failed)
Total:   410.24 ms
Average: 86.13 µs
Min:     19.33 µs
p50:     46.96 µs
p95:     199.04 µs
Max:     15.64 ms
```


## Joni
```
Parsed names: 4763 (3308 failed)
Total:   791.99 ms
Average: 166.28 µs
Min:     24.38 µs
p50:     86.29 µs
p95:     372.46 µs
Max:     71.51 ms
```


## ANTLR
```
Parsed names: 4763 (3316 failed)
Total:   494.68 ms
Average: 103.86 µs
Min:     19.04 µs
p50:     49.83 µs
p95:     301.71 µs
Max:     9.76 ms
```

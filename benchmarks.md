# Benchmarks

results from a macbook pro M4 Pro with Liberica JDK17 and the shaded jar

## 4.2.0-SNAPSHOT (2026-07-05)

> benchmark --warmup --input=data/benchmark-data.txt

```
Parsed names: 8017 (3345 unparsable)
Total:   226.47 ms
Average: 28.25 µs
Min:     1.00 µs
p50:     24.96 µs
p95:     74.50 µs
Max:     3.30 ms

Breakdown by name type:
  SCIENTIFIC           4532
  OTHER                3258
  INFORMAL             143
  FORMULA              42
  PLACEHOLDER          42
```

## V4 (2026-05-16)
All classic and GNA tests pass now
More features than dev, still 3-4x faster and no additional threads!

> benchmark --warmup --input=data/benchmark-data.txt

```
Parsed names: 8017 (3362 unparsable)
Total:   298.18 ms
Average: 37.19 µs
Min:     917 ns
p50:     38.50 µs
p95:     91.33 µs
Max:     3.13 ms

Breakdown by name type:
  SCIENTIFIC           4515
  VIRUS                3227
  INFORMAL             143
  OTHER                48
  FORMULA              42
  PLACEHOLDER          42
```

> benchmark --warmup --input=data/col-names.txt

```
Parsed names: 6259108 (24402 unparsable)
Total:   212829.80 ms
Average: 34.00 µs
Min:     1.42 µs
p50:     32.50 µs
p95:     47.21 µs
Max:     5.81 ms

Breakdown by name type:
  SCIENTIFIC           6234255
  VIRUS                21067
  FORMULA              3327
  INFORMAL             451
  PLACEHOLDER          8
```


## V4 (earlier — for reference)
most of classic name parser tests pass. GNA tests largely unsupported still.
+/- on par with dev version.

> benchmark --warmup --input=data/benchmark-data.txt

```
Parsed names: 8017 (3367 unparsable)
Total:   191.70 ms
Average: 23.91 µs
Min:     875 ns
p50:     19.08 µs
p95:     69.25 µs
Max:     1.72 ms

Breakdown by name type:
  SCIENTIFIC           4534
  VIRUS                3233
  INFORMAL             122
  OTHER                49
  FORMULA              42
  PLACEHOLDER          37
```

> benchmark --warmup --input=data/col-names.txt

```
Parsed names: 6259108 (24501 unparsable)
Total:   123868.11 ms
Average: 19.79 µs
Min:     1.46 µs
p50:     18.54 µs
p95:     29.46 µs
Max:     2.62 ms

Breakdown by name type:
  SCIENTIFIC           6234205
  VIRUS                21166
  FORMULA              3327
  INFORMAL             402
  PLACEHOLDER          8
```


## DEV
> benchmark --warmup --input=data/benchmark-data.txt
> Warming up the JIT — parsing the first 100 names without timing…
```
Parsed names: 8017 (3352 unparsable)
Total:   807.70 ms
Average: 100.75 µs
Min:     10.58 µs
p50:     86.29 µs
p95:     182.88 µs
Max:     14.42 ms

Breakdown by name type:
  SCIENTIFIC           4490
  VIRUS                3227
  INFORMAL             152
  PLACEHOLDER          45
  HYBRID_FORMULA       40
  NO_NAME              35
  OTU                  28
```

> benchmark --warmup --input=data/col-names.txt
```
Parsed names: 6259108 (24408 unparsable)
Total:   787905.96 ms
Average: 125.88 µs
Min:     12.63 µs
p50:     97.83 µs
p95:     150.13 µs
Max:     1010.29 ms

Breakdown by name type:
  SCIENTIFIC           6232252
  VIRUS                20954
  HYBRID_FORMULA       3335
  INFORMAL             2506
  NO_NAME              48
  PLACEHOLDER          13
```

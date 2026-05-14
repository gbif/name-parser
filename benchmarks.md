# Benchmarks

results from a macbook pro M4 Pro with Liberica JDK17 and the shaded jar

## V4 (2026-05-14)
> benchmark --warmup --input=data/benchmark-data.txt

```
Parsed names: 8017 (3368 unparsable)
Total:   265.32 ms
Average: 33.09 µs
Min:     917 ns
p50:     32.21 µs
p95:     81.63 µs
Max:     3.16 ms

Breakdown by name type:
  SCIENTIFIC           4515
  VIRUS                3233
  INFORMAL             137
  OTHER                48
  FORMULA              42
  PLACEHOLDER          42
```

> benchmark --warmup --input=data/col-names.txt

```
Parsed names: 6259108 (24501 unparsable)
Total:   180369.95 ms
Average: 28.82 µs
Min:     1.42 µs
p50:     27.50 µs
p95:     40.25 µs
Max:     3.29 ms

Breakdown by name type:
  SCIENTIFIC           6234203
  VIRUS                21166
  FORMULA              3327
  PLACEHOLDER          8
```

## V4 (earlier — for reference)
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

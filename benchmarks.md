# Benchmarks

results from a macbook pro M4 Pro via IntelliJ:

## V4
> benchmark --warmup --input=data/benchmark-data.txt

```
Parsed names: 8017 (3367 unparsable)
Total:   311.02 ms
Average: 38.80 µs
Min:     750 ns
p50:     21.21 µs
p95:     81.67 µs
Max:     87.69 ms

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
```


## DEV
```
Parsed names: 4763 (3308 failed)
Total:   405.72 ms
Average: 85.18 µs
Min:     16.29 µs
p50:     42.54 µs
p95:     208.33 µs
Max:     15.54 ms

Breakdown by name type:
  VIRUS                3250
  SCIENTIFIC           1385
  INFORMAL             70
  NO_NAME              20
  HYBRID_FORMULA       19
  PLACEHOLDER          17
  OTU                  2
```

# Benchmarks

results from a macbook pro M4 Pro via IntelliJ:

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

## V4 pipeline
```
Parsed names: 4763 (3304 failed)
Total:   83.89 ms
Average: 17.61 µs
Min:     833 ns
p50:     4.88 µs
p95:     67.79 µs
Max:     1.33 ms

Breakdown by name type:
  VIRUS                3255
  SCIENTIFIC           1404
  INFORMAL             55
  FORMULA              19
  PLACEHOLDER          19
  OTHER                11
```


## DEV before Claude
```
Parsed names: 4763 (3307 failed)
Total:   482.02 ms
Average: 101.20 µs
Min:     17.92 µs
p50:     55.17 µs
p95:     224.38 µs
Max:     16.59 ms
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



# Full Catalogue of Life Names

## DEV
```
```

## V4 Pipelines
```
Parsed names: 6259059 (25430 failed)
Total:   93358.84 ms
Average: 14.92 µs
Min:     1.33 µs
p50:     14.08 µs
p95:     21.33 µs
Max:     7.90 ms

Breakdown by name type:
SCIENTIFIC           6225136
VIRUS                22094
INFORMAL             8493
FORMULA              3327
PLACEHOLDER          9
```

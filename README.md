# Java Concurrency Benchmarks

Measured comparison of execution strategies for the same workload — sequential, a fixed
platform-thread pool, virtual threads (Java 21), and parallel streams — under two
workload types, plus a demonstration of carrier-thread pinning.

The point is not that one strategy wins. It is that **the right answer changes with the
workload**, and that a single keyword can erase the benefit of virtual threads entirely.

---

## Results

Apple M4 Pro (14 cores) · OpenJDK 21.0.8 · JMH 1.37 · average time, lower is better.
Reproduce with `java -jar target/benchmarks.jar`.

### Waiting work — 100 tasks, 10 ms latency each

| Strategy | Time | vs sequential |
| --- | ---: | ---: |
| `sequential` | 1206.7 ms | 1× |
| `parallelStream` | 95.0 ms | 12.7× |
| `fixedPool(16)` | 83.5 ms | 14.5× |
| **`virtualThreads`** | **12.4 ms** | **97.6×** |

### Computing work — 100 tasks, no blocking

| Strategy | Time | vs sequential |
| --- | ---: | ---: |
| `sequential` | 21.5 ms | 1× |
| `parallelStream` | 2.16 ms | 9.9× |
| `fixedPool(14 = cores)` | 2.20 ms | 9.8× |
| `virtualThreads` | 2.20 ms | 9.8× |

---

## What the two tables say together

**Virtual threads are not faster. They are cheaper to block.**

In the waiting workload they finish 6.7× ahead of a 16-thread pool. In the computing
workload they land on exactly the same number as the pool — 2.20 ms against 2.20 ms.
No gain at all.

The reason is the same in both rows. A virtual thread earns its keep by *unmounting*
from its carrier platform thread while it waits, letting that carrier run something
else. Computing work never waits, so there is no moment to unmount, and the work still
proceeds at the rate the cores allow.

This is worth stating plainly because the common summary — "virtual threads are the fast
ones" — predicts a speedup in the second table that does not exist.

**A fixed pool's size is a concurrency ceiling.** 100 tasks, 16 threads, 10 ms each
cannot finish faster than ⌈100/16⌉ × 10 ms ≈ 70 ms; the measured 83.5 ms is that bound
plus overhead. Raising the ceiling means more OS threads, each with its own stack — which
is exactly the cost virtual threads remove.

**`parallelStream` on waiting work blocks a pool you share with everything else.** It
runs on the JVM-wide common ForkJoinPool. The 95.0 ms above is only this benchmark's
cost; the real price is paid by unrelated code elsewhere in the same JVM that also calls
`parallelStream()` and now waits behind blocked I/O.

---

## Pinning: the same code, 7.7× slower

Same strategy (virtual threads), same work, same 10 ms wait. Every task locks its **own**
lock object, so there is no contention between tasks. The only difference is which lock:

| | Time |
| --- | ---: |
| `reentrantLock` | 12.4 ms |
| `synchronizedBlock` | **94.8 ms** |

A virtual thread that blocks inside a `synchronized` block cannot unmount — the monitor
is tied to the carrier thread, so the virtual thread stays *pinned* to it. Concurrency
collapses from "as many as there are tasks" back to "as many as there are carriers",
which is the core count. Everything virtual threads were adopted for is gone, and nothing
in the code looks wrong.

`ReentrantLock` provides the same mutual exclusion without being tied to the monitor, so
the thread unmounts normally.

### Confirmed on different hardware

CI runs the same benchmarks on a 4-core GitHub runner. The pinning penalty there is not
7.7× but **24×** — 256.1 ms against 10.7 ms — and that number is the mechanism showing
its work:

```
100 tasks × 10 ms ÷ 4 carriers = 250 ms predicted
                                  256 ms measured
```

Pinned virtual threads are limited to the carrier count, so the penalty grows as cores
shrink. The 14-core machine hides most of it; a smaller container exposes it. Code that
looks fine on a developer laptop can behave very differently on a 2-core pod.

**Version caveat:** this measurement is on **Java 21**. [JEP 491](https://openjdk.org/jeps/491),
delivered in JDK 24, removes this pinning for `synchronized`. On a newer JDK the gap
closes. The lesson that survives the version change is the general one: a
runtime-level detail can quietly undo an architectural decision, and only measurement
tells you it happened.

---

## Design

Strategies are interchangeable behind one interface, so the same workload runs under each:

```
workload/     IoBoundWorkload (sleeps) · CpuBoundWorkload (computes)
strategy/     ExecutionStrategy + 4 implementations
report/       ReportAggregator — orchestrates, knows nothing about how work runs
jmh/          benchmarks
```

`ExecutionStrategy` guarantees two things, and the tests enforce them for every
implementation: results come back **in task order**, and each task runs **exactly once**.
Without those guarantees the strategies would not be comparable — the benchmark would be
timing different work, not the same work done differently.

`IoBoundWorkload` sleeps rather than making a real network call. The goal is to measure
what blocking a thread costs, not what a network costs; a real call would add variance
that has nothing to do with the strategies.

---

## Running it

```bash
mvn test
```

```bash
mvn package -DskipTests && java -jar target/benchmarks.jar
```

One benchmark class at a time:

```bash
java -jar target/benchmarks.jar PinningBenchmark
```

Requires JDK 21+. The full run takes a few minutes.

Benchmark numbers are machine-specific. The ratios between strategies are the durable
part; the absolute milliseconds are not.

---

## Tests

26 tests, no infrastructure required.

| Suite | What it covers |
| --- | --- |
| `ExecutionStrategyContractTest` | Order preservation, exactly-once execution, empty input, failure propagation — run against all four strategies |
| `ReportAggregatorTest` | Every strategy produces an identical result for the same workload |
| `VirtualThreadScalingTest` | A small platform pool cannot beat its own arithmetic bound; virtual threads do |

`VirtualThreadScalingTest` asserts on orders of magnitude, not exact timings, so it does
not turn flaky on a loaded machine.

## License

MIT — see [LICENSE](LICENSE).

package connection;


import java.util.concurrent.atomic.AtomicIntegerArray;

public class CounterMap {
    private final AtomicIntegerArray counters;

    public CounterMap(int size) {
        this.counters = new AtomicIntegerArray(size);
    }

    public void increment(int index) {
        counters.incrementAndGet(index);
    }

    public int getAndIncrement(int index) {
        return counters.getAndIncrement(index);
    }

    public void reset(int index) {
        counters.set(index, 0);
    }
    public void resetAll() {
        for (int i = 0; i < counters.length(); i++) {
            counters.set(i, 0);
        }
    }
}



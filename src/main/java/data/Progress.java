package data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Progress {
    long in;
    long out;
    long hash;
    int size;
    int word;
    long duration;
    LocalDateTime time;
    public static String PROGRESS_BAR_STRING = "[ → | ← | Size| Words | Duration | Time | # ]";

    public Progress(long in, long out, long hash, LocalDateTime time) {
        this.in = in;
        this.out = out;
        this.hash = hash;
        this.time = time;
    }

    public Progress(long in, long out, long hash, int size, int word, long duration, LocalDateTime time) {
        this.in = in;
        this.out = out;
        this.hash = hash;
        this.size = size;
        this.word = word;
        this.duration = duration;
        this.time = time;
    }

    public void setIn(long in) {
        this.in = in;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "[ " +
                String.format("→ %-5d", in) +
                String.format("| ← %-5d", out) +
                String.format("| Size: %-5d", size) +
                String.format("| Words: %-5d", word) +
                String.format("| Duration: %-5dms", duration) +
                "| Time: " + time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SS dd LLL yyyy")) +
                String.format("| # %-5d", hash) +
                ']';
    }
}

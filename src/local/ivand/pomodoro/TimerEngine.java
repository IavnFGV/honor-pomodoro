package local.ivand.pomodoro;

/** Monotonic timer. Owned by the service main thread; no Android dependencies. */
public final class TimerEngine {
    public int workMinutes = 25, shortMinutes = 5, longMinutes = 15;
    public int completed = 0;
    public boolean work = true, running = false;
    public long remaining = 25 * 60000L, deadline;
    public long duration() { return (work ? workMinutes : completed > 0 && completed % 4 == 0 ? longMinutes : shortMinutes) * 60000L; }
    public long left(long now) { return running ? Math.max(0, deadline - now) : remaining; }
    public void start(long now) { if (!running) { if (remaining <= 0) remaining = duration(); deadline = now + remaining; running = true; } }
    public void pause(long now) { remaining = left(now); running = false; }
    public void reset() { running = false; remaining = duration(); }
    public void select(boolean isWork) { work = isWork; reset(); }
    public boolean tick(long now) {
        if (!running || now < deadline) return false;
        if (work) completed++;
        work = !work;
        running = false;
        remaining = duration();
        return true;
    }
}

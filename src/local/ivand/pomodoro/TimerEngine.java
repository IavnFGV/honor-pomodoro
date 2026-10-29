package local.ivand.pomodoro;

/** Monotonic timer. Owned by the service main thread; no Android dependencies. */
public final class TimerEngine {
    public int workMinutes = 25, shortMinutes = 5, longMinutes = 15;
    public int completed = 0;
    public int cycleCompleted = 0, longEvery=4;
    public boolean work = true, running = false;
    public long remaining = 25 * 60000L, deadline;
    public long oneOffMillis;
    public long duration() { return oneOffMillis>0?oneOffMillis:(work ? workMinutes : cycleCompleted > 0 && cycleCompleted % longEvery == 0 ? longMinutes : shortMinutes) * 60000L; }
    public void setNextMinutes(int minutes){if(running||minutes<1||minutes>(work?120:60))throw new IllegalArgumentException("Invalid next interval");oneOffMillis=minutes*60000L;remaining=oneOffMillis;}
    public void changeCurrentMinutes(int minutes,long now){
        if(minutes<1||minutes>(work?120:60))throw new IllegalArgumentException("Invalid current interval");
        oneOffMillis=minutes*60000L;remaining=oneOffMillis;
        if(running)deadline=now+remaining;
    }
    public long left(long now) { return running ? Math.max(0, deadline - now) : remaining; }
    public void start(long now) { if (!running) { if (remaining <= 0) remaining = duration(); deadline = now + remaining; running = true; } }
    public void pause(long now) { remaining = left(now); running = false; }
    public void reset() { running = false; remaining = duration(); }
    public void select(boolean isWork) { work = isWork; oneOffMillis=0; reset(); }
    public boolean tick(long now) {
        if (!running || now < deadline) return false;
        if (work){completed++;cycleCompleted++;}
        work = !work;
        oneOffMillis=0;
        running = false;
        remaining = duration();
        return true;
    }
}

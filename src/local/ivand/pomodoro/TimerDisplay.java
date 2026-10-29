package local.ivand.pomodoro;

/** UI must never rebase a running countdown to the delivery time of a snapshot. */
public final class TimerDisplay {
    public static long remaining(boolean running,long pausedLeft,long deadline,long now){return running?Math.max(0,deadline-now):pausedLeft;}
}

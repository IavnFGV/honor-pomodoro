import local.ivand.pomodoro.TimerDisplay;
public final class TimerDisplayTest {
    static void check(long got,long expected){if(got!=expected)throw new AssertionError(got+" != "+expected);}
    public static void main(String[] args){
        check(TimerDisplay.remaining(true,20000,100000,95000),5000);
        check(TimerDisplay.remaining(true,18000,100000,96000),4000);
        check(TimerDisplay.remaining(true,17000,100000,110000),0);
        check(TimerDisplay.remaining(false,20000,100000,999999),20000);
        System.out.println("TimerDisplay: delayed snapshots cannot rebase or accelerate the countdown; pause/expiry PASS");
    }
}

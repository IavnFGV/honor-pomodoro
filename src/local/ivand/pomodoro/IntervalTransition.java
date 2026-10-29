package local.ivand.pomodoro;

/** Service-main-thread policy; recovery never restores a pending automatic decision. */
public final class IntervalTransition {
    public static final int NONE=0, REST=1, WORK=2;
    public int kind;
    public long deadline;
    public void begin(boolean finishedWork,long now){begin(finishedWork,now,false,true);}
    public void begin(boolean finishedWork,long now,boolean autoWork,boolean autoRest){kind=finishedWork?REST:WORK;deadline=(finishedWork?autoRest:autoWork)?now+10000:0;}
    public void edit(){deadline=0;}
    public void cancel(){kind=NONE;deadline=0;}
    public boolean expire(TimerEngine timer,long now){if(kind==NONE||deadline==0||now<deadline)return false;cancel();timer.start(now);return true;}
    public boolean answer(TimerEngine timer,boolean yes,long now){if(kind==NONE)return false;if(yes)timer.start(now);cancel();return true;}
    public int seconds(long now){return deadline>0?(int)Math.max(0,(deadline-now+999)/1000):0;}
}

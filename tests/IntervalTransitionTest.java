import local.ivand.pomodoro.TimerEngine;
import local.ivand.pomodoro.IntervalTransition;
public final class IntervalTransitionTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        TimerEngine t=new TimerEngine();IntervalTransition x=new IntervalTransition();
        t.start(0);check(t.tick(1500000));x.begin(true,1500000);
        check(x.seconds(1500000)==10&&!x.expire(t,1509999)&&!t.running);
        check(x.expire(t,1510000)&&t.running&&!t.work&&x.kind==0);
        check(!x.expire(t,1510001));
        t.pause(1511000);x.cancel();check(!x.expire(t,Long.MAX_VALUE)&&!t.running);
        t.select(false);x.begin(true,0);check(x.answer(t,false,1000));check(!x.expire(t,20000)&&!t.running);
        x.begin(true,0);check(x.answer(t,true,1000)&&t.running&&!t.work);
        check(t.tick(301000));x.begin(false,301000);check(t.work&&!t.running&&x.kind==IntervalTransition.WORK);
        check(!x.expire(t,Long.MAX_VALUE)&&!t.running);check(x.answer(t,true,400000)&&t.running&&t.work);
        t.completed=4;t.cycleCompleted=4;t.select(false);check(t.remaining==900000);x.begin(true,0);check(x.expire(t,10000)&&t.left(10000)==900000);
        IntervalTransition recovered=new IntervalTransition();check(!recovered.expire(t,Long.MAX_VALUE));
        t.select(true);x.begin(false,1000,true,true);check(!x.expire(t,10999)&&x.expire(t,11000)&&t.work&&t.running);
        t.select(false);x.begin(true,1000,false,false);check(!x.expire(t,Long.MAX_VALUE)&&!t.running);
        x.begin(true,0,false,true);x.edit();t.setNextMinutes(10);check(!x.expire(t,20000)&&!t.running);
        check(x.answer(t,true,21000)&&t.running&&t.left(21000)==600000&&t.shortMinutes==5);
        check(t.tick(621000)&&t.work&&t.duration()==1500000);t.select(false);check(t.duration()==900000);
        t.completed=1;t.cycleCompleted=1;t.select(false);check(t.duration()==300000);
        boolean rejected=false;try{t.setNextMinutes(61);}catch(IllegalArgumentException expected){rejected=true;}check(rejected);
        t.select(true);x.begin(false,0,true,true);x.edit();check(!x.expire(t,20000));
        x.begin(false,30000,true,true);check(x.expire(t,40000)&&t.running);
        System.out.println("IntervalTransition: timeout boundary, yes/no, manual pause, long rest, no auto-work, recovery PASS");
    }
}

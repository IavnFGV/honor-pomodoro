import local.ivand.pomodoro.TimerEngine;
public final class TimerEngineTest {
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        TimerEngine t = new TimerEngine();
        t.start(1000); check(t.left(61000) == 24*60000L);
        t.pause(61000); check(t.left(90000) == 24*60000L);
        t.start(100000); check(!t.tick(100001));
        check(t.tick(100000+24*60000L)); check(t.completed == 1 && !t.work && !t.running);
        check(!t.tick(Long.MAX_VALUE)); check(t.completed == 1);
        for(int i=1; i<4; i++) { t.select(true); t.start(0); check(t.tick(25*60000L)); }
        check(t.completed == 4 && t.remaining == 15*60000L);
        t.start(0); check(t.tick(15*60000L)); check(t.work && t.completed == 4);
        t.start(0); t.reset(); check(!t.running && t.remaining == 25*60000L);
        t.completed=0;t.select(true);t.start(1000);t.changeCurrentMinutes(15,301000);
        check(t.running&&t.left(301000)==900000&&t.deadline==1201000&&t.workMinutes==25&&t.completed==0);
        check(!t.tick(1200999)&&t.tick(1201000)&&t.completed==1&&!t.work&&t.duration()==300000);
        t.select(true);t.changeCurrentMinutes(10,999999);check(!t.running&&t.remaining==600000);
        t.start(1000000);check(t.deadline==1600000);
        boolean rejected=false;try{t.changeCurrentMinutes(0,1000000);}catch(IllegalArgumentException expected){rejected=true;}check(rejected&&t.deadline==1600000);
        System.out.println("TimerEngine: pause/resume, deadline, exactly-once completion, fourth long break PASS");
    }
}

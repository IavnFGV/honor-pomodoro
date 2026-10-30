import local.ivand.pomodoro.PomodoroPreset;
import local.ivand.pomodoro.TimerEngine;
public final class PomodoroPresetTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        TimerEngine t=new TimerEngine();t.cycleCompleted=7;t.start(0);t.changeCurrentMinutes(10,1000);
        PomodoroPreset deep=PomodoroPreset.defaults()[2];deep.apply(t);
        check(t.cycleCompleted==0&&!t.running&&t.work&&t.duration()==3000000&&t.oneOffMillis==0);
        t.start(0);check(t.tick(3000000));check(!t.work&&t.duration()==600000);
        t.start(0);check(t.tick(600000));check(t.work);
        t.start(0);check(t.tick(3000000));check(t.cycleCompleted==2&&t.duration()==1200000);
        PomodoroPreset custom=new PomodoroPreset("custom-test","Мой режим",35,7,18,3,false);custom.apply(t);
        for(int i=0;i<3;i++){t.select(true);t.start(0);check(t.tick(2100000));check(t.duration()==(i==2?1080000:420000));}
        check(custom.work==35&&custom.every==3);
        t.changeCurrentMinutes(2,0);check(t.duration()==120000&&custom.longRest==18);t.select(true);check(t.duration()==2100000);
        boolean invalid=false;try{new PomodoroPreset("custom-bad","  ",25,5,15,4,false);}catch(IllegalArgumentException e){invalid=true;}check(invalid);
        invalid=false;try{new PomodoroPreset("custom-bad","Bad",25,5,15,0,false);}catch(IllegalArgumentException e){invalid=true;}check(invalid);
        invalid=false;try{new PomodoroPreset("custom-bad","Bad",25,5,15,9,false);}catch(IllegalArgumentException e){invalid=true;}check(invalid);
        System.out.println("PomodoroPreset: 2/3-work cycles, apply/reset, one-off isolation, validation PASS");
    }
}

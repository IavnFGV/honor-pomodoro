package local.ivand.pomodoro;

/** Immutable saved configuration; one-off spoken durations never modify it. */
public final class PomodoroPreset {
    public final String id,name;
    public final int work,rest,longRest,every;
    public final boolean builtIn;
    public PomodoroPreset(String id,String name,int work,int rest,int longRest,int every,boolean builtIn){
        if(id==null||id.trim().isEmpty()||name==null||name.trim().isEmpty()||name.trim().length()>32||name.contains("\n")||name.contains("\r"))throw new IllegalArgumentException("Название: от 1 до 32 символов");
        if(work<1||work>120||rest<1||rest>60||longRest<1||longRest>60||every<1||every>8)throw new IllegalArgumentException("Проверьте длительности и длину цикла");
        this.id=id;this.name=name.trim();this.work=work;this.rest=rest;this.longRest=longRest;this.every=every;this.builtIn=builtIn;
    }
    public static PomodoroPreset[] defaults(){return new PomodoroPreset[]{
        new PomodoroPreset("builtin-light","Лёгкий старт",15,5,15,4,true),
        new PomodoroPreset("builtin-classic","Классика",25,5,15,4,true),
        new PomodoroPreset("builtin-deep","Глубокая работа",50,10,20,2,true)
    };}
    public void apply(TimerEngine timer){
        timer.workMinutes=work;timer.shortMinutes=rest;timer.longMinutes=longRest;timer.longEvery=every;
        timer.cycleCompleted=0;timer.select(true);
    }
    public String summary(){return work+" / "+rest+" / "+longRest+" мин · после "+every+" работ";}
}

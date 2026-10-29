package local.ivand.pomodoro;

/** Russian remaining-time phrase, rounded up to the next second. */
public final class SpokenTime {
    private static String noun(long n,String one,String few,String many){long v=n%100;return v>=11&&v<=14?many:n%10==1?one:n%10>=2&&n%10<=4?few:many;}
    public static String format(long millis,boolean running){
        long total=(Math.max(0,millis)+999)/1000;
        if(total==0)return "Интервал завершён.";
        long minutes=total/60,seconds=total%60,first=minutes>0?minutes:seconds;
        StringBuilder phrase=new StringBuilder(first%10==1&&first%100!=11?"Осталась ":"Осталось ");
        if(minutes>0)phrase.append(minutes).append(' ').append(noun(minutes,"минута","минуты","минут"));
        if(seconds>0){if(minutes>0)phrase.append(' ');phrase.append(seconds).append(' ').append(noun(seconds,"секунда","секунды","секунд"));}
        phrase.append('.');if(!running)phrase.append(" Таймер на паузе.");return phrase.toString();
    }
}

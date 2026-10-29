package local.ivand.pomodoro;

import java.util.Locale;
/** Restricted whole-minute commands; no free-form arithmetic or seconds. */
public final class DurationCommand {
    private static final String[] SMALL={"","один","два","три","четыре","пять","шесть","семь","восемь","девять","десять","одиннадцать","двенадцать","тринадцать","четырнадцать","пятнадцать","шестнадцать","семнадцать","восемнадцать","девятнадцать"};
    private static final String[] TENS={"","","двадцать","тридцать","сорок","пятьдесят","шестьдесят","семьдесят","восемьдесят","девяносто"};
    public static String number(int n){if(n==100)return "сто";if(n>100)return "сто "+number(n-100);if(n<20)return SMALL[n];return TENS[n/10]+(n%10==0?"":" "+SMALL[n%10]);}
    public static String spoken(int n){return number(n).replaceAll("\\bодин$","одну").replaceAll("\\bдва$","две");}
    public static String unit(int n){return n%100>=11&&n%100<=14?"минут":n%10==1?"минуту":n%10>=2&&n%10<=4?"минуты":"минут";}
    public static boolean editing(String text){return text.matches("^(поменяй|сделай|измени|изменить|поставь|установи)( .*)?$");}
    public static int parse(String text){
        String s=text.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
        s=s.replaceFirst("^(поменяй|сделай|измени|изменить|поставь|установи)( интервал| время)?( на)? ","").replaceFirst("^на ","");
        if(!s.matches(".+ (минут|минуту|минуты|минута)"))return -1;
        s=s.replaceFirst(" (минут|минуту|минуты|минута)$","").replaceAll("\\b(одну|одна)\\b","один").replaceAll("\\bдве\\b","два");
        for(int n=1;n<=120;n++)if(s.equals(number(n))||s.equals(Integer.toString(n)))return n;
        return -1;
    }
}

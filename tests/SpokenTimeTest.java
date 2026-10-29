import local.ivand.pomodoro.SpokenTime;
public final class SpokenTimeTest {
    static void expect(long ms,boolean running,String text){String got=SpokenTime.format(ms,running);if(!got.equals(text))throw new AssertionError(got+" != "+text);}
    public static void main(String[] args){
        expect(0,true,"Интервал завершён.");expect(1,true,"Осталась 1 секунда.");
        expect(22000,true,"Осталось 22 секунды.");expect(11000,true,"Осталось 11 секунд.");
        expect(60000,true,"Осталась 1 минута.");expect(125000,true,"Осталось 2 минуты 5 секунд.");
        expect(1260000,false,"Осталась 21 минута. Таймер на паузе.");expect(7200000,true,"Осталось 120 минут.");
        System.out.println("SpokenTime: minute/second plurals, rounding, paused/expired states PASS");
    }
}

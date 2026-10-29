import local.ivand.pomodoro.DurationCommand;
public final class DurationCommandTest {
    static void expect(String s,int value){int got=DurationCommand.parse(s);if(got!=value)throw new AssertionError(s+" => "+got);}
    public static void main(String[] args){
        expect("поменяй интервал на десять минут",10);expect("измени интервал на двадцать одну минуту",21);
        expect("сделай интервал на пятнадцать минут",15);expect("сделай интервал десять минут",10);
        expect("на две минуты",2);expect("поставь тридцать пять минут",35);expect("изменить время на сто двадцать минут",120);
        expect("на 10 минут",10);expect("на ноль минут",-1);expect("на сто двадцать одну минуту",-1);
        expect("на минус десять минут",-1);
        expect("на десять секунд",-1);expect("не меняй на десять минут",-1);expect("поменяй интервал",-1);
        for(int i=1;i<=120;i++)expect("поменяй интервал на "+DurationCommand.spoken(i)+" "+DurationCommand.unit(i),i);
        if(!DurationCommand.editing("поменяй интервал")||!DurationCommand.editing("сделай интервал")||DurationCommand.editing("не поменяй интервал"))throw new AssertionError();
        System.out.println("DurationCommand: Russian 1..120, minute forms, invalid/negated/seconds rejection PASS");
    }
}

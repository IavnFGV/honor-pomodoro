package local.ivand.pomodoro;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Compact landscape settings using real Android controls and the timer palette. */
final class SettingsPanel {
    interface Actions {void action(String name);void voice();void configure(int w,int s,int l);void exit();}
    private final Activity activity;
    private final Actions actions;
    private final int ink=0xfff3f1e2,muted=0xff96a4a0,card=0xff202b2a,button=0xff30413b,accent=0xfff18770;
    private int dp(float n){return (int)(n*activity.getResources().getDisplayMetrics().density+.5f);}
    SettingsPanel(Activity a,Actions callback){activity=a;actions=callback;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String label,int size,int color){TextView t=new TextView(activity);t.setText(label);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);t.setFontFeatureSettings("tnum");return t;}
    private Button button(String label,int color,Runnable click){Button b=new Button(activity);b.setText(label);b.setTextSize(13);b.setTextColor(color);b.setAllCaps(false);b.setPadding(dp(8),0,dp(8),0);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setBackground(shape(button,12));b.setOnClickListener(v->click.run());return b;}
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(1);return v;}
    private void gap(LinearLayout v,int size){View space=new View(activity);v.addView(space,new LinearLayout.LayoutParams(1,dp(size)));}
    private Dialog dialog(String title,LinearLayout body){
        Dialog d=new Dialog(activity);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=column();panel.setPadding(dp(20),dp(14),dp(20),dp(16));panel.setBackground(shape(card,22));
        LinearLayout heading=new LinearLayout(activity);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=text(title,22,ink);name.setTypeface(null,Typeface.BOLD);heading.addView(name,new LinearLayout.LayoutParams(0,dp(42),1));
        Button close=button("×",muted,()->d.dismiss());close.setTextSize(25);close.setContentDescription("Закрыть настройки");heading.addView(close,new LinearLayout.LayoutParams(dp(42),dp(38)));
        panel.addView(heading);gap(panel,10);panel.addView(body,new LinearLayout.LayoutParams(-1,0,1));d.setContentView(panel);
        Window w=d.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setDimAmount(.65f);w.getDecorView().setSystemUiVisibility(5894);
        int width=Math.min(dp(596),activity.getResources().getDisplayMetrics().widthPixels-dp(24));
        int height=Math.min(dp(328),activity.getResources().getDisplayMetrics().heightPixels-dp(24));
        w.setLayout(width,height);d.setOnDismissListener(x->activity.getWindow().getDecorView().setSystemUiVisibility(5894));return d;
    }
    private int[] stepper(LinearLayout parent,String label,int initial,int max){
        int[] value={initial};LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(label,14,ink),new LinearLayout.LayoutParams(0,dp(44),1));
        TextView number=text(""+initial,19,ink);number.setGravity(Gravity.CENTER);number.setContentDescription(label+": "+initial+" минут");
        Button minus=button("−",ink,()->{value[0]=Math.max(1,value[0]-1);number.setText(""+value[0]);number.setContentDescription(label+": "+value[0]+" минут");});
        Button plus=button("+",ink,()->{value[0]=Math.min(max,value[0]+1);number.setText(""+value[0]);number.setContentDescription(label+": "+value[0]+" минут");});
        minus.setContentDescription("Уменьшить: "+label);plus.setContentDescription("Увеличить: "+label);
        row.addView(minus,new LinearLayout.LayoutParams(dp(36),dp(36)));row.addView(number,new LinearLayout.LayoutParams(dp(40),dp(40)));row.addView(plus,new LinearLayout.LayoutParams(dp(36),dp(36)));
        parent.addView(row);return value;
    }
    void show(boolean screenOn,boolean voiceOn){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(0);
        LinearLayout left=column(),right=column();content.addView(left,new LinearLayout.LayoutParams(0,-1,1));View divider=new View(activity);divider.setBackgroundColor(0xff394841);LinearLayout.LayoutParams div=new LinearLayout.LayoutParams(dp(1),-1);div.setMargins(dp(18),0,dp(18),0);content.addView(divider,div);content.addView(right,new LinearLayout.LayoutParams(0,-1,1));
        left.addView(text("ИНТЕРВАЛЫ · МИНУТЫ",10,muted),new LinearLayout.LayoutParams(-1,dp(22)));
        android.content.SharedPreferences p=activity.getSharedPreferences("timer",0);
        int[] work=stepper(left,"Работа",p.getInt("work",25),120),rest=stepper(left,"Перерыв",p.getInt("short",5),60),longRest=stepper(left,"Длинный",p.getInt("long",15),60);
        gap(left,8);
        Dialog d=dialog("Настройки",content);
        Button save=button("Применить интервалы",0xff141b1c,()->{actions.configure(work[0],rest[0],longRest[0]);d.dismiss();});save.setBackground(shape(accent,12));left.addView(save,new LinearLayout.LayoutParams(-1,dp(38)));
        left.addView(text("Сбросит текущий таймер",10,muted),new LinearLayout.LayoutParams(-1,dp(24)));
        Switch screen=new Switch(activity);screen.setText("Не гасить экран");screen.setTextSize(14);screen.setTextColor(ink);screen.setChecked(screenOn);screen.setButtonTintList(null);screen.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xffa0cda4,0xff82908a}));screen.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff506657,0xff37433f}));screen.setOnCheckedChangeListener((b,on)->actions.action("screen"));right.addView(screen,new LinearLayout.LayoutParams(-1,dp(44)));
        Switch mic=new Switch(activity);mic.setText("Голос «Помидорка»");mic.setTextSize(14);mic.setTextColor(ink);mic.setChecked(voiceOn);mic.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xffa0cda4,0xff82908a}));mic.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff506657,0xff37433f}));mic.setOnCheckedChangeListener((b,on)->{actions.voice();d.dismiss();});right.addView(mic,new LinearLayout.LayoutParams(-1,dp(44)));
        gap(right,5);right.addView(button("Голосовые команды   ›",ink,()->{d.dismiss();help();}),new LinearLayout.LayoutParams(-1,dp(38)));
        gap(right,6);right.addView(button("Проверить индикатор   ›",muted,()->{d.dismiss();led();}),new LinearLayout.LayoutParams(-1,dp(38)));
        gap(right,6);right.addView(button("Завершить работу приложения",muted,()->{d.dismiss();actions.exit();}),new LinearLayout.LayoutParams(-1,dp(34)));
        d.show();d.getWindow().setLayout(Math.min(dp(596),activity.getResources().getDisplayMetrics().widthPixels-dp(24)),Math.min(dp(328),activity.getResources().getDisplayMetrics().heightPixels-dp(24)));
    }
    private void help(){LinearLayout body=column();ScrollView scroll=new ScrollView(activity);TextView copy=text("Скажите «Помидорка», затем после сигнала — команду. Можно одной фразой: «Помидорка, пауза».\n\nНачать работу · Пауза · Продолжить\nНачать перерыв · Сколько осталось\nОстановить таймер → «Помидорка, подтверждаю»\n\nМикрофон работает локально. Записи не сохраняются. После перезапуска голос включается вручную.",15,ink);copy.setGravity(Gravity.TOP);copy.setLineSpacing(dp(4),1);scroll.addView(copy);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));Dialog d=dialog("Голосовые команды",body);d.show();d.getWindow().setLayout(dp(596),dp(328));}
    private void led(){LinearLayout body=column();TextView note=text("Экспериментальная функция. Выберите цвет и погасите экран кнопкой питания. При зарядке система может удерживать зелёный свет.",15,muted);note.setGravity(Gravity.TOP);body.addView(note,new LinearLayout.LayoutParams(-1,dp(78)));
        LinearLayout row=new LinearLayout(activity);Button red=button("Красный",accent,()->actions.action("red")),green=button("Зелёный",0xffa0cda4,()->actions.action("green"));row.addView(red,new LinearLayout.LayoutParams(0,dp(48),1));View space=new View(activity);row.addView(space,new LinearLayout.LayoutParams(dp(12),1));row.addView(green,new LinearLayout.LayoutParams(0,dp(48),1));body.addView(row);gap(body,12);body.addView(button("Снять световое уведомление",ink,()->actions.action("clearLed")),new LinearLayout.LayoutParams(-1,dp(42)));Dialog d=dialog("Индикатор телефона",body);d.show();d.getWindow().setLayout(dp(596),dp(328));}
}

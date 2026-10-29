package local.ivand.pomodoro;

import android.app.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Next-stage editor. The service owns the deadline and selected one-off duration. */
final class TransitionDialog extends Dialog {
    interface Actions {void accept();void reject();void edit();void minutes(int value);}
    private final int kind,accent;
    private final Actions actions;
    private final TextView number,detail,hint;
    private int minutes;
    private boolean touched;
    private final int ink=0xfff3f1e2,muted=0xffa0b1a8;
    private int dp(int v){return (int)(v*getContext().getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private Button button(String label,int color,int background,Runnable click){Button b=new Button(getContext());b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(color);b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setBackground(shape(background,14));b.setOnClickListener(v->click.run());return b;}
    private void beginEdit(){if(!touched){touched=true;actions.edit();}detail.setText("Автостарт остановлен.\nНачните этап, когда будете готовы.");}
    private void choose(int value){beginEdit();minutes=Math.max(1,Math.min(kind==IntervalTransition.WORK?120:60,value));number.setText(Integer.toString(minutes));actions.minutes(minutes);}
    private void editable(Button button){button.setOnTouchListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_DOWN)beginEdit();return false;});}
    TransitionDialog(Activity owner,int type,Actions callback){
        super(owner);kind=type;actions=callback;accent=type==IntervalTransition.REST?0xffa0cda4:0xfff18770;
        requestWindowFeature(Window.FEATURE_NO_TITLE);setCancelable(false);
        LinearLayout panel=new LinearLayout(owner);panel.setOrientation(1);panel.setPadding(dp(24),dp(20),dp(24),dp(20));panel.setBackground(shape(0xff202b2a,24));
        panel.addView(text("СЛЕДУЮЩИЙ ЭТАП",10,muted));
        TextView title=text(type==IntervalTransition.REST?"Время отдохнуть":"Готов к работе?",27,ink);title.setTypeface(null,Typeface.BOLD);LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(-1,dp(42));titleLp.bottomMargin=dp(12);panel.addView(title,titleLp);
        LinearLayout body=new LinearLayout(owner);body.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout duration=new LinearLayout(owner);duration.setOrientation(1);duration.setGravity(Gravity.CENTER);
        LinearLayout step=new LinearLayout(owner);step.setGravity(Gravity.CENTER_VERTICAL);
        Button minus=button("−",ink,0xff34443c,()->choose(minutes-1));minus.setContentDescription("Уменьшить длительность");editable(minus);
        number=text("",48,accent);number.setGravity(Gravity.CENTER);number.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));number.setFontFeatureSettings("tnum");
        Button plus=button("+",ink,0xff34443c,()->choose(minutes+1));plus.setContentDescription("Увеличить длительность");editable(plus);
        step.addView(minus,new LinearLayout.LayoutParams(dp(42),dp(44)));step.addView(number,new LinearLayout.LayoutParams(dp(104),dp(64)));step.addView(plus,new LinearLayout.LayoutParams(dp(42),dp(44)));duration.addView(step);duration.addView(text("минут · только этот раз",11,muted));
        LinearLayout presets=new LinearLayout(owner);int[] choices=type==IntervalTransition.REST?new int[]{5,10,15}:new int[]{15,25,50};
        for(int value:choices){Button b=button(Integer.toString(value),ink,0xff2c3934,()->choose(value));editable(b);b.setContentDescription("Выбрать "+value+" минут");LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(57),dp(34));lp.setMargins(dp(3),dp(10),dp(3),0);presets.addView(b,lp);}duration.addView(presets);
        body.addView(duration,new LinearLayout.LayoutParams(dp(206),dp(135)));
        LinearLayout info=new LinearLayout(owner);info.setOrientation(1);info.setPadding(dp(24),0,0,0);
        detail=text("",16,ink);info.addView(detail,new LinearLayout.LayoutParams(-1,dp(62)));
        hint=text("",12,muted);hint.setLineSpacing(dp(3),1);info.addView(hint);body.addView(info,new LinearLayout.LayoutParams(0,-2,1));panel.addView(body,new LinearLayout.LayoutParams(-1,dp(142)));
        LinearLayout buttons=new LinearLayout(owner);Button yes=button(type==IntervalTransition.REST?"Начать отдых":"Начать работу",0xff141b1c,accent,()->actions.accept());yes.setTypeface(null,Typeface.BOLD);buttons.addView(yes,new LinearLayout.LayoutParams(0,dp(46),1));
        Button no=button("Позже",ink,0xff34443c,()->actions.reject());LinearLayout.LayoutParams noLp=new LinearLayout.LayoutParams(dp(150),dp(46));noLp.leftMargin=dp(12);buttons.addView(no,noLp);panel.addView(buttons);
        setContentView(panel);getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_FULLSCREEN);immersive();
    }
    int kind(){return kind;}
    void update(int seconds,boolean auto,boolean voice,int selectedMinutes){
        minutes=selectedMinutes;number.setText(Integer.toString(minutes));number.setContentDescription(minutes+" минут");
        if(!auto)touched=false;
        detail.setText(auto&&!touched?"Автостарт через "+seconds+" с.\nИли начните прямо сейчас.":"Жду запуска.\nВремя можно изменить.");
        hint.setText(voice?"«Поменяй интервал на десять минут»\n«Старт» / «Да» / «Нет»":"Выберите длительность\nи нажмите «Начать».");
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused){immersive();getWindow().getDecorView().postDelayed(()->{if(isShowing())immersive();},500);}}
    @Override public void show(){super.show();immersive();getWindow().setLayout(Math.min(dp(580),getContext().getResources().getDisplayMetrics().widthPixels-dp(24)),WindowManager.LayoutParams.WRAP_CONTENT);}
}

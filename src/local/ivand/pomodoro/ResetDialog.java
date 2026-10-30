package local.ivand.pomodoro;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Two explicit reset scopes, opened from the tomato above the timer. */
final class ResetDialog {
    private static int dp(Activity a,int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    private static GradientDrawable shape(Activity a,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));return d;}
    static void show(Activity owner,Runnable interval,Runnable cycle){
        FullscreenDialog dialog=new FullscreenDialog(owner);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setPadding(dp(owner,24),dp(owner,18),dp(owner,24),dp(owner,18));root.setBackground(shape(owner,0xff202b2a,24));
        TextView title=new TextView(owner);title.setText("Начать заново");title.setTextSize(23);title.setTextColor(0xfff3f1e2);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        add(owner,root,dialog,"Сбросить интервал","Текущий этап с начала · прогресс цикла сохранится",interval);
        add(owner,root,dialog,"Сбросить весь цикл","Первая работа выбранного режима · прогресс обнулится",cycle);
        Button cancel=new Button(owner);cancel.setText("Отмена");cancel.setAllCaps(false);cancel.setTextColor(0xffa0b1a8);cancel.setBackgroundColor(android.graphics.Color.TRANSPARENT);cancel.setOnClickListener(v->dialog.dismiss());root.addView(cancel,new LinearLayout.LayoutParams(-1,dp(owner,42)));
        dialog.setContentView(root);Window w=dialog.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);w.setDimAmount(.65f);
        dialog.setOnDismissListener(d->owner.getWindow().getDecorView().setSystemUiVisibility(5894));dialog.show();w.setLayout(Math.min(dp(owner,520),owner.getResources().getDisplayMetrics().widthPixels-dp(owner,24)),-2);
    }
    private static void add(Activity owner,LinearLayout root,FullscreenDialog dialog,String title,String detail,Runnable action){
        LinearLayout row=new LinearLayout(owner);row.setOrientation(1);row.setPadding(dp(owner,16),dp(owner,10),dp(owner,16),dp(owner,10));row.setBackground(shape(owner,0xff34443c,14));
        TextView heading=new TextView(owner);heading.setText(title);heading.setTextSize(18);heading.setTextColor(0xfff18770);row.addView(heading);
        TextView hint=new TextView(owner);hint.setText(detail);hint.setTextSize(12);hint.setTextColor(0xffa0b1a8);row.addView(hint);
        row.setContentDescription(title+". "+detail);row.setFocusable(true);row.setOnClickListener(v->{dialog.dismiss();action.run();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(owner,12);root.addView(row,lp);
    }
}

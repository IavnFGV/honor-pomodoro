package local.ivand.pomodoro;

import android.app.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Confirmation that keeps the same palette and immersive behavior as the timer. */
final class ConfirmDialog {
    private static int dp(Activity a,int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
    private static GradientDrawable shape(Activity a,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));return d;}
    static void show(Activity owner,String title,String message,String accept,Runnable confirmed){
        FullscreenDialog dialog=new FullscreenDialog(owner);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setPadding(dp(owner,24),dp(owner,22),dp(owner,24),dp(owner,22));root.setBackground(shape(owner,0xff202b2a,24));
        TextView heading=new TextView(owner);heading.setText(title);heading.setTextSize(23);heading.setTextColor(0xfff3f1e2);heading.setTypeface(null,Typeface.BOLD);root.addView(heading);
        TextView detail=new TextView(owner);detail.setText(message);detail.setTextSize(15);detail.setTextColor(0xffa0b1a8);detail.setLineSpacing(dp(owner,3),1);LinearLayout.LayoutParams description=new LinearLayout.LayoutParams(-1,-2);description.setMargins(0,dp(owner,12),0,dp(owner,24));root.addView(detail,description);
        LinearLayout row=new LinearLayout(owner);
        Button no=button(owner,"Отмена",false);no.setOnClickListener(v->dialog.dismiss());row.addView(no,new LinearLayout.LayoutParams(0,dp(owner,46),1));
        Button yes=button(owner,accept,true);yes.setOnClickListener(v->{dialog.dismiss();confirmed.run();});LinearLayout.LayoutParams yesLp=new LinearLayout.LayoutParams(0,dp(owner,46),1);yesLp.leftMargin=dp(owner,12);row.addView(yes,yesLp);root.addView(row);
        dialog.setContentView(root);Window w=dialog.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);w.setDimAmount(.65f);
        dialog.setOnDismissListener(d->owner.getWindow().getDecorView().setSystemUiVisibility(5894));dialog.show();w.setLayout(Math.min(dp(owner,520),owner.getResources().getDisplayMetrics().widthPixels-dp(owner,24)),-2);
    }
    private static Button button(Activity owner,String label,boolean primary){Button b=new Button(owner);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(primary?0xff141b1c:0xfff3f1e2);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(owner,10),0,dp(owner,10),0);b.setBackground(shape(owner,primary?0xfff18770:0xff34443c,14));return b;}
}

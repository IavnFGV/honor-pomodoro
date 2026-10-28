package local.ivand.pomodoro;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public final class MainActivity extends Activity {
    private Face face;
    private boolean registered, running, work=true, voice, keepScreen=true;
    private long left=25*60000L,duration=25*60000L,receivedAt;
    private int completed,today;
    private String status="Голос выключен",message="";
    private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        left=i.getLongExtra("left",left);duration=i.getLongExtra("duration",duration);receivedAt=SystemClock.elapsedRealtime();
        running=i.getBooleanExtra("running",false);work=i.getBooleanExtra("work",true);voice=i.getBooleanExtra("voice",false);
        completed=i.getIntExtra("completed",0);today=i.getIntExtra("today",0);keepScreen=i.getBooleanExtra("keepScreen",true);
        status=i.getStringExtra("voiceStatus");message=i.getStringExtra("message");
        if(keepScreen)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        face.setContentDescription((work?"Работа ":"Перерыв ")+time()+". "+status);face.invalidate();
    }};
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);face=new Face();setContentView(face);immersive();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();}
    @Override public void onResume(){super.onResume();registerReceiver(receiver,new IntentFilter(PomodoroService.UPDATE));registered=true;act("state");}
    @Override public void onPause(){if(registered){unregisterReceiver(receiver);registered=false;}super.onPause();}
    private void act(String action){startService(new Intent(this,PomodoroService.class).setAction(action));}
    private void voice(){if(!voice&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);else act("voice");}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==1&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)act("voice");}
    private long remaining(){return running?Math.max(0,left-(SystemClock.elapsedRealtime()-receivedAt)):left;}
    private String time(){long seconds=(remaining()+999)/1000;return String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
    private void reset(){new AlertDialog.Builder(this).setTitle("Сбросить текущий интервал?").setMessage("Законченные подходы сохранятся.").setPositiveButton("Сбросить",(d,w)->act("reset")).setNegativeButton("Отмена",null).show();}
    private void settings(){new SettingsPanel(this,new SettingsPanel.Actions(){
        public void action(String name){act(name);}
        public void voice(){MainActivity.this.voice();}
        public void configure(int w,int s,int l){startService(new Intent(MainActivity.this,PomodoroService.class).setAction("config").putExtra("work",w).putExtra("short",s).putExtra("long",l));}
        public void exit(){act("exit");finish();}
    }).show(keepScreen,voice);}
    private final class Face extends View {
        private final Paint p=new Paint(3);private float scale,ox,oy;private float downX,downY;
        private final int bg=Color.rgb(20,27,28),muted=Color.rgb(150,164,160),white=Color.rgb(243,241,226);
        Face(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_HARDWARE,null);setFocusable(true);}
        private int accent(){return work?Color.rgb(241,135,112):Color.rgb(160,205,164);}
        private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(s==null?"":s,x,y,p);}
        private void pill(Canvas c,float l,float t,float r,float b,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,18,18,p);}
        private String clipped(String s,int max){return s==null?"":s.length()>max?s.substring(0,max-1)+"…":s;}
        private void harvest(Canvas c,float cx,float cy){
            int count=!work&&completed>0?(completed-1)%4+1:completed%4;
            for(int i=0;i<4;i++){
                float x=cx+(i-1.5f)*26;
                p.setStyle(Paint.Style.FILL);
                p.setColor(i<count?Color.rgb(241,135,112):Color.rgb(53,66,62));
                c.drawOval(x-9,cy-6,x+9,cy+8,p);
                p.setColor(i<count?Color.rgb(160,205,164):Color.rgb(92,111,99));
                Path leaf=new Path();leaf.moveTo(x,cy-3);leaf.lineTo(x-5,cy-9);leaf.lineTo(x-1,cy-7);leaf.lineTo(x+1,cy-12);leaf.lineTo(x+3,cy-7);leaf.lineTo(x+7,cy-9);leaf.lineTo(x+3,cy-3);leaf.close();c.drawPath(leaf,p);
            }
            text(c,"Завершено "+count+" из 4",cx,cy+26,10,muted,false);
        }
        private void landscape(Canvas c){
            text(c,"P O M O D O R O",102,31,11,muted,true);
            pill(c,574,12,622,48,Color.rgb(36,46,46));text(c,"•••",598,35,17,white,true);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.rgb(45,56,55));c.drawCircle(174,193,129,p);
            p.setColor(accent());p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(new RectF(45,64,303,322),-90,360*Math.max(0,Math.min(1,remaining()/(float)duration)),false,p);
            p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
            p.setColor(accent());c.drawOval(163,104,187,124,p);p.setColor(Color.rgb(151,180,135));c.drawRect(174,100,177,109,p);
            text(c,time(),174,216,71,white,false);
            text(c,running?(work?"РАБОТАЕМ":"ОТДЫХАЕМ"):remaining()<duration?"НА ПАУЗЕ":"ГОТОВ К СТАРТУ",174,246,10,muted,true);
            harvest(c,174,277);
            text(c,work?"Время работать":"Можно выдохнуть",481,92,23,white,true);
            text(c,"Подход "+(work?completed+1:completed)+" · длинный перерыв после 4",481,117,11,muted,false);
            pill(c,350,150,552,208,accent());text(c,running?"Пауза":remaining()<duration?"Продолжить":"Начать",451,187,18,bg,true);
            pill(c,562,150,614,208,Color.rgb(36,46,46));text(c,"↺",588,188,27,white,false);
            pill(c,350,224,477,261,work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Работа",413,248,13,work?white:muted,work);
            pill(c,487,224,614,261,!work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Перерыв",550,248,13,!work?white:muted,!work);
            text(c,"Сегодня · подходов: "+today,482,286,12,muted,false);
            pill(c,345,302,619,341,Color.rgb(33,44,42));p.setColor(voice?Color.rgb(160,205,164):muted);c.drawCircle(362,322,4,p);
            text(c,clipped(status,34),491,326,11,voice?white:muted,false);
            text(c,clipped(message,85),320,355,9,muted,false);
        }
        @Override protected void onDraw(Canvas c){
            c.drawColor(bg);boolean wide=getWidth()>getHeight();float designW=wide?640:360,designH=wide?360:640;scale=Math.min(getWidth()/designW,getHeight()/designH);ox=(getWidth()-designW*scale)/2;oy=(getHeight()-designH*scale)/2;
            c.save();c.translate(ox,oy);c.scale(scale,scale);
            if(wide){landscape(c);c.restore();if(running&&hasWindowFocus())postInvalidateDelayed(33);return;}
            text(c,"P O M O D O R O",96,38,11,muted,true);
            pill(c,279,17,337,51,Color.rgb(36,46,46));text(c,"•••",308,39,17,white,true);
            text(c,work?"Время сосредоточиться":"Можно выдохнуть",180,93,21,white,true);
            text(c,"Подход "+(work?completed+1:completed)+"  /  длинный перерыв после 4",180,117,11,muted,false);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.rgb(45,56,55));c.drawCircle(180,270,118,p);
            p.setStrokeCap(Paint.Cap.ROUND);p.setColor(accent());
            c.drawArc(new RectF(62,152,298,388),-90,360*Math.max(0,Math.min(1,remaining()/(float)duration)),false,p);p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
            // A tiny original tomato mark; no external assets or animation dependency.
            p.setColor(accent());c.drawOval(168,187,192,207,p);p.setColor(Color.rgb(151,180,135));c.drawRect(179,183,182,192,p);
            text(c,time(),180,287,65,white,false);
            text(c,running?(work?"РАБОТАЕМ":"ОТДЫХАЕМ"):remaining()<duration?"НА ПАУЗЕ":"ГОТОВ К СТАРТУ",180,319,10,muted,true);
            harvest(c,180,346);
            pill(c,35,416,275,474,accent());text(c,running?"Пауза":remaining()<duration?"Продолжить":"Начать",155,452,18,bg,true);
            pill(c,285,416,333,474,Color.rgb(36,46,46));text(c,"↺",309,453,27,white,false);
            pill(c,35,490,175,526,work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Работа",105,513,13,work?white:muted,work);
            pill(c,185,490,325,526,!work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Перерыв",255,513,13,!work?white:muted,!work);
            text(c,"Сегодня  ·  "+today+" "+(today==1?"подход":"подходов"),180,558,12,muted,false);
            pill(c,24,578,336,619,Color.rgb(33,44,42));p.setColor(voice?Color.rgb(160,205,164):muted);c.drawCircle(43,599,4,p);
            text(c,clipped(status,36),187,603,11,voice?white:muted,false);
            text(c,clipped(message,48),180,635,9,muted,false);
            c.restore();if(running&&hasWindowFocus())postInvalidateDelayed(33);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
            if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){if(Math.abs(x-downX)>25||Math.abs(y-downY)>25)return true;performClick();
                if(getWidth()>getHeight()){
                    if(y<55&&x>570)settings();
                    else if(y>150&&y<208&&x>350){if(x>557)reset();else act(running?"pause":"start");}
                    else if(y>224&&y<261&&x>350&&x<615){String action=x<482?"work":"break";if(running)new AlertDialog.Builder(MainActivity.this).setTitle("Сменить этап?").setMessage("Текущий интервал будет сброшен.").setPositiveButton("Сменить",(d,w)->act(action)).setNegativeButton("Отмена",null).show();else act(action);}
                    else if(y>302&&y<343&&x>345)voice();
                    return true;
                }
                if(y<65&&x>260)settings();
                else if(y>416&&y<474){if(x>280)reset();else if(x>30)act(running?"pause":"start");}
                else if(y>490&&y<526){final String action=x<180?"work":"break";if(running)new AlertDialog.Builder(MainActivity.this).setTitle("Сменить этап?").setMessage("Текущий интервал будет сброшен.").setPositiveButton("Сменить",(d,w)->act(action)).setNegativeButton("Отмена",null).show();else act(action);}
                else if(y>575&&y<623)voice();return true;
            }return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}

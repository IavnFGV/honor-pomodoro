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
    public static final String ACTION_WAKE_SCREEN="local.ivand.pomodoro.WAKE_SCREEN";
    private Face face;
    private boolean registered, running, work=true, voice, keepScreen=true;
    private long left=25*60000L,duration=25*60000L,deadline;
    private int cycleCompleted,longEvery=4;
    private String presetName="Свой режим";
    private String status="Голос выключен",message="";
    private TransitionDialog transitionDialog;
    private void updateDecision(int kind,int seconds,boolean auto){
        if(kind==0){if(transitionDialog!=null){transitionDialog.dismiss();transitionDialog=null;}return;}
        if(transitionDialog==null||transitionDialog.kind()!=kind){if(transitionDialog!=null)transitionDialog.dismiss();transitionDialog=new TransitionDialog(this,kind,new TransitionDialog.Actions(){
            public void accept(){act("yes");}public void reject(){act("no");}public void edit(){act("editNext");}
            public void minutes(int value){startService(new Intent(MainActivity.this,PomodoroService.class).setAction("nextMinutes").putExtra("minutes",value));}
        });transitionDialog.show();}
        transitionDialog.update(seconds,auto,voice,(int)(duration/60000));
    }
    private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        left=i.getLongExtra("left",left);duration=i.getLongExtra("duration",duration);deadline=i.getLongExtra("deadline",deadline);
        running=i.getBooleanExtra("running",false);work=i.getBooleanExtra("work",true);voice=i.getBooleanExtra("voice",false);
        keepScreen=i.getBooleanExtra("keepScreen",true);
        cycleCompleted=i.getIntExtra("cycleCompleted",0);longEvery=i.getIntExtra("longEvery",4);presetName=i.getStringExtra("presetName");
        status=i.getStringExtra("voiceStatus");message=i.getStringExtra("message");
        long decisionDeadline=i.getLongExtra("decisionDeadline",0);
        updateDecision(i.getIntExtra("decision",0),(int)Math.max(0,(decisionDeadline-SystemClock.elapsedRealtime()+999)/1000),decisionDeadline>0);
        if(keepScreen)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        face.setContentDescription((work?"Работа ":"Перерыв ")+time()+". "+status);face.invalidate();
    }};
    @Override public void onCreate(Bundle b){super.onCreate(b);configureLockScreen(getIntent());getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);face=new Face();setContentView(face);immersive();}
    private void configureLockScreen(Intent intent){
        // Android 7 uses window flags; setShowWhenLocked/setTurnScreenOn require API 27.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        if(intent!=null&&ACTION_WAKE_SCREEN.equals(intent.getAction()))getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);configureLockScreen(intent);immersive();}
    @Override protected void onStop(){getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);super.onStop();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus){immersive();getWindow().getDecorView().postDelayed(()->{if(hasWindowFocus())immersive();},500);}}
    @Override public void onResume(){super.onResume();immersive();UiUpdates.subscribe(receiver);registered=true;act("state");}
    @Override public void onPause(){if(registered){UiUpdates.unsubscribe(receiver);registered=false;}super.onPause();}
    @Override public void onDestroy(){if(transitionDialog!=null)transitionDialog.dismiss();super.onDestroy();}
    private void act(String action){startService(new Intent(this,PomodoroService.class).setAction(action));}
    private void voice(){if(!voice&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);else act("voice");}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==1&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)act("voice");}
    private long remaining(){return TimerDisplay.remaining(running,left,deadline,SystemClock.elapsedRealtime());}
    private String time(){long seconds=(remaining()+999)/1000;return String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
    private void reset(){ResetDialog.show(this,()->act("reset"),()->act("resetCycle"));}
    private void changeStage(String action){if(running)ConfirmDialog.show(this,"Сменить этап?","Текущий интервал будет сброшен.\nПрогресс цикла сохранится.","Сменить",()->act(action));else act(action);}
    private void settings(){new SettingsPanel(this,new SettingsPanel.Actions(){
        public void action(String name){act(name);}
        public void voice(){MainActivity.this.voice();}
        public void configure(int w,int s,int l){startService(new Intent(MainActivity.this,PomodoroService.class).setAction("config").putExtra("work",w).putExtra("short",s).putExtra("long",l));}
        public void exit(){act("exit");finish();}
    }).show(keepScreen,voice);}
    private void presets(){new PresetPanel(this,id->startService(new Intent(this,PomodoroService.class).setAction("preset").putExtra("id",id))).show();}
    private final class Face extends View {
        private final Paint p=new Paint(3);private float scale,ox,oy;private float downX,downY;
        private final int bg=Color.rgb(20,27,28),muted=Color.rgb(150,164,160),white=Color.rgb(243,241,226);
        Face(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_NONE,null);setFocusable(true);}
        private int accent(){return work?Color.rgb(241,135,112):Color.rgb(160,205,164);}
        private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);c.drawText(s==null?"":s,x,y,p);}
        private void pill(Canvas c,float l,float t,float r,float b,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,18,18,p);}
        private String clipped(String s,int max){return s==null?"":s.length()>max?s.substring(0,max-1)+"…":s;}
        private void resetTomato(Canvas c,float x,float y){
            p.setStyle(Paint.Style.FILL);p.setColor(accent());c.drawOval(x-18,y-12,x+18,y+16,p);
            p.setColor(Color.rgb(151,180,135));Path leaf=new Path();leaf.moveTo(x,y-6);leaf.lineTo(x-10,y-16);leaf.lineTo(x-2,y-13);leaf.lineTo(x+2,y-21);leaf.lineTo(x+5,y-13);leaf.lineTo(x+12,y-16);leaf.close();c.drawPath(leaf,p);
            text(c,"↺",x,y+10,23,bg,true);
        }
        private void harvest(Canvas c,float cx,float cy){
            int count=!work&&cycleCompleted>0?(cycleCompleted-1)%longEvery+1:cycleCompleted%longEvery;
            float spacing=longEvery>5?22:26;
            for(int i=0;i<longEvery;i++){
                float x=cx+(i-(longEvery-1)/2f)*spacing;
                p.setStyle(Paint.Style.FILL);
                p.setColor(i<count?Color.rgb(241,135,112):Color.rgb(53,66,62));
                c.drawOval(x-9,cy-6,x+9,cy+8,p);
                p.setColor(i<count?Color.rgb(160,205,164):Color.rgb(92,111,99));
                Path leaf=new Path();leaf.moveTo(x,cy-3);leaf.lineTo(x-5,cy-9);leaf.lineTo(x-1,cy-7);leaf.lineTo(x+1,cy-12);leaf.lineTo(x+3,cy-7);leaf.lineTo(x+7,cy-9);leaf.lineTo(x+3,cy-3);leaf.close();c.drawPath(leaf,p);
            }
            text(c,"Завершено "+count+" из "+longEvery,cx,cy+26,10,muted,false);
        }
        private void landscape(Canvas c){
            pill(c,25,12,315,48,Color.rgb(36,46,46));text(c,clipped(presetName,28),163,36,13,white,true);text(c,"⌄",299,35,17,muted,false);
            pill(c,574,12,622,48,Color.rgb(36,46,46));text(c,"•••",598,35,17,white,true);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.rgb(45,56,55));c.drawCircle(174,193,129,p);
            p.setColor(accent());p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(new RectF(45,64,303,322),-90,360*Math.max(0,Math.min(1,remaining()/(float)duration)),false,p);
            p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
            resetTomato(c,174,113);
            text(c,time(),174,216,71,white,false);
            text(c,running?(work?"РАБОТАЕМ":"ОТДЫХАЕМ"):remaining()<duration?"НА ПАУЗЕ":"ГОТОВ К СТАРТУ",174,246,10,muted,true);
            harvest(c,174,277);
            text(c,work?"Время работать":"Можно выдохнуть",481,92,23,white,true);
            text(c,"В своём ритме · до длинного отдыха",481,117,11,muted,false);
            pill(c,350,150,614,208,accent());text(c,running?"Пауза":remaining()<duration?"Продолжить":"Старт",482,187,18,bg,true);
            pill(c,350,224,477,261,work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Работа",413,248,13,work?white:muted,work);
            pill(c,487,224,614,261,!work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Перерыв",550,248,13,!work?white:muted,!work);
            pill(c,345,302,619,341,Color.rgb(33,44,42));p.setColor(voice?Color.rgb(160,205,164):muted);c.drawCircle(362,322,4,p);
            text(c,clipped(status,34),491,326,11,voice?white:muted,false);
            text(c,clipped(message,85),320,355,9,muted,false);
        }
        @Override protected void onDraw(Canvas c){
            c.drawColor(bg);boolean wide=getWidth()>getHeight();float designW=wide?640:360,designH=wide?360:640;scale=Math.min(getWidth()/designW,getHeight()/designH);ox=(getWidth()-designW*scale)/2;oy=(getHeight()-designH*scale)/2;
            c.save();c.translate(ox,oy);c.scale(scale,scale);
            if(wide){landscape(c);c.restore();if(running&&hasWindowFocus())postInvalidateDelayed(33);return;}
            pill(c,24,17,264,51,Color.rgb(36,46,46));text(c,clipped(presetName,23),137,39,12,white,true);text(c,"⌄",249,39,16,muted,false);
            pill(c,279,17,337,51,Color.rgb(36,46,46));text(c,"•••",308,39,17,white,true);
            text(c,work?"Время сосредоточиться":"Можно выдохнуть",180,93,21,white,true);
            text(c,"В своём ритме · до длинного отдыха",180,117,11,muted,false);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.rgb(45,56,55));c.drawCircle(180,270,118,p);
            p.setStrokeCap(Paint.Cap.ROUND);p.setColor(accent());
            c.drawArc(new RectF(62,152,298,388),-90,360*Math.max(0,Math.min(1,remaining()/(float)duration)),false,p);p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
            // A tiny original tomato mark; no external assets or animation dependency.
            resetTomato(c,180,197);
            text(c,time(),180,287,65,white,false);
            text(c,running?(work?"РАБОТАЕМ":"ОТДЫХАЕМ"):remaining()<duration?"НА ПАУЗЕ":"ГОТОВ К СТАРТУ",180,319,10,muted,true);
            harvest(c,180,346);
            pill(c,35,416,325,474,accent());text(c,running?"Пауза":remaining()<duration?"Продолжить":"Старт",180,452,18,bg,true);
            pill(c,35,490,175,526,work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Работа",105,513,13,work?white:muted,work);
            pill(c,185,490,325,526,!work?Color.rgb(54,66,62):Color.rgb(30,40,39));text(c,"Перерыв",255,513,13,!work?white:muted,!work);
            pill(c,24,578,336,619,Color.rgb(33,44,42));p.setColor(voice?Color.rgb(160,205,164):muted);c.drawCircle(43,599,4,p);
            text(c,clipped(status,36),187,603,11,voice?white:muted,false);
            text(c,clipped(message,48),180,635,9,muted,false);
            c.restore();if(running&&hasWindowFocus())postInvalidateDelayed(33);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
            if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){if(Math.abs(x-downX)>25||Math.abs(y-downY)>25)return true;performClick();
                if(getWidth()>getHeight()){
                    if(y<55&&x>570)settings();else if(y<55&&x>25&&x<315)presets();
                    else if(y>89&&y<137&&x>150&&x<198)reset();
                    else if(y>150&&y<208&&x>350&&x<614)act(running?"pause":"start");
                    else if(y>224&&y<261&&x>350&&x<615)changeStage(x<482?"work":"break");
                    else if(y>302&&y<343&&x>345)voice();
                    return true;
                }
                if(y<65&&x>270)settings();else if(y<65&&x>24&&x<264)presets();
                else if(y>173&&y<221&&x>156&&x<204)reset();
                else if(y>416&&y<474&&x>35&&x<325)act(running?"pause":"start");
                else if(y>490&&y<526)changeStage(x<180?"work":"break");
                else if(y>575&&y<623)voice();return true;
            }return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}

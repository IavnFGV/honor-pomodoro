package local.ivand.pomodoro;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.media.*;
import android.os.*;
import android.util.Log;
import java.util.Locale;

public final class PomodoroService extends Service {
    public static final String UPDATE="local.ivand.pomodoro.UPDATE";
    private final Handler handler=new Handler();
    private final TimerEngine timer=new TimerEngine();
    private VoiceWorker voice;
    private Thread voiceThread;
    private PowerManager.WakeLock cpu;
    private String voiceStatus="Голос выключен", lastCommand="";
    private boolean keepScreen=true;
    private long confirmUntil;
    private String today;
    private int todayCount;
    private String notificationText="";
    private boolean destroyed;
    private PendingIntent alarm;
    private NotificationManager notifications;
    private String date() {return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new java.util.Date());}
    @Override public void onCreate() {
        super.onCreate(); notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        SharedPreferences p=getSharedPreferences("timer",0);
        timer.workMinutes=p.getInt("work",25); timer.shortMinutes=p.getInt("short",5);timer.longMinutes=p.getInt("long",15);
        timer.completed=p.getInt("completed",0);timer.work=p.getBoolean("isWork",true);
        timer.remaining=p.getLong("remaining",timer.duration());
        // Recovered intervals are paused. Never silently start a microphone after process death/reboot.
        if(p.getBoolean("running",false)) timer.remaining=Math.min(timer.duration(),Math.max(0,p.getLong("wallDeadline",0)-System.currentTimeMillis()));
        if(timer.remaining<=0) timer.remaining=timer.duration();
        today=p.getString("date",date()); todayCount=p.getInt("todayCount",0);
        keepScreen=p.getBoolean("keepScreen",true);
        alarm=PendingIntent.getService(this,3,new Intent(this,PomodoroService.class).setAction("tick"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        startForeground(1,notification("Готов к работе"));handler.post(ticker);
    }
    private Notification notification(String text) {
        return new Notification.Builder(this).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Помодоро · "+(timer.work?"Работа":"Перерыв")).setContentText(text)
            .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false)
            .addAction(android.R.drawable.ic_media_pause,"Пауза",PendingIntent.getService(this,1,new Intent(this,PomodoroService.class).setAction("pause"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();
    }
    private void save() {
        long left=timer.left(SystemClock.elapsedRealtime());
        getSharedPreferences("timer",0).edit().putInt("work",timer.workMinutes).putInt("short",timer.shortMinutes).putInt("long",timer.longMinutes)
          .putInt("completed",timer.completed).putBoolean("isWork",timer.work).putLong("remaining",left)
          .putBoolean("running",timer.running).putLong("wallDeadline",System.currentTimeMillis()+left)
          .putString("date",today).putInt("todayCount",todayCount).putBoolean("keepScreen",keepScreen).apply();
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); am.cancel(alarm);
        if(timer.running) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,timer.deadline,alarm);
    }
    private final Runnable ticker=new Runnable(){ public void run(){
        finishIfDue(); publish(); handler.postDelayed(this,1000);
    }};
    private void finishIfDue() {
        boolean wasWork=timer.work;
        if(timer.tick(SystemClock.elapsedRealtime())) {
            rollDay();if(wasWork) todayCount++;
            save(); signal(wasWork?Color.RED:Color.GREEN);
            lastCommand=wasWork?"Работа завершена. Время отдохнуть":"Перерыв завершён. Можно начинать";
            Log.i("HonorPomo","COMPLETE work="+wasWork+" completed="+timer.completed);
        }
    }
    private void rollDay(){if(!date().equals(today)){today=date();todayCount=0;}}
    private void tone() {try {final ToneGenerator t=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,65);t.startTone(ToneGenerator.TONE_PROP_ACK,180);handler.postDelayed(()->t.release(),300);}catch(Exception e){Log.w("HonorPomo","Tone",e);}}
    private void signal(int color) {
        notifications.cancel(2);
        notifications.notify(2,new Notification.Builder(this).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
          .setContentTitle("Помодоро").setContentText("Этап завершён · откройте таймер")
          .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE))
          // Android 7 demotes silent notifications to IMPORTANCE_LOW regardless of priority.
          // A real short vibration keeps this completion/LED notification alerting.
          .setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[]{0,80})
          .setAutoCancel(true).setLights(color,500,1500).build());tone();
        Log.i("HonorPomo","LED requested="+Integer.toHexString(color)+" on=500 off=1500 (hardware unverified)");
    }
    private void publish() {
        rollDay();long left=timer.left(SystemClock.elapsedRealtime());
        sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()).putExtra("left",left).putExtra("duration",timer.duration())
          .putExtra("running",timer.running).putExtra("work",timer.work).putExtra("completed",timer.completed).putExtra("today",todayCount)
          .putExtra("voice",voice!=null).putExtra("voiceStatus",voiceStatus).putExtra("message",lastCommand).putExtra("keepScreen",keepScreen));
        String summary=(timer.work?"Работа":"Перерыв")+" · "+((left+59999)/60000)+" мин · "+(timer.running?"идёт":"пауза")+" · "+(voice!=null?"микрофон включён":"голос выключен");
        if(!summary.equals(notificationText)){notificationText=summary;notifications.notify(1,notification(summary));}
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String action=intent==null?"state":intent.getAction(); if(action==null) action="state";
        finishIfDue();long now=SystemClock.elapsedRealtime();
        switch(action) {
          case "start":timer.start(now);lastCommand="";notifications.cancel(2);break;
          case "pause":timer.pause(now);break;
          case "reset":timer.reset();lastCommand="Таймер сброшен";break;
          case "work":timer.select(true);lastCommand="";break;
          case "break":timer.select(false);lastCommand="";break;
          case "voice": if(voice==null)startVoice();else stopVoice();break;
          case "screen":keepScreen=!keepScreen;break;
          case "red":signal(Color.RED);lastCommand="Тест красного LED · погасите экран";break;
          case "green":signal(Color.GREEN);lastCommand="Тест зелёного LED · погасите экран";break;
          case "clearLed":notifications.cancel(2);lastCommand="Индикация приложения снята";break;
          case "config":
            timer.workMinutes=Math.max(1,Math.min(120,intent.getIntExtra("work",25)));
            timer.shortMinutes=Math.max(1,Math.min(60,intent.getIntExtra("short",5)));
            timer.longMinutes=Math.max(1,Math.min(60,intent.getIntExtra("long",15)));timer.reset();break;
          case "exit": stopVoice();timer.pause(now);save();stopForeground(true);stopSelf();return START_NOT_STICKY;
        }
        if(!action.equals("state")&&!action.equals("tick")) save();publish();return START_STICKY;
    }
    private void startVoice() {
        if(voiceThread!=null && voiceThread.isAlive()){voiceStatus="Предыдущее прослушивание завершается…";return;}
        cpu=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"HonorPomo:voice");cpu.setReferenceCounted(false);cpu.acquire();
        voice=new VoiceWorker(this,new VoiceWorker.Listener(){
            public void status(String s){handler.post(()->{if(!destroyed&&voice!=null){voiceStatus=s;publish();}});}
            public void command(String s){handler.post(()->{if(!destroyed&&voice!=null)handleVoice(s);});}
            public void wake(){handler.post(()->{if(!destroyed&&voice!=null)tone();});}
        });
        final VoiceWorker worker=voice;
        voiceThread=new Thread(()->{worker.run();handler.post(()->{if(voice==worker){voice=null;if(cpu!=null&&cpu.isHeld())cpu.release();publish();}});},"HonorVoice");
        voiceThread.start();
    }
    private void stopVoice(){if(voice!=null)voice.stop();voice=null;voiceStatus="Голос выключен";if(cpu!=null&&cpu.isHeld())cpu.release();}
    private void handleVoice(String text) {
        Log.i("HonorPomo","VOICE final="+text); long now=SystemClock.elapsedRealtime();
        lastCommand="Услышал: «"+text+"»";
        switch(text) {
          case "начать работу":case "работа":case "работу": if(!timer.work)timer.select(true);timer.start(now);break;
          case "начать":case "старт":case "таймер":case "запусти таймер":case "включи таймер":timer.start(now);break;
          case "пауза":timer.pause(now);break;
          case "продолжить":timer.start(now);break;
          case "начать перерыв":case "перерыв":timer.select(false);timer.start(now);break;
          case "остановить таймер":case "стоп":timer.pause(now);confirmUntil=now+15000;lastCommand="Сбросить? «Помидорка, подтверждаю»";break;
          case "подтверждаю":if(now<confirmUntil){timer.reset();lastCommand="Таймер сброшен";}confirmUntil=0;break;
          case "отмена":confirmUntil=0;lastCommand="Отменено";break;
          case "сколько осталось":lastCommand="Осталось "+((timer.left(now)+59999)/60000)+" мин";break;
          default:lastCommand="Команда не принята";break;
        }
        save();publish();tone();
    }
    @Override public void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);stopVoice();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}

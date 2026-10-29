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
    private final IntervalTransition transition=new IntervalTransition();
    private PowerManager.WakeLock endingLock;
    private long warnedDeadline;
    private int warnedSecond;
    private VoiceWorker voice;
    private Thread voiceThread;
    private PowerManager.WakeLock cpu;
    private String voiceStatus="Голос выключен", lastCommand="";
    private boolean keepScreen=true;
    private boolean autoWork=false,autoRest=true;
    private String presetId="",presetName="Свой режим";
    private long confirmUntil;
    private String today;
    private int todayCount;
    private String notificationText="";
    private boolean destroyed;
    private PendingIntent alarm;
    private NotificationManager notifications;
    private SpeechOutput speech;
    private boolean speaking;
    private String date() {return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new java.util.Date());}
    @Override public void onCreate() {
        super.onCreate(); notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        SharedPreferences p=getSharedPreferences("timer",0);
        timer.workMinutes=p.getInt("work",25); timer.shortMinutes=p.getInt("short",5);timer.longMinutes=p.getInt("long",15);
        timer.completed=p.getInt("completed",0);timer.work=p.getBoolean("isWork",true);
        timer.longEvery=p.getInt("longEvery",4);timer.cycleCompleted=p.getInt("cycleCompleted",timer.completed);
        presetId=p.getString("presetId","");presetName=p.getString("presetName","Свой режим");
        timer.oneOffMillis=p.getLong("oneOffMillis",0);
        timer.remaining=p.getLong("remaining",timer.duration());
        // Recovered intervals are paused. Never silently start a microphone after process death/reboot.
        if(p.getBoolean("running",false)) timer.remaining=Math.min(timer.duration(),Math.max(0,p.getLong("wallDeadline",0)-System.currentTimeMillis()));
        if(timer.remaining<=0) timer.remaining=timer.duration();
        today=p.getString("date",date()); todayCount=p.getInt("todayCount",0);
        keepScreen=p.getBoolean("keepScreen",true);
        autoWork=p.getBoolean("autoWork",false);autoRest=p.getBoolean("autoRest",true);
        alarm=PendingIntent.getService(this,3,new Intent(this,PomodoroService.class).setAction("tick"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        startForeground(1,notification("Готов к работе"));handler.post(ticker);
        speech=new SpeechOutput(this,new SpeechOutput.Listener(){
            public void gate(boolean active){speaking=active;if(voice!=null)voice.speechGate(active);}
            public void error(String message){if(!destroyed){lastCommand=message;publish();}}
        });
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
          .putLong("oneOffMillis",timer.oneOffMillis).putBoolean("autoWork",autoWork).putBoolean("autoRest",autoRest)
          .putInt("longEvery",timer.longEvery).putInt("cycleCompleted",timer.cycleCompleted).putString("presetId",presetId).putString("presetName",presetName)
          .putString("date",today).putInt("todayCount",todayCount).putBoolean("keepScreen",keepScreen).apply();
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); am.cancel(alarm);
        if(timer.running){long at=timer.work&&timer.left(SystemClock.elapsedRealtime())>5000?timer.deadline-5000:timer.deadline;am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,alarm);}
        else if(transition.kind!=0&&transition.deadline>0)am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,transition.deadline,alarm);
    }
    private final Runnable ticker=new Runnable(){ public void run(){
        finishIfDue(); publish(); handler.postDelayed(this,250);
    }};
    private void finishIfDue() {
        long now=SystemClock.elapsedRealtime();
        long left=timer.left(now);
        if(timer.running&&timer.work&&left>0&&left<=5000){
            int second=(int)((left+999)/1000);
            if(warnedDeadline!=timer.deadline){warnedDeadline=timer.deadline;warnedSecond=0;}
            if(second!=warnedSecond){warnedSecond=second;holdEndingCpu();warningTone();Log.i("HonorPomo","ENDING seconds="+second);}
        }
        boolean wasWork=timer.work;
        if(timer.tick(now)) {
            rollDay();if(wasWork) todayCount++;
            transition.begin(wasWork,now,autoWork,autoRest);holdEndingCpu();
            if(voice!=null)voice.setDecision(transition.kind);
            save(); signal(wasWork?Color.RED:Color.GREEN);
            lastCommand=wasWork?"Работа завершена. Начать перерыв?":"Перерыв завершён. Готов начать работу?";
            showWakeScreen();
            Log.i("HonorPomo","COMPLETE work="+wasWork+" completed="+timer.completed);
        }
        if(transition.expire(timer,now)){clearDecision();lastCommand=timer.work?"Работа началась автоматически":"Перерыв начался автоматически";save();Log.i("HonorPomo",timer.work?"AUTO_WORK started":"AUTO_REST started");}
    }
    private void holdEndingCpu(){if(endingLock==null){endingLock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"HonorPomo:ending");endingLock.setReferenceCounted(false);}endingLock.acquire(20000);}
    private void clearDecision(){transition.cancel();if(voice!=null)voice.setDecision(0);if(endingLock!=null&&endingLock.isHeld())endingLock.release();}
    private void answerDecision(boolean yes){if(transition.answer(timer,yes,SystemClock.elapsedRealtime())){clearDecision();lastCommand=yes?"Интервал начался":"Ожидаю запуска";save();}}
    private void editDecision(){if(transition.kind==0)return;transition.edit();if(endingLock!=null&&endingLock.isHeld())endingLock.release();lastCommand="Выберите минуты и нажмите «Начать»";save();}
    private void setNextMinutes(int minutes){if(transition.kind==0)return;editDecision();try{timer.setNextMinutes(minutes);lastCommand="Следующий интервал: "+minutes+" мин · только этот раз";save();}catch(IllegalArgumentException e){lastCommand="Допустимо от 1 до "+(timer.work?120:60)+" минут";}}
    private void warningTone(){try{final ToneGenerator t=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,40);t.startTone(ToneGenerator.TONE_PROP_BEEP,65);handler.postDelayed(()->t.release(),120);}catch(Exception error){Log.w("HonorPomo","Warning tone",error);}}
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
        UiUpdates.send(this,new Intent(UPDATE).putExtra("left",left).putExtra("duration",timer.duration()).putExtra("deadline",timer.deadline)
          .putExtra("running",timer.running).putExtra("work",timer.work).putExtra("completed",timer.completed).putExtra("today",todayCount)
          .putExtra("voice",voice!=null).putExtra("voiceStatus",voiceStatus).putExtra("message",lastCommand).putExtra("keepScreen",keepScreen)
          .putExtra("decision",transition.kind).putExtra("decisionSeconds",transition.seconds(SystemClock.elapsedRealtime()))
          .putExtra("decisionAuto",transition.deadline>0).putExtra("decisionDeadline",transition.deadline)
          .putExtra("longEvery",timer.longEvery).putExtra("cycleCompleted",timer.cycleCompleted).putExtra("presetName",presetName));
        String summary=(timer.work?"Работа":"Перерыв")+" · "+((left+59999)/60000)+" мин · "+(timer.running?"идёт":"пауза")+" · "+(voice!=null?"микрофон включён":"голос выключен");
        if(!summary.equals(notificationText)){notificationText=summary;notifications.notify(1,notification(summary));}
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String action=intent==null?"state":intent.getAction(); if(action==null) action="state";
        finishIfDue();long now=SystemClock.elapsedRealtime();
        if(action.equals("yes")){answerDecision(true);publish();return START_STICKY;}
        if(action.equals("no")){answerDecision(false);publish();return START_STICKY;}
        if(action.equals("editNext")){editDecision();publish();return START_STICKY;}
        if(action.equals("nextMinutes")){setNextMinutes(intent.getIntExtra("minutes",0));publish();return START_STICKY;}
        if(action.equals("speakTime")){announceRemaining();return START_STICKY;}
        if(action.equals("preset")){
            PomodoroPreset preset=new PresetStore(this).find(intent.getStringExtra("id"));
            if(preset!=null){clearDecision();preset.apply(timer);presetId=preset.id;presetName=preset.name;warnedDeadline=0;warnedSecond=0;lastCommand="Режим: "+preset.name;save();}
            else lastCommand="Режим не найден";
            publish();return START_STICKY;
        }
        if(java.util.Arrays.asList("start","pause","reset","work","break","config","exit").contains(action))clearDecision();
        switch(action) {
          case "start":timer.start(now);lastCommand="";notifications.cancel(2);break;
          case "pause":timer.pause(now);break;
          case "reset":timer.reset();lastCommand="Таймер сброшен";break;
          case "work":timer.select(true);lastCommand="";break;
          case "break":timer.select(false);lastCommand="";break;
          case "voice": if(voice==null)startVoice();else stopVoice();break;
          case "screen":keepScreen=!keepScreen;break;
          case "autoWork":autoWork=!autoWork;break;
          case "autoRest":autoRest=!autoRest;break;
          case "red":signal(Color.RED);lastCommand="Тест красного LED · погасите экран";break;
          case "green":signal(Color.GREEN);lastCommand="Тест зелёного LED · погасите экран";break;
          case "clearLed":notifications.cancel(2);lastCommand="Индикация приложения снята";break;
          case "config":
            timer.workMinutes=Math.max(1,Math.min(120,intent.getIntExtra("work",25)));
            timer.shortMinutes=Math.max(1,Math.min(60,intent.getIntExtra("short",5)));
            timer.longMinutes=Math.max(1,Math.min(60,intent.getIntExtra("long",15)));timer.oneOffMillis=0;timer.cycleCompleted=0;presetId="";presetName="Свой режим";timer.reset();break;
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
            public void wake(){handler.post(()->{if(!destroyed&&voice!=null){showWakeScreen();tone();}});}
        });
        final VoiceWorker worker=voice;
        worker.setDecision(transition.kind);
        if(speaking)worker.speechGate(true);
        voiceThread=new Thread(()->{worker.run();handler.post(()->{if(voice==worker){voice=null;if(cpu!=null&&cpu.isHeld())cpu.release();publish();}});},"HonorVoice");
        voiceThread.start();
    }
    private void stopVoice(){if(voice!=null)voice.stop();voice=null;voiceStatus="Голос выключен";if(cpu!=null&&cpu.isHeld())cpu.release();}
    private void showWakeScreen(){
        // Background Activity launches are supported on the target Android 7.
        // Do not dismiss keyguard or change the phone's lock credentials.
        try{
            // On this Android 7 device TURN_SCREEN_ON alone does not wake an existing paused Activity.
            // A timed display wake lock wakes keyguard too; it never dismisses it.
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            PowerManager.WakeLock display=pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,"HonorPomo:voice-display");
            display.setReferenceCounted(false);display.acquire(1500);
            startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.ACTION_WAKE_SCREEN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP));
            Log.i("HonorPomo","WAKE_UI requested; keyguard not dismissed");
        }catch(RuntimeException error){Log.w("HonorPomo","WAKE_UI blocked; open notification",error);}
    }
    private void handleVoice(String text) {
        finishIfDue(); // Enforce the same timeout boundary as taps, even between handler ticks.
        showWakeScreen();
        Log.i("HonorPomo","VOICE final="+text); long now=SystemClock.elapsedRealtime();
        lastCommand="Услышал: «"+text+"»";
        if(transition.kind!=0){
            int minutes=DurationCommand.parse(text);
            if(minutes>0){setNextMinutes(minutes);publish();tone();return;}
            if(DurationCommand.editing(text)){editDecision();publish();return;}
            if(text.equals("да")||text.equals("старт")||text.equals("начать")||text.equals("продолжить")){answerDecision(true);publish();tone();return;}
            if(text.equals("нет")||text.equals("пауза")){answerDecision(false);publish();tone();return;}
        }
        if(text.equals("да")||text.equals("нет"))return;
        int currentMinutes=DurationCommand.parse(text);
        if(currentMinutes>0){
            try{timer.changeCurrentMinutes(currentMinutes,now);warnedDeadline=0;warnedSecond=0;
                if(endingLock!=null&&endingLock.isHeld())endingLock.release();
                lastCommand="Осталось "+currentMinutes+" мин · только этот интервал";
                Log.i("HonorPomo","CURRENT_DURATION remainingMinutes="+currentMinutes+" running="+timer.running);
                save();
            }catch(IllegalArgumentException error){lastCommand="Допустимо от 1 до "+(timer.work?120:60)+" минут";}
            publish();tone();return;
        }
        if(DurationCommand.editing(text)){lastCommand="Скажите, например: «на пятнадцать минут»";publish();return;}
        if(!text.equals("сколько осталось"))clearDecision();
        switch(text) {
          case "начать работу":case "работа":case "работу": if(!timer.work)timer.select(true);timer.start(now);break;
          case "начать":case "старт":case "таймер":case "запусти таймер":case "включи таймер":timer.start(now);break;
          case "пауза":timer.pause(now);break;
          case "продолжить":timer.start(now);break;
          case "начать перерыв":case "перерыв":timer.select(false);timer.start(now);break;
          case "остановить таймер":case "стоп":timer.pause(now);confirmUntil=now+15000;lastCommand="Сбросить? «Помидорка, подтверждаю»";break;
          case "подтверждаю":if(now<confirmUntil){timer.reset();lastCommand="Таймер сброшен";}confirmUntil=0;break;
          case "отмена":confirmUntil=0;lastCommand="Отменено";break;
          case "сколько осталось":announceRemaining();return;
          default:lastCommand="Команда не принята";break;
        }
        save();publish();tone();
    }
    private void announceRemaining(){lastCommand=SpokenTime.format(timer.left(SystemClock.elapsedRealtime()),timer.running);publish();if(speech!=null)speech.say(lastCommand);}
    @Override public void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);clearDecision();stopVoice();if(speech!=null)speech.close();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}

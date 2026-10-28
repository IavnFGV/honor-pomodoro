package local.ivand.pomodoro;

import android.content.Context;
import android.media.*;
import android.os.SystemClock;
import android.util.Log;
import java.io.*;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

/** One microphone, wake detector then a bounded grammar command window. No audio files saved. */
final class VoiceWorker implements Runnable {
    private static final String[] COMMANDS={"начать работу","работа","работу","начать","старт","таймер","запусти таймер","включи таймер","пауза","продолжить","остановить таймер","стоп","подтверждаю","отмена","начать перерыв","перерыв","сколько осталось"};
    private static String grammar(boolean addressed) {
        org.json.JSONArray words=new org.json.JSONArray();
        if(addressed)words.put("помидорка");
        for(String c:COMMANDS)words.put((addressed?"помидорка ":"")+c);
        words.put("[unk]");return words.toString();
    }
    interface Listener { void status(String text); void command(String text); void wake(); }
    private final Context context;
    private final Listener listener;
    private volatile boolean stop;
    private volatile AudioRecord recording;
    VoiceWorker(Context c, Listener l) { context=c; listener=l; }
    void stop() { stop=true; AudioRecord r=recording; if(r!=null) try {r.stop();} catch(Exception ignored) {} }
    private void copy(String asset, File file) throws IOException {
        if(stop) throw new IOException("Stopped");
        String[] children=context.getAssets().list(asset);
        if(children.length>0) {
            if(!file.isDirectory() && !file.mkdirs()) throw new IOException("mkdir " + file);
            for(String child:children) copy(asset+"/"+child,new File(file,child));
        } else {
            // Files are private to this version of the app; do not re-extract 100 MB every toggle.
            if(file.isFile() && file.length()>0) return;
            try(InputStream in=context.getAssets().open(asset); FileOutputStream out=new FileOutputStream(file)) {
                byte[] b=new byte[65536]; int n; while((n=in.read(b))!=-1) {if(stop) throw new IOException("Stopped"); out.write(b,0,n);}
            } catch(IOException e) { file.delete(); throw e; }
        }
    }
    public void run() {
        AudioRecord r=null;
        try {
            listener.status("Подготовка голоса…");
            File root=new File(context.getFilesDir(),"models-v1"); root.mkdirs();

            copy("vosk-model-small-ru-0.22",new File(root,"vosk"));
            try(
                Model model=new Model(new File(root,"vosk").getPath());
                Recognizer wake=new Recognizer(model,16000f,grammar(true));
                Recognizer asr=new Recognizer(model,16000f,grammar(false))) {
                Log.i("HonorVoice","WAKE_ENGINE Vosk addressed grammar: помидорка; final-only");
                if(stop) return;
                int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
                if(min<=0) throw new IOException("Microphone buffer="+min);
                r=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,1280*16));
                recording=r;
                if(r.getState()!=AudioRecord.STATE_INITIALIZED) throw new IOException("Microphone unavailable");
                if(stop) return;
                r.startRecording();
                if(r.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING) throw new IOException("Microphone did not start");
                short[] frame=new short[1280];
                long until=0, measured=0, totalMs=0, maxMs=0;
                listener.status("Жду: «Помидорка»");
                Log.i("HonorVoice","READY 16000Hz ARMv7; Vosk wake=помидорка; final-only; next-frame command");
                while(!stop) {
                    int offset=0; while(offset<frame.length && !stop) {
                        int n=r.read(frame,offset,frame.length-offset); if(n<=0) {if(stop) return; throw new IOException("Audio read="+n);} offset+=n;
                    }
                    if(stop) return;
                    long now=SystemClock.elapsedRealtime();
                    if(until==0) {
                        long before=SystemClock.elapsedRealtime(); boolean wakeFinal=wake.acceptWaveForm(frame,frame.length);
                        String wakeText=wakeFinal?new JSONObject(wake.getResult()).optString("text",""):"";
                        long cost=SystemClock.elapsedRealtime()-before; totalMs+=cost; maxMs=Math.max(maxMs,cost); measured++;
                        if(measured%125==0) { Log.i("HonorVoice","INFERENCE avgMs="+(totalMs/(double)measured)+" maxMs="+maxMs+" budgetMs=80 frames="+measured); }
                        if("помидорка".equals(wakeText)) {asr.reset(); until=now+6000; listener.status("Слушаю команду…"); listener.wake(); Log.i("HonorVoice","WAKE final="+wakeText);}
                        else if(wakeText.startsWith("помидорка ")) {
                            String command=wakeText.substring("помидорка ".length());
                            for(String allowed:COMMANDS)if(allowed.equals(command)){listener.command(command);Log.i("HonorVoice","ADDRESSED final="+wakeText);break;}
                            wake.reset();
                        }
                    } else {
                        boolean finalResult=asr.acceptWaveForm(frame,frame.length);
                        String text="";
                        if(finalResult) text=new JSONObject(asr.getResult()).optString("text","");
                        if(!text.isEmpty()) {listener.command(text); until=0;}
                        else if(now>=until) { text=new JSONObject(asr.getFinalResult()).optString("text",""); if(!text.isEmpty()) listener.command(text); until=0; }
                        if(until==0) {asr.reset();wake.reset();listener.status("Жду: «Помидорка»");}
                    }
                }
            }
        } catch(Throwable e) { if(!stop) {Log.e("HonorVoice","FAILED",e); listener.status("Ошибка голоса: "+e.getClass().getSimpleName()+": "+e.getMessage());} }
        finally {recording=null;if(r!=null) {try{r.stop();}catch(Exception ignored){} r.release();} Log.i("HonorVoice","CLOSED");}
    }
}

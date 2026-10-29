package local.ivand.pomodoro;

import android.content.Context;
import android.media.AudioManager;
import android.os.*;
import android.speech.tts.*;
import android.util.Log;
import java.util.*;

/** Uses an installed offline Russian system voice; never requests a network voice. */
final class SpeechOutput {
    interface Listener {void gate(boolean speaking);void error(String message);}
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final AudioManager audio;
    private TextToSpeech tts;
    private boolean ready,closed,initialized;
    private String queued;
    private long sequence;
    private String active;
    private final AudioManager.OnAudioFocusChangeListener focus=change->{};
    SpeechOutput(Context context,Listener listener){
        this.listener=listener;audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        tts=new TextToSpeech(context.getApplicationContext(),status->main.post(()->initialize(status)));
    }
    private void initialize(int status){
        if(closed)return;
        if(status!=TextToSpeech.SUCCESS){initialized=true;fail("Не удалось запустить синтез речи");return;}
        // Voice discovery may cross a Binder boundary; do not block the timer's main thread.
        new Thread(()->{
            Voice chosen=null;String error=null;
            try{
                Set<Voice> voices=tts.getVoices();
                if(voices!=null)for(Voice voice:voices){
                    if(!"ru".equals(voice.getLocale().getLanguage()))continue;
                    boolean missing=voice.getFeatures()!=null&&voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
                    Log.i("HonorSpeech","RU_VOICE name="+voice.getName()+" network="+voice.isNetworkConnectionRequired()+" missing="+missing);
                    if(!voice.isNetworkConnectionRequired()&&!missing&&(chosen==null||voice.getQuality()>chosen.getQuality()))chosen=voice;
                }
                if(chosen==null)error="Русский офлайн-голос не установлен";
                else if(tts.setVoice(chosen)!=TextToSpeech.SUCCESS)error="Не удалось выбрать русский голос";
            }catch(Exception e){error="Ошибка синтеза: "+e.getMessage();}
            final Voice selected=chosen;final String problem=error;
            main.post(()->{if(closed)return;initialized=true;if(problem!=null){fail(problem);return;}
                tts.setSpeechRate(.95f);tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    public void onStart(String id){Log.i("HonorSpeech","START "+id);}
                    public void onDone(String id){main.post(()->finish(id));}
                    public void onError(String id){main.post(()->{finish(id);fail("Не удалось произнести ответ");});}
                });
                ready=true;Log.i("HonorSpeech","READY offline voice="+selected.getName());
                if(queued!=null){String text=queued;queued=null;say(text);}
            });
        },"HonorSpeech-init").start();
    }
    void say(String text){
        if(closed)return;
        if(!ready){if(!initialized){queued=text;return;}fail("Русский офлайн-голос недоступен");return;}
        String id="remaining-"+(++sequence);active=id;
        listener.gate(true);audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        Bundle params=new Bundle();params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM,AudioManager.STREAM_MUSIC);
        Log.i("HonorSpeech","SAY "+text);
        if(tts.speak(text,TextToSpeech.QUEUE_FLUSH,params,id)!=TextToSpeech.SUCCESS){finish(id);fail("Не удалось произнести ответ");return;}
        main.postDelayed(()->{if(id.equals(active)){tts.stop();finish(id);}},20000);
    }
    private void finish(String id){if(closed||!id.equals(active))return;active=null;listener.gate(false);audio.abandonAudioFocus(focus);Log.i("HonorSpeech","DONE "+id);}
    private void fail(String message){Log.w("HonorSpeech",message);listener.error(message);}
    void close(){closed=true;main.removeCallbacksAndMessages(null);listener.gate(false);audio.abandonAudioFocus(focus);if(tts!=null){tts.stop();tts.shutdown();}}
}

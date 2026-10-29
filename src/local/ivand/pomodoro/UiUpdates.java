package local.ivand.pomodoro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Same-process, main-thread delivery. Never queue timer snapshots through Android's background broadcasts. */
final class UiUpdates {
    private static BroadcastReceiver subscriber;
    static void subscribe(BroadcastReceiver receiver){subscriber=receiver;}
    static void unsubscribe(BroadcastReceiver receiver){if(subscriber==receiver)subscriber=null;}
    static void send(Context context,Intent snapshot){if(subscriber!=null)subscriber.onReceive(context,snapshot);}
}

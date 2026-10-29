package local.ivand.pomodoro;

import android.app.*;

/** Reapply immersive mode when a child dialog closes or EMUI changes keyguard focus. */
final class FullscreenDialog extends Dialog {
    FullscreenDialog(Activity owner){super(owner);}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
    @Override public void show(){super.show();immersive();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused){immersive();getWindow().getDecorView().postDelayed(()->{if(isShowing())immersive();},500);}}
}

package ru.lzmode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (MainActivity.getInstance() != null) {
            if (intent.getAction().equals(MainActivity.ACTION_PLAY)) {
                MainActivity.getInstance().play();
            } else if (intent.getAction().equals(MainActivity.ACTION_PAUSE)) {
                MainActivity.getInstance().pause();
            }
        }
    }
}

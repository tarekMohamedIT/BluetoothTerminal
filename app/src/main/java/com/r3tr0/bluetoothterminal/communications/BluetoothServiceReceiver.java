package com.r3tr0.bluetoothterminal.communications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothServiceReceiver extends BroadcastReceiver {

    OnMessageReceivedListener onMessageReceivedListener;

    public BluetoothServiceReceiver() {
    }

    public BluetoothServiceReceiver(OnMessageReceivedListener onMessageReceivedListener) {
        this.onMessageReceivedListener = onMessageReceivedListener;
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener onMessageReceivedListener) {
        this.onMessageReceivedListener = onMessageReceivedListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (onMessageReceivedListener != null){
            onMessageReceivedListener.onMessageReceived(intent);
        }

    }

    public interface OnMessageReceivedListener{
        void onMessageReceived(Object message);
    }
}

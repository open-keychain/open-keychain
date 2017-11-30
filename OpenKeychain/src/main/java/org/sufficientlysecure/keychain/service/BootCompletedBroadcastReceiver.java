package org.sufficientlysecure.keychain.service;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class BootCompletedBroadcastReceiver extends BroadcastReceiver {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        // nothing to do here, we only listen for this to receive KeychainApplication.onCreate
    }
}
package org.sufficientlysecure.keychain.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.securitytoken.NfcSweetspotData;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;


public class ShowNfcSweetspotActivity extends BaseSecurityTokenActivity {
    public static final String EXTRA_TOKEN_INFO = "token_info";

    private View sweetspotIndicator;
    private View sweetspotIcon;
    private View sweetspotCircle1;
    private View sweetspotCircle2;
    private View sweetspotCircle3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.fade_in_quick, R.anim.fade_out_quick);

        setContentView(R.layout.show_nfc_sweetspot_activity);
        sweetspotIndicator = findViewById(R.id.nfc_sweetspot_indicator);

        Pair<Double, Double> nfcPosition = NfcSweetspotData.SWEETSPOT_DATA.get(Build.MODEL);
        if (nfcPosition == null) {
            throw new IllegalArgumentException("No data available for this model. This activity should not be called!");
        }
        DisplayMetrics displayDimensions = getDisplaySize();

        final float translationX = (float) (displayDimensions.widthPixels * nfcPosition.first);
        final float translationY = (float) (displayDimensions.heightPixels * nfcPosition.second);

        sweetspotIndicator.post(new Runnable() {
            @Override
            public void run() {
                sweetspotIndicator.setTranslationX(translationX - sweetspotIndicator.getWidth() / 2);
                sweetspotIndicator.setTranslationY(translationY - sweetspotIndicator.getHeight() / 2);
            }
        });

        sweetspotIcon = findViewById(R.id.nfc_sweetspot_icon);
        sweetspotCircle1 = findViewById(R.id.nfc_sweetspot_circle_1);
        sweetspotCircle2 = findViewById(R.id.nfc_sweetspot_circle_2);
        sweetspotCircle3 = findViewById(R.id.nfc_sweetspot_circle_3);

        sweetspotIcon.setAlpha(0.0f);
        sweetspotCircle1.setAlpha(0.0f);
        sweetspotCircle2.setAlpha(0.0f);
        sweetspotCircle3.setAlpha(0.0f);
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();

        DecelerateInterpolator interpolator = new DecelerateInterpolator();
        sweetspotIcon.animate().alpha(1.0f).setInterpolator(interpolator).setDuration(300).start();
        sweetspotCircle1.animate().alpha(1.0f).setInterpolator(interpolator).setDuration(500).setStartDelay(100).start();
        sweetspotCircle2.animate().alpha(1.0f).setInterpolator(interpolator).setDuration(700).setStartDelay(200).start();
        sweetspotCircle3.animate().alpha(1.0f).setInterpolator(interpolator).setDuration(1000).setStartDelay(300).start();
    }

    @Override
    protected void initTheme() {
        // do nothing
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(R.anim.fade_in_quick, R.anim.fade_out_quick);
    }

    @Override
    protected void onSecurityTokenPostExecute(SecurityTokenConnection stConnection) {
        Intent result = new Intent();
        result.putExtra(EXTRA_TOKEN_INFO, tokenInfo);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onTouchEvent(event);
    }

    @NonNull
    private DisplayMetrics getDisplaySize() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics;
    }

    public static boolean hasSweetspotData() {
        return NfcSweetspotData.SWEETSPOT_DATA.containsKey(Build.MODEL);
    }
}

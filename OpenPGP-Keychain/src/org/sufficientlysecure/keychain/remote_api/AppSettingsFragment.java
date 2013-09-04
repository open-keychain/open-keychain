package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.SelectSecretKeyActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;

public class AppSettingsFragment extends Fragment {

    private LinearLayout advancedSettingsContainer;
    private Button advancedSettingsButton;

    private Button selectKeyButton;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.api_app_settings_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        advancedSettingsButton = (Button) getActivity().findViewById(
                R.id.api_app_settings_advanced_button);
        advancedSettingsContainer = (LinearLayout) getActivity().findViewById(
                R.id.api_app_settings_advanced);
        selectKeyButton = (Button) getActivity().findViewById(
                R.id.api_app_settings_select_key_button);

        final Animation visibleAnimation = new AlphaAnimation(0.0f, 1.0f);
        visibleAnimation.setDuration(250);
        final Animation invisibleAnimation = new AlphaAnimation(1.0f, 0.0f);
        invisibleAnimation.setDuration(250);

        // TODO: Better: collapse/expand animation
        // final Animation animation2 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        // Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
        // Animation.RELATIVE_TO_SELF, 0.0f);
        // animation2.setDuration(150);

        advancedSettingsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (advancedSettingsContainer.getVisibility() == View.VISIBLE) {
                    advancedSettingsContainer.startAnimation(invisibleAnimation);
                    advancedSettingsContainer.setVisibility(View.INVISIBLE);
                } else {
                    advancedSettingsContainer.startAnimation(visibleAnimation);
                    advancedSettingsContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        selectKeyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectSecretKey();

            }
        });
    }

    private void selectSecretKey() {
        Intent intent = new Intent(getActivity(), SelectSecretKeyActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }
}

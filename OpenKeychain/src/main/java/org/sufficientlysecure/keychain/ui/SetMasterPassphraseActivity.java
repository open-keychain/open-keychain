package org.sufficientlysecure.keychain.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;

public class SetMasterPassphraseActivity extends BaseActivity {
    public static final String FRAGMENT_TAG = "currentFragment";
    private Fragment mCurrentFragment;
    private Class mFirstFragmentClass;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.set_master_passphrase_title);
        mToolbar.setNavigationIcon(null);
        mToolbar.setNavigationOnClickListener(null);

        if (savedInstanceState != null) {
            mCurrentFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        } else {
            showDialog();
            SetMasterPassphraseFragment frag = SetMasterPassphraseFragment.newInstance(false, null);
            mFirstFragmentClass = frag.getClass();
            loadFragment(frag, FragAction.START);
        }
    }

    private void showDialog() {
        final ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(this);
        CustomAlertDialogBuilder dialog = new CustomAlertDialogBuilder(theme);
        LayoutInflater inflater = LayoutInflater.from(theme);
        View view = inflater.inflate(R.layout.first_time_dialog, null);

        dialog.setView(view)
                .setCancelable(false)
                .setTitle(R.string.first_time_dialog_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // just close
                    }
                }).show();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.set_master_passphrase_activity);
    }

    public enum FragAction {
        START,
        TO_RIGHT,
        TO_LEFT
    }

    public void loadFragment(Fragment fragment, FragAction action) {
        mCurrentFragment = fragment;

        // Add the fragment to the 'fragment_container' FrameLayout
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        switch (action) {
            case START:
                transaction.setCustomAnimations(0, 0);
                transaction.replace(R.id.set_master_passphrase_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
                break;
            case TO_LEFT:
                getSupportFragmentManager().popBackStackImmediate();
                break;
            case TO_RIGHT:
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right, R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack(null);
                transaction.replace(R.id.migrate_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
                break;

        }

        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    public void onBackPressed() {
        if(mFirstFragmentClass.equals(mCurrentFragment.getClass())) {
            ActivityCompat.finishAffinity(this);
        } else {
            super.onBackPressed();
        }
    }
}

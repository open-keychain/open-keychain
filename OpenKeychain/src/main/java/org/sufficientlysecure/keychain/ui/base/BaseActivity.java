/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.service.KeyserverSyncAdapterService;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;

/**
 * Setups Toolbar
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected Toolbar mToolbar;
    protected View mStatusBar;
    protected ThemeChanger mThemeChanger;

    public static final int REQUEST_CODE_PIN = 1;
    public static final int REQUEST_KEYRING_PASSPHRASE_FOR_PIN = 2;
    public static final int REQUEST_KEYRING_PASSPHRASE_FOR_MOVE_TO_CARD = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTheme();
        initLayout();
        initToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResumeChecks(this);

        if (mThemeChanger.changeTheme()) {
            Intent intent = getIntent();
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void onResumeChecks(Context context) {
        KeyserverSyncAdapterService.cancelUpdates(context);
        // in case user has disabled sync from Android account settings
        ContactSyncAdapterService.deleteIfSyncDisabled(context);
    }

    protected void initLayout() {

    }

    protected void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mStatusBar = findViewById(R.id.status_bar);
    }

    /**
     * Override if you want a different theme!
     */
    protected void initTheme() {
        mThemeChanger = new ThemeChanger(this);
        mThemeChanger.setThemes(R.style.Theme_Keychain_Light, R.style.Theme_Keychain_Dark);
        mThemeChanger.changeTheme();
    }

    protected void setActionBarIcon(int iconRes) {
        mToolbar.setNavigationIcon(iconRes);
    }

    /**
     * Inflate custom design to look like a full screen dialog, as specified in Material Design Guidelines
     * see http://www.google.com/design/spec/components/dialogs.html#dialogs-full-screen-dialogs
     */
    public void setFullScreenDialogDoneClose(int doneText, View.OnClickListener doneOnClickListener,
            View.OnClickListener cancelOnClickListener) {
        setActionBarIcon(R.drawable.ic_close_white_24dp);

        // Inflate the custom action bar view
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(R.layout.full_screen_dialog, null);

        TextView firstTextView = ((TextView) customActionBarView.findViewById(R.id.full_screen_dialog_done_text));
        firstTextView.setText(doneText);
        customActionBarView.findViewById(R.id.full_screen_dialog_done).setOnClickListener(
                doneOnClickListener);

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END));
        mToolbar.setNavigationOnClickListener(cancelOnClickListener);
    }

    /** Close button only */
    protected void setFullScreenDialogClose(View.OnClickListener cancelOnClickListener, boolean white) {
        if (white) {
            setActionBarIcon(R.drawable.ic_close_white_24dp);
        } else {
            setActionBarIcon(R.drawable.ic_close_black_24dp);
        }
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        mToolbar.setNavigationOnClickListener(cancelOnClickListener);
    }

    protected void setFullScreenDialogClose(View.OnClickListener cancelOnClickListener) {
        setFullScreenDialogClose(cancelOnClickListener, true);
    }

    /** Close button only, with finish-action and given return status, white. */
    protected void setFullScreenDialogClose(final int result, boolean white) {
        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(result);
                finish();
            }
        }, white);
    }

    /**
     * Inflate custom design with two buttons using drawables.
     * This does not conform to the Material Design Guidelines, but we deviate here as this is used
     * to indicate "Allow access"/"Disallow access" to the API, which must be clearly indicated
     */
    protected void setFullScreenDialogTwoButtons(int firstText, int firstDrawableId, View.OnClickListener firstOnClickListener,
                                                 int secondText, int secondDrawableId, View.OnClickListener secondOnClickListener) {

        // Inflate the custom action bar view
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.full_screen_dialog_2, null);

        TextView firstTextView = ((TextView) customActionBarView.findViewById(R.id.actionbar_done_text));
        firstTextView.setText(firstText);
        firstTextView.setCompoundDrawablesWithIntrinsicBounds(firstDrawableId, 0, 0, 0);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                firstOnClickListener);
        TextView secondTextView = ((TextView) customActionBarView.findViewById(R.id.actionbar_cancel_text));
        secondTextView.setText(secondText);
        secondTextView.setCompoundDrawablesWithIntrinsicBounds(secondDrawableId, 0, 0, 0);
        customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                secondOnClickListener);

        // Show the custom action bar view and hide the normal Home icon and title.
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

}

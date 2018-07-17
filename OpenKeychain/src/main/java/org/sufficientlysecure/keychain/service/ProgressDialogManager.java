/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.service;


import android.app.ProgressDialog;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.os.CancellationSignal;

import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;


public class ProgressDialogManager {
    public static final String TAG_PROGRESS_DIALOG = "progressDialog";

    private FragmentActivity activity;

    public ProgressDialogManager(FragmentActivity activity) {
        this.activity = activity;
    }

    public void showProgressDialog() {
        showProgressDialog("", ProgressDialog.STYLE_SPINNER, null);
    }

    public void showProgressDialog(
            String progressDialogMessage, int progressDialogStyle, CancellationSignal cancellationSignal) {

        final ProgressDialogFragment frag = ProgressDialogFragment.newInstance(
                progressDialogMessage, progressDialogStyle, cancellationSignal != null);

        frag.setCancellationSignal(cancellationSignal);

        // TODO: This is a hack!, see
        // http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
        final FragmentManager manager = activity.getSupportFragmentManager();
        Handler handler = new Handler();
        handler.post(() -> frag.show(manager, TAG_PROGRESS_DIALOG));

    }

    public void setPreventCancel() {
        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag(TAG_PROGRESS_DIALOG);

        if (progressDialogFragment == null) {
            return;
        }

        progressDialogFragment.setPreventCancel();
    }

    public void dismissAllowingStateLoss() {
        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag(TAG_PROGRESS_DIALOG);

        if (progressDialogFragment == null) {
            return;
        }

        progressDialogFragment.dismissAllowingStateLoss();
    }

    public void onSetProgress(Integer resourceInt, int progress, int max) {
        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag(TAG_PROGRESS_DIALOG);

        if (progressDialogFragment == null) {
            return;
        }

        if (resourceInt != null) {
            progressDialogFragment.setProgress(resourceInt, progress, max);
        } else {
            progressDialogFragment.setProgress(progress, max);
        }
    }
}

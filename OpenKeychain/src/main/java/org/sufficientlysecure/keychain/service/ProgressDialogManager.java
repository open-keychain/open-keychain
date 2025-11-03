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
import android.os.Looper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.core.os.CancellationSignal;
import timber.log.Timber;

import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;


public class ProgressDialogManager {
    public static final String TAG_PROGRESS_DIALOG = "progressDialog";

    private FragmentActivity activity;
    private boolean isDismissed = false;
    private ProgressDialogFragment cachedFragment = null;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public ProgressDialogManager(FragmentActivity activity) {
        this.activity = activity;
    }

    public void showProgressDialog() {
        showProgressDialog("", ProgressDialog.STYLE_SPINNER, null);
    }

    public void showProgressDialog(
            String progressDialogMessage, int progressDialogStyle, CancellationSignal cancellationSignal) {
        if (isDismissed || activity == null || activity.isFinishing()) {
            return;
        }

        // Ensure execution on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> showProgressDialogInternal(progressDialogMessage, progressDialogStyle, cancellationSignal));
            return;
        }

        showProgressDialogInternal(progressDialogMessage, progressDialogStyle, cancellationSignal);
    }

    private void showProgressDialogInternal(
            String progressDialogMessage, int progressDialogStyle, CancellationSignal cancellationSignal) {
        if (isDismissed || activity == null || activity.isFinishing()) {
            return;
        }

        try {
            final ProgressDialogFragment frag = ProgressDialogFragment.newInstance(
                    progressDialogMessage, progressDialogStyle, cancellationSignal != null);

            frag.setCancellationSignal(cancellationSignal);
            cachedFragment = frag;

            // Use a Handler to ensure the dialog is displayed at the right time.
            mainHandler.post(() -> {
                if (isDismissed || activity == null || activity.isFinishing()) {
                    return;
                }

                try {
                    // TODO: This is a hack!, see
                    // http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
                    final FragmentManager manager = activity.getSupportFragmentManager();
                    if (manager != null && !manager.isStateSaved()) {
                        frag.show(manager, TAG_PROGRESS_DIALOG);
                    }
                } catch (Exception e) {
                    Timber.e(e, "Error showing progress dialog");
                }
            });

        } catch (Exception e) {
            Timber.e(e, "Error creating progress dialog");
        }
    }

    public void setPreventCancel() {
        if (isDismissed || activity == null || activity.isFinishing()) {
            return;
        }

        try {
            ProgressDialogFragment progressDialogFragment = getProgressDialogFragment();
            if (progressDialogFragment != null) {
                progressDialogFragment.setPreventCancel();
            }
        } catch (Exception e) {
            Timber.e(e, "Error setting prevent cancel");
        }
    }

    public void dismissAllowingStateLoss() {
        isDismissed = true;

        // Ensure execution on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::dismissInternal);
            return;
        }

        dismissInternal();
    }

    private void dismissInternal() {
        try {
            ProgressDialogFragment progressDialogFragment = getProgressDialogFragment();
            if (progressDialogFragment != null) {
                progressDialogFragment.dismissAllowingStateLoss();
            }
            cachedFragment = null;
        } catch (Exception e) {
            Timber.e(e, "Error dismissing progress dialog");
        }
    }

    public void onSetProgress(Integer resourceInt, int progress, int max) {
        if (isDismissed || activity == null || activity.isFinishing()) {
            return;
        }

        try {
            ProgressDialogFragment progressDialogFragment = getProgressDialogFragment();
            if (progressDialogFragment != null) {
                if (resourceInt != null) {
                    progressDialogFragment.setProgress(resourceInt, progress, max);
                } else {
                    progressDialogFragment.setProgress(progress, max);
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Error setting progress");
        }
    }

    private ProgressDialogFragment getProgressDialogFragment() {
        if (cachedFragment != null) {
            return cachedFragment;
        }

        try {
            if (activity != null && activity.getSupportFragmentManager() != null) {
                return (ProgressDialogFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag(TAG_PROGRESS_DIALOG);
            }
        } catch (Exception e) {
            Timber.e(e, "Error finding progress dialog fragment");
        }

        return null;
    }
}
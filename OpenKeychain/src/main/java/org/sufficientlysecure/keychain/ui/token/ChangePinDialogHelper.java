package org.sufficientlysecure.keychain.ui.token;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.support.annotation.CheckResult;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenContract.ManageSecurityTokenMvpPresenter;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;


class ChangePinDialogHelper {
    @CheckResult
    static AlertDialog createAdminPinDialog(Context context, final ManageSecurityTokenMvpPresenter presenter) {
        ContextThemeWrapper themedContext = ThemeChanger.getDialogThemeWrapper(context);

        @SuppressLint("InflateParams") // it's a dialog, no root element
        View view = LayoutInflater.from(themedContext).inflate(R.layout.admin_pin_dialog, null, false);
        final EditText adminPin = (EditText) view.findViewById(R.id.admin_pin_current);
        final EditText newPin = (EditText) view.findViewById(R.id.pin_new);
        final EditText newPinRepeat = (EditText) view.findViewById(R.id.pin_new_repeat);

        AlertDialog dialog = new Builder(themedContext)
                .setView(view)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.token_unlock_ok, null).create();
        dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        checkAndHandleInput(adminPin, newPin, newPinRepeat, dialog, presenter);
                    }
                });
            }
        });

        return dialog;
    }

    private static void checkAndHandleInput(EditText adminPinView, EditText newPinView, EditText newPinRepeatView,
            DialogInterface dialog, ManageSecurityTokenMvpPresenter presenter) {
        String adminPin = adminPinView.getText().toString();
        String newPin = newPinView.getText().toString();
        String newPinRepeat = newPinRepeatView.getText().toString();

        if (adminPin.length() < 8) {
            adminPinView.setError(adminPinView.getContext().getString(R.string.token_error_admin_min8));
            return;
        }

        if (newPin.length() < 6) {
            newPinView.setError(newPinView.getContext().getString(R.string.token_error_pin_min6));
            return;
        }

        if (!newPin.equals(newPinRepeat)) {
            newPinRepeatView.setError(newPinRepeatView.getContext().getString(R.string.token_error_pin_repeat));
            return;
        }

        dialog.dismiss();
        presenter.onInputAdminPin(adminPin, newPin);
    }
}

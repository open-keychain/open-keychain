package org.sufficientlysecure.keychain.ui.passphrasedialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.util.Passphrase;

public class BackupCodePassphraseDialogFragment extends BasePassphraseDialogFragment {
    private EditText[] mBackupCodeEditText;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

        // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
        //alert.setTitle()

        LayoutInflater inflater = LayoutInflater.from(theme);
        View view = inflater.inflate(R.layout.passphrase_dialog_backup_code, null);
        alert.setView(view);

        mBackupCodeEditText = new EditText[6];
        mBackupCodeEditText[0] = (EditText) view.findViewById(R.id.backup_code_1);
        mBackupCodeEditText[1] = (EditText) view.findViewById(R.id.backup_code_2);
        mBackupCodeEditText[2] = (EditText) view.findViewById(R.id.backup_code_3);
        mBackupCodeEditText[3] = (EditText) view.findViewById(R.id.backup_code_4);
        mBackupCodeEditText[4] = (EditText) view.findViewById(R.id.backup_code_5);
        mBackupCodeEditText[5] = (EditText) view.findViewById(R.id.backup_code_6);

        setupEditTextFocusNext(mBackupCodeEditText);

        AlertDialog dialog = alert.create();
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                activity.getString(R.string.btn_unlock), (DialogInterface.OnClickListener) null);
        return dialog;
    }

    private static void setupEditTextFocusNext(final EditText[] backupCodes) {
        for (int i = 0; i < backupCodes.length - 1; i++) {

            final int next = i + 1;

            backupCodes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    boolean inserting = before < count;
                    boolean cursorAtEnd = (start + count) == 4;

                    if (inserting && cursorAtEnd) {
                        backupCodes[next].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Override the default behavior so the dialog is NOT dismissed on click
        final Button positive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder backupCodeInput = new StringBuilder(26);
                for (EditText editText : mBackupCodeEditText) {
                    if (editText.getText().length() < 4) {
                        return;
                    }
                    backupCodeInput.append(editText.getText());
                    backupCodeInput.append('-');
                }
                backupCodeInput.deleteCharAt(backupCodeInput.length() - 1);

                Passphrase passphrase = new Passphrase(backupCodeInput.toString());
                returnWithPassphrase(passphrase);
            }
        });
    }
}

/*
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

package org.sufficientlysecure.keychain.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Window;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class RedirectImportKeysActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.redirect_import_keys_activity);

        startQrCodeCaptureActivity();
    }

    private void startQrCodeCaptureActivity() {
        final Intent intent = new Intent(this, QrCodeCaptureActivity.class);

        new AlertDialog.Builder(this)
                .setTitle(R.string.redirect_import_key_title)
                .setMessage(R.string.redirect_import_key_message)
                .setPositiveButton(R.string.redirect_import_key_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // intent directly to ImportKeyChain activity
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.redirect_import_key_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // close window
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
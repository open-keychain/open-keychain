/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import java.util.ArrayList;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.dialog.ShareQrCodeDialogFragment;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ShareActivity extends SherlockFragmentActivity {
    // Actions for internal use only:
    public static final String ACTION_SHARE_KEYRING = Constants.INTENT_PREFIX + "SHARE_KEYRING";
    public static final String ACTION_SHARE_KEYRING_WITH_QR_CODE = Constants.INTENT_PREFIX
            + "SHARE_KEYRING_WITH_QR_CODE";

    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleActions(getIntent());
    }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        long masterKeyId = extras.getLong(EXTRA_MASTER_KEY_ID);

        // get public keyring as ascii armored string
        ArrayList<String> keyringArmored = ProviderHelper.getPublicKeyRingsAsArmoredString(this,
                new long[] { masterKeyId });

        if (ACTION_SHARE_KEYRING.equals(action)) {
            // let user choose application
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, keyringArmored.get(0));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent,
                    getResources().getText(R.string.action_share_key_with)));
        } else if (ACTION_SHARE_KEYRING_WITH_QR_CODE.equals(action)) {
            ShareQrCodeDialogFragment dialog = ShareQrCodeDialogFragment.newInstance(keyringArmored
                    .get(0));
            dialog.show(getSupportFragmentManager(), "qrCodeShareDialog");
        }

        // close this activity
        // TODO: finish() would also close dialog...
        // integrate this into new KeyViewActivity when ready
        // finish();
    }
}

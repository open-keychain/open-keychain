/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.net.Uri;

import java.util.ArrayList;

public interface EncryptActivityInterface {

    public interface UpdateListener {
        void onNotifyUpdate();
    }

    public boolean isUseArmor();

    public long getSignatureKey();
    public long[] getEncryptionKeys();
    public String[] getEncryptionUsers();
    public void setSignatureKey(long signatureKey);
    public void setEncryptionKeys(long[] encryptionKeys);
    public void setEncryptionUsers(String[] encryptionUsers);

    public void setPassphrase(String passphrase);

    // ArrayList on purpose as only those are parcelable
    public ArrayList<Uri> getInputUris();
    public ArrayList<Uri> getOutputUris();
    public void setInputUris(ArrayList<Uri> uris);
    public void setOutputUris(ArrayList<Uri> uris);

    public String getMessage();
    public void setMessage(String message);

    /**
     * Call this to notify the UI for changes done on the array lists or arrays,
     * automatically called if setter is used
     */
    public void notifyUpdate();

    public void startEncrypt(boolean share);
}

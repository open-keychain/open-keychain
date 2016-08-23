/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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

package org.sufficientlysecure.keychain.operations;

import android.content.Context;
import android.support.annotation.NonNull;
import org.sufficientlysecure.keychain.operations.results.ChangePassphraseWorkflowResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ChangePassphraseWorkflowParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

/**
 * This operation changes the passphrase workflow of the application.
 * The available workflows are as mentioned below.
 *
 * Single passphrase workflow:
 * The user only uses the master passphrase for all passphrase requiring operations.
 * This is achieved by using a intermediary symmetric key. The master passphrase provided by
 * the user is used to encrypt this intermediary key. The intermediary key is used to encrypt
 * all secret keyrings.
 *
 * Multi-passphrase workflow:
 * The user uses the master passphrase only for the applock, managing the master passphrase and
 * changing the passphrase workflow. All other keyrings are encrypted using individual passphrases.
 *
 * Consequently, we re-encrypt all secret keys when going from single to multi & vice versa.
 */
public class ChangePassphraseWorkflowOperation extends BaseOperation<ChangePassphraseWorkflowParcel> {
    public static final String CACHE_FILE_NAME = "change_workflow.pcl";

    public ChangePassphraseWorkflowOperation(Context context, ProviderHelper providerHelper,
                                             Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public ChangePassphraseWorkflowResult execute(ChangePassphraseWorkflowParcel changeParcel,
                                                  CryptoInputParcel cryptoInputParcel) {
        mProgressable.setPreventCancel();
        return mProviderHelper.changePassphraseWorkflowOperation(
                mProgressable,
                CACHE_FILE_NAME,
                changeParcel.mPassphrases,
                changeParcel.mMasterPassphrase,
                changeParcel.mToSinglePassphraseWorkflow
        );
    }

}

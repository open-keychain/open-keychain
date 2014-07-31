/*
 * Copyright (C) Art O Cathain
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

package org.sufficientlysecure.keychain.support;

import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.pgp.WrappedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;

/**
 * Created by art on 21/06/14.
 */
class ProviderHelperStub extends ProviderHelper {
    public ProviderHelperStub(Context context) {
        super(context);
    }

    @Override
    public WrappedPublicKeyRing getCanonicalizedPublicKeyRing(Uri id) throws NotFoundException {
        byte[] data = TestDataUtil.readFully(getClass().getResourceAsStream("/public-key-for-sample.blob"));
        return new WrappedPublicKeyRing(data, false, 0);
    }
}

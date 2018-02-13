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

package org.sufficientlysecure.keychain.securitytoken;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.math.ec.ECCurve;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;


// 4.3.3.6 Algorithm Attributes
public class EdDSAKeyFormat extends KeyFormat {

    public EdDSAKeyFormat() {
        super(KeyFormatType.EdDSAKeyFormatType);
    }

    @Override
    public void addToSaveKeyringParcel(SaveKeyringParcel.Builder builder, int keyFlags) {
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(SaveKeyringParcel.Algorithm.EDDSA,
                null, null, keyFlags, 0L));
    }
}

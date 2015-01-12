/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.UserAttributeSubpacketTags;
import org.spongycastle.openpgp.PGPUserAttributeSubpacketVector;

public class WrappedUserAttribute {

    public static final int UAT_UNKNOWN = 0;
    public static final int UAT_IMAGE = UserAttributeSubpacketTags.IMAGE_ATTRIBUTE;
    public static final int UAT_LINKED_ID = 100;

    final private PGPUserAttributeSubpacketVector mVector;

    WrappedUserAttribute(PGPUserAttributeSubpacketVector vector) {
        mVector = vector;
    }

    PGPUserAttributeSubpacketVector getVector() {
        return mVector;
    }

    public int getType() {
        if (mVector.getSubpacket(UserAttributeSubpacketTags.IMAGE_ATTRIBUTE) != null) {
            return UAT_IMAGE;
        }
        return 0;
    }

}

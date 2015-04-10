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

package org.sufficientlysecure.keychain.util;

import org.sufficientlysecure.keychain.pgp.Progressable;

/** This is a simple variant of ProgressScaler which shows a fixed progress message, ignoring
 * the provided ones.
 */
public class ProgressFixedScaler extends ProgressScaler {

    final int mResId;

    public ProgressFixedScaler(Progressable wrapped, int from, int to, int max, int resId) {
        super(wrapped, from, to, max);
        mResId = resId;
    }

    public void setProgress(int resourceId, int progress, int max) {
        if (mWrapped != null) {
            mWrapped.setProgress(mResId, mFrom + progress * (mTo - mFrom) / max, mMax);
        }
    }

    public void setProgress(String message, int progress, int max) {
        if (mWrapped != null) {
            mWrapped.setProgress(mResId, mFrom + progress * (mTo - mFrom) / max, mMax);
        }
    }

}

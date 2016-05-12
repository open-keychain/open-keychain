/*
 * Copyright (C) 2015-2016 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.linked;

import java.net.URI;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

public abstract class LinkedResource {

    public abstract URI toUri();

    public abstract @DrawableRes int getDisplayIcon();
    public abstract @StringRes int getVerifiedText(boolean isSecret);
    public abstract String getDisplayTitle(Context context);
    public abstract String getDisplayComment(Context context);
    public boolean isViewable() {
        return false;
    }
    public Intent getViewIntent() {
        return null;
    }

}

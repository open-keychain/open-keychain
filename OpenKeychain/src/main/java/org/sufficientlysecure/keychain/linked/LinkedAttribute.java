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
import android.support.annotation.DrawableRes;

public class LinkedAttribute extends UriAttribute {

    public final LinkedResource mResource;

    protected LinkedAttribute(URI uri, LinkedResource resource) {
        super(uri);
        if (resource == null) {
            throw new AssertionError("resource must not be null in a LinkedIdentity!");
        }
        mResource = resource;
    }

    public @DrawableRes int getDisplayIcon() {
        return mResource.getDisplayIcon();
    }

    public String getDisplayTitle(Context context) {
        return mResource.getDisplayTitle(context);
    }

    public String getDisplayComment(Context context) {
        return mResource.getDisplayComment(context);
    }

}

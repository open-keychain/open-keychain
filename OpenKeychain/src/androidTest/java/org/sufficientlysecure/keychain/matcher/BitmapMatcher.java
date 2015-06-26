/*
 * Copyright (C) 2015 Vincent Breitmoser <look@my.amazin.horse>
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
 *
 * From the droidcon anroid espresso repository.
 * https://github.com/xrigau/droidcon-android-espresso/
 *
 */

package org.sufficientlysecure.keychain.matcher;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;


public class BitmapMatcher extends TypeSafeMatcher<View> {

    private final Bitmap mBitmap;

    public BitmapMatcher(Bitmap bitmap) {
        super(View.class);
        mBitmap = bitmap;
    }

    @Override
    public boolean matchesSafely(View view) {
        if ( !(view instanceof ImageView) ) {
            return false;
        }
        Drawable drawable = ((ImageView) view).getDrawable();
        return drawable != null && (drawable instanceof BitmapDrawable)
                && ((BitmapDrawable) drawable).getBitmap().sameAs(mBitmap);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with equivalent specified bitmap");
    }

    public static BitmapMatcher withBitmap(Bitmap bitmap) {
        return new BitmapMatcher(bitmap);
    }

}

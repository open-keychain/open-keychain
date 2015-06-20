/*
 * Copyright (C) 2015 Xavi Rigau <xrigau@gmail.com>
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


import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;


public class DrawableMatcher extends TypeSafeMatcher<View> {

    private final int mResourceId;
    private final boolean mIgnoreFilters;

    public DrawableMatcher(int resourceId, boolean ignoreFilters) {
        super(View.class);
        mResourceId = resourceId;
        mIgnoreFilters = ignoreFilters;
    }

    private String resourceName = null;
    private Drawable expectedDrawable = null;

    @Override
    public boolean matchesSafely(View target) {
        if (expectedDrawable == null) {
            loadDrawableFromResources(target.getResources());
        }
        if (invalidExpectedDrawable()) {
            return false;
        }

        if (target instanceof ImageView) {
            return hasImage((ImageView) target) || hasBackground(target);
        }
        if (target instanceof TextView) {
            return hasCompoundDrawable((TextView) target) || hasBackground(target);
        }
        return hasBackground(target);
    }

    private void loadDrawableFromResources(Resources resources) {
        try {
            expectedDrawable = resources.getDrawable(mResourceId);
            resourceName = resources.getResourceEntryName(mResourceId);
        } catch (Resources.NotFoundException ignored) {
            // view could be from a context unaware of the resource id.
        }
    }

    private boolean invalidExpectedDrawable() {
        return expectedDrawable == null;
    }

    private boolean hasImage(ImageView target) {
        return isSameDrawable(target.getDrawable());
    }

    private boolean hasCompoundDrawable(TextView target) {
        for (Drawable drawable : target.getCompoundDrawables()) {
            if (isSameDrawable(drawable)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBackground(View target) {
        return isSameDrawable(target.getBackground());
    }

    private boolean isSameDrawable(Drawable drawable) {
        if (drawable == null) {
            return false;
        }
        // if those are both bitmap drawables, compare their bitmaps (ignores color filters, which is what we want!)
        if (mIgnoreFilters && drawable instanceof BitmapDrawable && expectedDrawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap().sameAs((((BitmapDrawable) expectedDrawable).getBitmap()));
        }
        return expectedDrawable.getConstantState().equals(drawable.getConstantState());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with drawable from resource id: ");
        description.appendValue(mResourceId);
        if (resourceName != null) {
            description.appendText("[");
            description.appendText(resourceName);
            description.appendText("]");
        }
    }

    public static DrawableMatcher withDrawable(int resourceId, boolean ignoreFilters) {
        return new DrawableMatcher(resourceId, ignoreFilters);
    }
    public static DrawableMatcher withDrawable(int resourceId) {
        return new DrawableMatcher(resourceId, true);
    }

}

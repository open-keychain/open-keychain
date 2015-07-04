/*
 * Copyright (C) 2015 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.ui.util;

import android.content.Context;
import android.view.ContextThemeWrapper;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Preferences;

public class ThemeChanger {
    private Context mContext;
    private Preferences mPreferences;
    private String mCurrentTheme = null;

    public ThemeChanger(Context context) {
        mContext = context;
        mPreferences = Preferences.getPreferences(mContext);
    }

    /**
     * Apply the theme set in preferences if it isn't equal to mCurrentTheme
     * anymore or mCurrentTheme hasn't been set yet.
     * If a new theme is applied in this method, then return true, so
     * the caller can re-create the activity, if need be.
     */
    public boolean changeTheme() {
        String newTheme = mPreferences.getTheme();
        if (mCurrentTheme != null && mCurrentTheme.equals(newTheme)) {
            return false;
        }

        int themeId = R.style.LightTheme;
        if (Constants.Pref.Theme.DARK.equals(newTheme)) {
            themeId = R.style.DarkTheme;
        }

        ContextThemeWrapper w = new ContextThemeWrapper(mContext, themeId);
        mContext.getTheme().setTo(w.getTheme());
        mCurrentTheme = newTheme;

        return true;
    }
}

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.util;

import android.app.Activity;
import android.content.res.Resources;

import com.github.johnpersano.supertoasts.SuperCardToast;
import com.github.johnpersano.supertoasts.SuperToast;

/**
 * @author danielhass
 * Notify wrapper which allows a more easy use of different notification libraries
 */
public class Notify {

    public static enum Style {OK, WARN, INFO, ERROR}

    /**
     * Shows a simple in-layout notification with the CharSequence given as parameter
     * @param activity
     * @param text     Text to show
     * @param style    Notification styling
     */
    public static void showNotify(Activity activity, CharSequence text, Style style) {

        SuperCardToast st = new SuperCardToast(activity);
        st.setText(text);
        st.setDuration(SuperToast.Duration.MEDIUM);
        switch (style){
            case OK:
                st.setBackground(SuperToast.Background.GREEN);
                break;
            case WARN:
                st.setBackground(SuperToast.Background.ORANGE);
                break;
            case ERROR:
                st.setBackground(SuperToast.Background.RED);
                break;
        }
        st.show();

    }

    /**
     * Shows a simple in-layout notification with the resource text from given id
     * @param activity
     * @param resId    ResourceId of notification text
     * @param style    Notification styling
     * @throws Resources.NotFoundException
     */
    public static void showNotify(Activity activity, int resId, Style style) throws Resources.NotFoundException {
        showNotify(activity, activity.getResources().getText(resId), style);
    }
}
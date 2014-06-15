/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Senecaso
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui.adapter;

import android.app.Activity;
import android.content.res.Resources;

import com.github.johnpersano.supertoasts.SuperCardToast;
import com.github.johnpersano.supertoasts.SuperToast;

/**
 * @author danielhass
 * Notify wrapper which allows a more easy use of different notification libraries
 */
public class Notify {

    public static enum Style {OK, WARN, ERROR}

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
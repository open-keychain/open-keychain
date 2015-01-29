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

package org.sufficientlysecure.keychain.ui.util;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.Snackbar.SnackbarDuration;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

/**
 * Notify wrapper which allows a more easy use of different notification libraries
 */
public class Notify {

    public static enum Style {OK, WARN, INFO, ERROR}

    public static final int LENGTH_INDEFINITE = 0;
    public static final int LENGTH_LONG = 3500;

    /**
     * Shows a simple in-layout notification with the CharSequence given as parameter
     * @param activity
     * @param text     Text to show
     * @param style    Notification styling
     */
    public static void showNotify(Activity activity, CharSequence text, Style style) {

        Snackbar bar = Snackbar.with(activity)
                .text(text)
                .duration(SnackbarDuration.LENGTH_LONG);

        switch (style) {
            case OK:
                break;
            case WARN:
                bar.textColor(Color.YELLOW);
                break;
            case ERROR:
                bar.textColor(Color.RED);
                break;
        }

        SnackbarManager.show(bar);

    }

    public static Showable createNotify (Activity activity, int resId, int duration, Style style) {
        final Snackbar bar = Snackbar.with(activity)
                .text(resId);
        if (duration == LENGTH_INDEFINITE) {
            bar.duration(SnackbarDuration.LENGTH_INDEFINITE);
        } else {
            bar.duration(duration);
        }

        switch (style) {
            case OK:
                bar.actionColor(Color.GREEN);
                break;
            case WARN:
                bar.textColor(Color.YELLOW);
                break;
            case ERROR:
                bar.textColor(Color.RED);
                break;
        }

        return new Showable () {
            @Override
            public void show() {
                SnackbarManager.show(bar);
            }
        };
    }

    public static Showable createNotify(Activity activity, int resId, int duration, Style style,
                                        final ActionListener listener, int resIdAction) {
        return createNotify(activity, activity.getString(resId), duration, style, listener, resIdAction);
    }

    public static Showable createNotify(Activity activity, String msg, int duration, Style style,
                                        final ActionListener listener, int resIdAction) {
        final Snackbar bar = Snackbar.with(activity)
                .text(msg)
                .actionLabel(resIdAction)
                .actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        listener.onAction();
                    }
                });
        if (duration == LENGTH_INDEFINITE) {
            bar.duration(SnackbarDuration.LENGTH_INDEFINITE);
        } else {
            bar.duration(duration);
        }

        switch (style) {
            case OK:
                bar.actionColor(Color.GREEN);
                break;
            case WARN:
                bar.textColor(Color.YELLOW);
                break;
            case ERROR:
                bar.textColor(Color.RED);
                break;
        }

        return new Showable () {
            @Override
            public void show() {
                SnackbarManager.show(bar);
            }
        };

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

    public interface Showable {
        public void show();

    }

    public interface ActionListener {
        public void onAction();

    }

}
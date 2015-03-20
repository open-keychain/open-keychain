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
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.Snackbar.SnackbarDuration;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListenerAdapter;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.FabContainer;

/**
 * Notify wrapper which allows a more easy use of different notification libraries
 */
public class Notify {

    public static enum Style {OK, WARN, ERROR}

    public static final int LENGTH_INDEFINITE = 0;
    public static final int LENGTH_LONG = 3500;

    public static Showable create(final Activity activity, String text, int duration, Style style,
                                  final ActionListener actionListener, int actionResId) {
        final Snackbar snackbar = Snackbar.with(activity)
                .type(SnackbarType.MULTI_LINE)
                .text(text);

        if (duration == LENGTH_INDEFINITE) {
            snackbar.duration(SnackbarDuration.LENGTH_INDEFINITE);
        } else {
            snackbar.duration(duration);
        }

        switch (style) {
            case OK:
                snackbar.actionColorResource(R.color.android_green_light);
                break;

            case WARN:
                snackbar.textColorResource(R.color.android_orange_light);
                break;

            case ERROR:
                snackbar.textColorResource(R.color.android_red_light);
                break;
        }

        if (actionListener != null) {
            snackbar.actionLabel(actionResId)
                    .actionListener(new ActionClickListener() {
                        @Override
                        public void onActionClicked(Snackbar snackbar) {
                            actionListener.onAction();
                        }
                    });
        }

        if (activity instanceof FabContainer) {
            snackbar.eventListener(new EventListenerAdapter() {
                @Override
                public void onShow(Snackbar snackbar) {
                    ((FabContainer) activity).fabMoveUp(snackbar.getHeight());
                }

                @Override
                public void onDismiss(Snackbar snackbar) {
                    ((FabContainer) activity).fabRestorePosition();
                }
            });
        }

        return new Showable() {
            @Override
            public void show() {
                SnackbarManager.show(snackbar, activity);
            }

            @Override
            public void show(Fragment fragment) {
                if (fragment != null) {
                    View view = fragment.getView();

                    if (view != null && view instanceof ViewGroup) {
                        SnackbarManager.show(snackbar, (ViewGroup) view);
                        return;
                    }
                }

                show();
            }

            @Override
            public void show(ViewGroup viewGroup) {
                if (viewGroup != null) {
                    SnackbarManager.show(snackbar, viewGroup);
                    return;
                }

                show();
            }
        };
    }

    public static Showable create(Activity activity, String text, int duration, Style style) {
        return create(activity, text, duration, style, null, -1);
    }

    public static Showable create(Activity activity, String text, Style style) {
        return create(activity, text, LENGTH_LONG, style);
    }

    public static Showable create(Activity activity, int textResId, int duration, Style style,
                                  ActionListener actionListener, int actionResId) {
        return create(activity, activity.getString(textResId), duration, style, actionListener, actionResId);
    }

    public static Showable create(Activity activity, int textResId, int duration, Style style) {
        return create(activity, activity.getString(textResId), duration, style);
    }

    public static Showable create(Activity activity, int textResId, Style style) {
        return create(activity, activity.getString(textResId), style);
    }

    public interface Showable {

        public void show();

        public void show(Fragment fragment);

        public void show(ViewGroup viewGroup);

    }

    public interface ActionListener {

        public void onAction();

    }

}

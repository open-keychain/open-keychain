package org.sufficientlysecure.keychain.ui.util;

/**
 * Created by rohan on 20/9/15.
 */
/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;
public class ContentDescriptionHint {
    private static final int ESTIMATED_TOAST_HEIGHT_DIPS = 48;
    public static void setup(View view) {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return showLongClickText(view, view.getContentDescription());
            }
        });
    }

    public static void setup(View view, final int textResId) {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return showLongClickText(view, view.getContext().getString(textResId));
            }
        });
    }

    public static void setup(View view, final CharSequence text) {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return showLongClickText(view, text);
            }
        });
    }

    public static void remove(final View view) {
        view.setOnLongClickListener(null);
    }

    private static boolean showLongClickText(View view, CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }

        final int[] screenPos = new int[2]; // origin is device display
        final Rect displayFrame = new Rect(); // includes decorations (e.g. status bar)
        view.getLocationOnScreen(screenPos);
        view.getWindowVisibleDisplayFrame(displayFrame);

        final Context context = view.getContext();
        final int viewWidth = view.getWidth();
        final int viewHeight = view.getHeight();
        final int viewCenterX = screenPos[0] + viewWidth / 2;
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        final int estimatedToastHeight = (int) (ESTIMATED_TOAST_HEIGHT_DIPS
                * context.getResources().getDisplayMetrics().density);

        Toast longClickText = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        boolean showBelow = screenPos[1] < estimatedToastHeight;
        if (showBelow) {
            // Show below
            // Offsets are after decorations (e.g. status bar) are factored in
            longClickText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    viewCenterX - screenWidth / 2,
                    screenPos[1] - displayFrame.top + viewHeight);
        } else {
            // Show above
            // Offsets are after decorations (e.g. status bar) are factored in
            // NOTE: We can't use Gravity.BOTTOM because when the keyboard is up
            // its height isn't factored in.
            longClickText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    viewCenterX - screenWidth / 2,
                    screenPos[1] - displayFrame.top - estimatedToastHeight);
        }

        longClickText.show();
        return true;
    }

}

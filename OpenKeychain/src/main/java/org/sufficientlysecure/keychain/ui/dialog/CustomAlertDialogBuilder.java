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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

/**
 * This class extends AlertDiaog.Builder, styling the header using emphasis color.
 * Note that this class is a huge hack, because dialog boxes aren't easily stylable.
 * Also, the dialog NEEDS to be called with show() directly, not create(), otherwise
 * the order of internal operations will lead to a crash!
 */
public class CustomAlertDialogBuilder extends AlertDialog.Builder {

    public CustomAlertDialogBuilder(Context context) {
        super(context);
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();

        int dividerId = dialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
        View divider = dialog.findViewById(dividerId);
        if (divider != null) {
            divider.setBackgroundColor(dialog.getContext().getResources().getColor(R.color.header_text));
        }

        int textViewId = dialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
        TextView tv = (TextView) dialog.findViewById(textViewId);
        if (tv != null) {
            tv.setTextColor(dialog.getContext().getResources().getColor(R.color.header_text));
        }

        return dialog;
    }

}

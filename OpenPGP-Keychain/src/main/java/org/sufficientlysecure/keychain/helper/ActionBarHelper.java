/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.helper;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class ActionBarHelper {

    /**
     * Set actionbar without home button if called from another app
     * 
     * @param activity
     */
    public static void setBackButton(ActionBarActivity activity) {
        // set actionbar without home button if called from another app
        final ActionBar actionBar = activity.getSupportActionBar();
        Log.d(Constants.TAG, "calling package (only set when using startActivityForResult)="
                + activity.getCallingPackage());
        if (activity.getCallingPackage() != null
                && activity.getCallingPackage().equals(Constants.PACKAGE_NAME)) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }
    }

    /**
     * Sets custom view on ActionBar for Done/Cancel activities
     * 
     * @param actionBar
     * @param firstText
     * @param firstDrawableId
     * @param firstOnClickListener
     * @param secondText
     * @param secondDrawableId
     * @param secondOnClickListener
     */
    public static void setTwoButtonView(ActionBar actionBar, int firstText, int firstDrawableId,
            OnClickListener firstOnClickListener, int secondText, int secondDrawableId,
            OnClickListener secondOnClickListener) {

        // Inflate the custom action bar view
        final LayoutInflater inflater = (LayoutInflater) actionBar.getThemedContext()
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_view_done_cancel, null);

        TextView firstTextView = ((TextView) customActionBarView.findViewById(R.id.actionbar_done_text));
        firstTextView.setText(firstText);
        firstTextView.setCompoundDrawablesWithIntrinsicBounds(firstDrawableId, 0, 0, 0);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                firstOnClickListener);
        TextView secondTextView = ((TextView) customActionBarView.findViewById(R.id.actionbar_cancel_text));
        secondTextView.setText(secondText);
        secondTextView.setCompoundDrawablesWithIntrinsicBounds(secondDrawableId, 0, 0, 0);
        customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                secondOnClickListener);

        // Show the custom action bar view and hide the normal Home icon and title.
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /**
     * Sets custom view on ActionBar for Done activities
     * 
     * @param actionBar
     * @param firstText
     * @param firstOnClickListener
     */
    public static void setOneButtonView(ActionBar actionBar, int firstText, int firstDrawableId,
            OnClickListener firstOnClickListener) {
        // Inflate a "Done" custom action bar view to serve as the "Up" affordance.
        final LayoutInflater inflater = (LayoutInflater) actionBar.getThemedContext()
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater
                .inflate(R.layout.actionbar_custom_view_done, null);

        TextView firstTextView = ((TextView) customActionBarView.findViewById(R.id.actionbar_done_text));
        firstTextView.setText(firstText);
        firstTextView.setCompoundDrawablesWithIntrinsicBounds(firstDrawableId, 0, 0, 0);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                firstOnClickListener);

        // Show the custom action bar view and hide the normal Home icon and title.
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(customActionBarView);
    }
}

/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.ui.keyview.presenter;


import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;


public interface ViewKeyMvpView {
    void switchToFragment(Fragment frag, String backStackName);

    void startActivity(Intent intent);
    void startActivityAndShowResultSnackbar(Intent intent);
    void showDialogFragment(DialogFragment dialogFragment, final String tag);
    void setContentShown(boolean show, boolean animate);

    void showContextMenu(int position, View anchor);
}

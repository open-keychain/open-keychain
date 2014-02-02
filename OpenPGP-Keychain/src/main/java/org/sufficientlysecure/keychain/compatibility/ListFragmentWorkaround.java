/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.compatibility;

import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

/**
 * Bug on Android >= 4.1
 * <p/>
 * http://code.google.com/p/android/issues/detail?id=35885
 * <p/>
 * Items are not checked in layout
 */
public class ListFragmentWorkaround extends ListFragment {

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        l.setItemChecked(position, l.isItemChecked(position));
    }
}

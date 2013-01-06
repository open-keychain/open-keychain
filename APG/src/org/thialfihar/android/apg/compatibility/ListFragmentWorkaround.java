/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.compatibility;

import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Bug on Android >= 4.1
 * 
 * http://code.google.com/p/android/issues/detail?id=35885
 * 
 * Items are not checked in layout
 */
public class ListFragmentWorkaround extends SherlockListFragment {

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        l.setItemChecked(position, l.isItemChecked(position));
    }
}

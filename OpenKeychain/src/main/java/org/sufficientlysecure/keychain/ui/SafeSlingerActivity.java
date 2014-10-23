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

package org.sufficientlysecure.keychain.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.NumberPicker;

import org.sufficientlysecure.keychain.R;


public class SafeSlingerActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.safe_slinger_activity);

        NumberPicker numberPicker = (NumberPicker) findViewById(R.id.safe_slinger_number_picker);

        numberPicker.setDisplayedValues(new String[]{"2","3","4","5","6","7","8","9","10"});
        numberPicker.setValue(0);
//        numberPicker.setMaxValue(8);
//        numberPicker.setMinValue(0);
    }

}

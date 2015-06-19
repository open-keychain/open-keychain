/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

/**
 * Simply encapsulates a dialog. If orbot is not installed, it shows an install dialog, else a dialog to enable orbot.
 */
public class OrbotRequiredDialogActivity extends FragmentActivity {

    public final static String RESULT_IGNORE_TOR = "ignore_tor";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Runnable ignoreTor = new Runnable() {
            @Override
            public void run() {
                Intent data = new Intent();
                data.putExtra(RESULT_IGNORE_TOR, true);
                setResult(RESULT_OK, data);
            }
        };

        if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor,
                Preferences.getPreferences(this).getProxyPrefs(), this)) {
            Intent data = new Intent();
            data.putExtra(RESULT_IGNORE_TOR, false);
            setResult(RESULT_OK, data);
        }
    }
}

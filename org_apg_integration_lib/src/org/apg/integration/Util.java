/*
 * Copyright (C) 2010-2011 K-9 Mail Contributors
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

package org.apg.integration;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import android.widget.Toast;

public class Util {

    /**
     * Check whether APG is installed and at a high enough version.
     * 
     * @param context
     * @return whether a suitable version of APG was found
     */
    public boolean isApgAvailable(Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(Constants.APG_PACKAGE_NAME,
                    0);
            if (pi.versionCode >= Constants.MIN_REQUIRED_VERSION) {
                return true;
            } else {
                Toast.makeText(context,
                        "This APG version is not supported! Please update to a newer one!",
                        Toast.LENGTH_LONG).show();
            }
        } catch (NameNotFoundException e) {
            // not found
        }

        return false;
    }
}

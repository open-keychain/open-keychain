/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.os.Build;
import android.os.Handler;

/**
 * Bug on Android >= 4.2
 * 
 * http://code.google.com/p/android/issues/detail?id=41901
 * 
 * DialogFragment disappears on pressing home and comming back. This also happens especially in
 * FileDialogFragment after launching a file manager and coming back.
 * 
 * Usage: <code>
 * DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
 *          public void run() {
 *              // show dialog...
 *          }
 *      });
 * </code>
 */
public class DialogFragmentWorkaround {
    public static final SDKLevel17Interface INTERFACE =
                ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) ? new SDKLevel17Impl()
                 : new SDKLevelPriorLevel17Impl());

    private static final int RUNNABLE_DELAY = 300;

    public interface SDKLevel17Interface {
        // Workaround for http://code.google.com/p/android/issues/detail?id=41901
        void runnableRunDelayed(Runnable runnable);
    }

    private static class SDKLevelPriorLevel17Impl implements SDKLevel17Interface {
        @Override
        public void runnableRunDelayed(Runnable runnable) {
            runnable.run();
        }
    }

    private static class SDKLevel17Impl implements SDKLevel17Interface {
        @Override
        public void runnableRunDelayed(Runnable runnable) {
            new Handler().postDelayed(runnable, RUNNABLE_DELAY);
        }
    }

    // Can't instantiate this class
    private DialogFragmentWorkaround() {
    }
}

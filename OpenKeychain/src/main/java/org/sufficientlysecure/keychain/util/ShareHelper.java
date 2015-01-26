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

package org.sufficientlysecure.keychain.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShareHelper {
    Context mContext;

    public ShareHelper(Context context) {
        mContext = context;
    }

    /**
     * Create Intent Chooser but exclude specific activites, e.g., EncryptActivity to prevent encrypting again
     * <p/>
     * Put together from some stackoverflow posts...
     */
    public Intent createChooserExcluding(Intent prototype, String title, String[] activityBlacklist) {
        // Produced an empty list on Huawei U8860 with Android Version 4.0.3
        // TODO: test on 4.1, 4.2, 4.3, only tested on 4.4
        // Disabled on 5.0 because using EXTRA_INITIAL_INTENTS prevents the usage based sorting
        // introduced in 5.0: https://medium.com/@xXxXxXxXxXam/how-lollipops-share-menu-is-organized-d204888f606d
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Intent.createChooser(prototype, title);
        }

        List<LabeledIntent> targetedShareIntents = new ArrayList<>();

        List<ResolveInfo> resInfoList = mContext.getPackageManager().queryIntentActivities(prototype, 0);
        List<ResolveInfo> resInfoListFiltered = new ArrayList<>();
        if (!resInfoList.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfoList) {
                // do not add blacklisted ones
                if (resolveInfo.activityInfo == null || Arrays.asList(activityBlacklist).contains(resolveInfo.activityInfo.name))
                    continue;

                resInfoListFiltered.add(resolveInfo);
            }

            if (!resInfoListFiltered.isEmpty()) {
                // sorting for nice readability
                Collections.sort(resInfoListFiltered, new Comparator<ResolveInfo>() {
                    @Override
                    public int compare(ResolveInfo first, ResolveInfo second) {
                        String firstName = first.loadLabel(mContext.getPackageManager()).toString();
                        String secondName = second.loadLabel(mContext.getPackageManager()).toString();
                        return firstName.compareToIgnoreCase(secondName);
                    }
                });

                // create the custom intent list
                for (ResolveInfo resolveInfo : resInfoListFiltered) {
                    Intent targetedShareIntent = (Intent) prototype.clone();
                    targetedShareIntent.setPackage(resolveInfo.activityInfo.packageName);
                    targetedShareIntent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);

                    LabeledIntent lIntent = new LabeledIntent(targetedShareIntent,
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.loadLabel(mContext.getPackageManager()),
                            resolveInfo.activityInfo.icon);
                    targetedShareIntents.add(lIntent);
                }

                // Create chooser with only one Intent in it
                Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), title);
                // append all other Intents
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
                return chooserIntent;
            }

        }

        // fallback to Android's default chooser
        return Intent.createChooser(prototype, title);
    }
}

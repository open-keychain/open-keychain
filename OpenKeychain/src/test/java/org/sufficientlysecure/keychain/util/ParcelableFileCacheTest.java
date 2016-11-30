/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.os.Bundle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;

import java.util.ArrayList;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class ParcelableFileCacheTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testInputOutput() throws Exception {

        ParcelableFileCache<Bundle> cache = new ParcelableFileCache<Bundle>(RuntimeEnvironment.application, "test.pcl");

        ArrayList<Bundle> list = new ArrayList<Bundle>();

        for (int i = 0; i < 50; i++) {
            Bundle b = new Bundle();
            b.putInt("key1", i);
            b.putString("key2", Integer.toString(i));
            list.add(b);
        }

        // write to cache file
        cache.writeCache(list.size(), list.iterator());

        // read back
        IteratorWithSize<Bundle> it = cache.readCache();

        Assert.assertEquals("number of entries must be correct", list.size(), it.getSize());

        while (it.hasNext()) {
            Bundle b = it.next();
            Assert.assertEquals("input values should be equal to output values",
                    b.getInt("key1"), b.getInt("key1"));
            Assert.assertEquals("input values should be equal to output values",
                    b.getString("key2"), b.getString("key2"));
        }

    }

}

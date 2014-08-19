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

import android.os.Bundle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class FileImportCacheTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testInputOutput() throws Exception {

        FileImportCache<Bundle> cache = new FileImportCache<Bundle>(Robolectric.application, "test.pcl");

        ArrayList<Bundle> list = new ArrayList<Bundle>();

        for (int i = 0; i < 50; i++) {
            Bundle b = new Bundle();
            b.putInt("key1", i);
            b.putString("key2", Integer.toString(i));
            list.add(b);
        }

        // write to cache file
        cache.writeCache(list);

        // read back
        List<Bundle> last = cache.readCacheIntoList();

        for (int i = 0; i < list.size(); i++) {
            Assert.assertEquals("input values should be equal to output values",
                    list.get(i).getInt("key1"), last.get(i).getInt("key1"));
            Assert.assertEquals("input values should be equal to output values",
                    list.get(i).getString("key2"), last.get(i).getString("key2"));
        }

    }

}

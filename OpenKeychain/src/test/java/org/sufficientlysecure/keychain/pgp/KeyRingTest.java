/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.pgp;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class KeyRingTest {

    @Test
    public void splitCompleteUserIdShouldReturnAll3Components() throws Exception {
        KeyRing.UserId info = KeyRing.splitUserId("Max Mustermann (this is a comment) <max@example.com>");
        Assert.assertEquals("Max Mustermann", info.name);
        Assert.assertEquals("this is a comment", info.comment);
        Assert.assertEquals("max@example.com", info.email);
    }

    @Test
    public void splitUserIdWithAllButCommentShouldReturnNameAndEmail() throws Exception {
        KeyRing.UserId info = KeyRing.splitUserId("Max Mustermann <max@example.com>");
        Assert.assertEquals("Max Mustermann", info.name);
        Assert.assertNull(info.comment);
        Assert.assertEquals("max@example.com", info.email);
    }

    @Test
    public void splitUserIdWithAllButEmailShouldReturnNameAndComment() throws Exception {
        KeyRing.UserId info = KeyRing.splitUserId("Max Mustermann (this is a comment)");
        Assert.assertEquals(info.name, "Max Mustermann");
        Assert.assertEquals(info.comment, "this is a comment");
        Assert.assertNull(info.email);
    }

}
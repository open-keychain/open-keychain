/*
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

import java.util.Iterator;
import java.util.Random;

import junit.framework.Assert;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;


public class TestingUtils {
    public static Passphrase genPassphrase() {
        return genPassphrase(false);
    }

    public static Passphrase genPassphrase(boolean noEmpty) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789!@#$%^&*()-_=";
        Random r = new Random();
        StringBuilder passbuilder = new StringBuilder();
        // 20% chance for an empty passphrase
        for(int i = 0, j = noEmpty || r.nextInt(10) > 2 ? r.nextInt(20)+1 : 0; i < j; i++) {
            passbuilder.append(chars.charAt(r.nextInt(chars.length())));
        }
        System.out.println("Generated passphrase: '" + passbuilder.toString() + "'");
        return new Passphrase(passbuilder.toString());
    }

    public static void assertArrayEqualsPrefix(String msg, byte[] expected, byte[] actual) {

        Assert.assertTrue("exepected must be shorter or equal to actual array length",
                expected.length <= actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(msg, expected[i], actual[i]);
        }

    }

    public static void assertArrayEqualsSuffix(String msg, byte[] expected, byte[] actual) {

        Assert.assertTrue("exepected must be shorter or equal to actual array length",
                expected.length <= actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(msg, expected[i], actual[actual.length -expected.length +i]);
        }

    }

    public static KeyringPassphrases generateKeyringPassphrases(UncachedKeyRing keyRing, Passphrase passphrase) {
        KeyringPassphrases keyringPassphrases = new KeyringPassphrases(keyRing.getMasterKeyId());
        Iterator<UncachedPublicKey> iterator = keyRing.getPublicKeys();
        while(iterator.hasNext()) {
            UncachedPublicKey key = iterator.next();
            keyringPassphrases.mSubkeyPassphrases.put(key.getKeyId(), passphrase);
        }
        return keyringPassphrases;
    }

}

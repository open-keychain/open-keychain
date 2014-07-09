/*
 * Copyright (C) Art O Cathain
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

package org.sufficientlysecure.keychain.support;

import org.spongycastle.bcpg.ContainedPacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * Misc support functions. Would just use Guava / Apache Commons but
 * avoiding extra dependencies.
 */
public class TestDataUtil {
    public static byte[] readFully(InputStream input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        appendToOutput(input, output);
        return output.toByteArray();
    }

    public static void appendToOutput(InputStream input, OutputStream output) {
        byte[] buffer = new byte[8192];
        int bytesRead;
        try {
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readAllFully(Collection<String> inputResources) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (String inputResource : inputResources) {
            appendToOutput(getResourceAsStream(inputResource), output);
        }
        return output.toByteArray();
    }

    public static InputStream getResourceAsStream(String resourceName) {
        return TestDataUtil.class.getResourceAsStream(resourceName);
    }

    /**
     * Null-safe equivalent of {@code a.equals(b)}.
     */
    public static boolean equals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public static <T> boolean iterEquals(Iterator<T> a, Iterator<T> b, EqualityChecker<T> comparator) {
        while (a.hasNext()) {
            T aObject = a.next();
            if (!b.hasNext()) {
                return false;
            }
            T bObject = b.next();
            if (!comparator.areEquals(aObject, bObject)) {
                return false;
            }
        }

        if (b.hasNext()) {
            return false;
        }

        return true;
    }


    public static <T> boolean iterEquals(Iterator<T> a, Iterator<T> b) {
        return iterEquals(a, b, new EqualityChecker<T>() {
            @Override
            public boolean areEquals(T lhs, T rhs) {
                return TestDataUtil.equals(lhs, rhs);
            }
        });
    }

    public static interface EqualityChecker<T> {
        public boolean areEquals(T lhs, T rhs);
    }


    public static byte[] concatAll(java.util.List<ContainedPacket>  packets) {
        byte[][] byteArrays = new byte[packets.size()][];
        try {
            for (int i = 0; i < packets.size(); i++) {
                byteArrays[i] = packets.get(i).getEncoded();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return concatAll(byteArrays);
    }

    public static byte[] concatAll(byte[]... byteArrays) {
        if (byteArrays.length == 1) {
            return byteArrays[0];
        } else if (byteArrays.length == 2) {
            return concat(byteArrays[0], byteArrays[1]);
        } else {
            byte[] first = concat(byteArrays[0], byteArrays[1]);
            byte[][] remainingArrays = new byte[byteArrays.length - 1][];
            remainingArrays[0] = first;
            System.arraycopy(byteArrays, 2, remainingArrays, 1, byteArrays.length - 2);
            return concatAll(remainingArrays);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

}

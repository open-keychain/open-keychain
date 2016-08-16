/*
 * Copyright (C) Art O Cathain
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

package org.sufficientlysecure.keychain.support;

import android.content.Context;

import org.bouncycastle.util.Arrays;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.provider.ProviderReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/** Helper methods for keyring tests. */
public class KeyringTestingHelper {

    private final Context context;

    public KeyringTestingHelper(Context robolectricContext) {
        this.context = robolectricContext;
    }

    public boolean addKeyring(Collection<String> blobFiles) throws Exception {

        ProviderHelper providerHelper = new ProviderHelper(context);

        byte[] data = TestDataUtil.readAllFully(blobFiles);
        UncachedKeyRing ring = UncachedKeyRing.decodeFromData(data);
        long masterKeyId = ring.getMasterKeyId();

        // Should throw an exception; key is not yet saved
        retrieveKeyAndExpectNotFound(providerHelper, masterKeyId);

        SaveKeyringResult saveKeyringResult = providerHelper.write().savePublicKeyRing(ring);

        boolean saveSuccess = saveKeyringResult.success();

        // Now re-retrieve the saved key. Should not throw an exception.
        providerHelper.read().getCanonicalizedPublicKeyRing(masterKeyId);

        // A different ID should still fail
        retrieveKeyAndExpectNotFound(providerHelper, masterKeyId - 1);

        return saveSuccess;
    }

    public static UncachedKeyRing removePacket(UncachedKeyRing ring, int position)
            throws IOException, PgpGeneralException {
        return UncachedKeyRing.decodeFromData(removePacket(ring.getEncoded(), position));
    }

    public static byte[] removePacket(byte[] ring, int position) throws IOException {
        Iterator<RawPacket> it = parseKeyring(ring);
        ByteArrayOutputStream out = new ByteArrayOutputStream(ring.length);

        int i = 0;
        while(it.hasNext()) {
            // at the right position, skip the packet
            if(i++ == position) {
                it.next();
                continue;
            }
            // write the old one
            out.write(it.next().buf);
        }

        if (i <= position) {
            throw new IndexOutOfBoundsException("injection index did not not occur in stream!");
        }

        return out.toByteArray();
    }

    public static UncachedKeyRing injectPacket(UncachedKeyRing ring, byte[] inject, int position)
            throws IOException, PgpGeneralException {
        return UncachedKeyRing.decodeFromData(injectPacket(ring.getEncoded(), inject, position));
    }

    public static byte[] injectPacket(byte[] ring, byte[] inject, int position) throws IOException {

        Iterator<RawPacket> it = parseKeyring(ring);
        ByteArrayOutputStream out = new ByteArrayOutputStream(ring.length + inject.length);

        int i = 0;
        while(it.hasNext()) {
            // at the right position, inject the new packet
            if(i++ == position) {
                out.write(inject);
            }
            // write the old one
            out.write(it.next().buf);
        }

        if (i <= position) {
            throw new IndexOutOfBoundsException("injection index did not not occur in stream!");
        }

        return out.toByteArray();

    }

    /** This class contains a single pgp packet, together with information about its position
     * in the keyring and its packet tag.
     */
    public static class RawPacket {
        public int position;

        // packet tag for convenience, this can also be read from the header
        public int tag;

        public int headerLength, length;
        // this buf includes the header, so its length is headerLength + length!
        public byte[] buf;

        @Override
        public boolean equals(Object other) {
            return other instanceof RawPacket && Arrays.areEqual(this.buf, ((RawPacket) other).buf);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(buf);
        }
    }

    /** A comparator which compares RawPackets by their position */
    public static final Comparator<RawPacket> packetOrder = new Comparator<RawPacket>() {
        public int compare(RawPacket left, RawPacket right) {
            return Integer.compare(left.position, right.position);
        }
    };

    /** Diff two keyrings, returning packets only present in one keyring in its associated List.
     *
     * Packets in the returned lists are annotated and ordered by their original order of appearance
     * in their origin keyrings.
     *
     * @return true if keyrings differ in at least one packet
     */
    public static boolean diffKeyrings(byte[] ringA, byte[] ringB,
                                       List<RawPacket> onlyA, List<RawPacket> onlyB)
            throws IOException {
        Iterator<RawPacket> streamA = parseKeyring(ringA);
        Iterator<RawPacket> streamB = parseKeyring(ringB);

        HashSet<RawPacket> a = new HashSet<RawPacket>(), b = new HashSet<RawPacket>();

        RawPacket p;
        int pos = 0;
        while(true) {
            p = streamA.next();
            if (p == null) {
                break;
            }
            p.position = pos++;
            a.add(p);
        }
        pos = 0;
        while(true) {
            p = streamB.next();
            if (p == null) {
                break;
            }
            p.position = pos++;
            b.add(p);
        }

        onlyA.clear();
        onlyB.clear();

        onlyA.addAll(a);
        onlyA.removeAll(b);
        onlyB.addAll(b);
        onlyB.removeAll(a);

        Collections.sort(onlyA, packetOrder);
        Collections.sort(onlyB, packetOrder);

        return !onlyA.isEmpty() || !onlyB.isEmpty();
    }

    /** Creates an iterator of RawPackets over a binary keyring. */
    public static Iterator<RawPacket> parseKeyring(byte[] ring) {

        final InputStream stream = new ByteArrayInputStream(ring);

        return new Iterator<RawPacket>() {
            RawPacket next;

            @Override
            public boolean hasNext() {
                if (next == null) try {
                    next = readPacket(stream);
                } catch (IOException e) {
                    return false;
                }
                return next != null;
            }

            @Override
            public RawPacket next() {
                if (!hasNext()) {
                    return null;
                }
                try {
                    return next;
                } finally {
                    next = null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    /** Read a single (raw) pgp packet from an input stream.
     *
     * Note that the RawPacket.position field is NOT set here!
     *
     * Variable length packets are not handled here. we don't use those in our test classes, and
     * otherwise rely on BouncyCastle's own unit tests to handle those correctly.
     */
    private static RawPacket readPacket(InputStream in) throws IOException {

        // save here. this is tag + length, max 6 bytes
        in.mark(6);

        int hdr = in.read();
        int headerLength = 1;

        if (hdr < 0) {
            return null;
        }

        if ((hdr & 0x80) == 0) {
            throw new IOException("invalid header encountered");
        }

        boolean newPacket = (hdr & 0x40) != 0;
        int tag;
        int bodyLen;

        if (newPacket) {
            tag = hdr & 0x3f;

            int l = in.read();
            headerLength += 1;

            if (l < 192) {
                bodyLen = l;
            } else if (l <= 223) {
                int b = in.read();
                headerLength += 1;

                bodyLen = ((l - 192) << 8) + (b) + 192;
            } else if (l == 255) {
                bodyLen = (in.read() << 24) | (in.read() << 16) | (in.read() << 8) | in.read();
                headerLength += 4;
            } else {
                // bodyLen = 1 << (l & 0x1f);
                throw new IOException("no support for partial bodies in test classes");
            }
        } else {
            int lengthType = hdr & 0x3;

            tag = (hdr & 0x3f) >> 2;

            switch (lengthType) {
                case 0:
                    bodyLen = in.read();
                    headerLength += 1;
                    break;
                case 1:
                    bodyLen = (in.read() << 8) | in.read();
                    headerLength += 2;
                    break;
                case 2:
                    bodyLen = (in.read() << 24) | (in.read() << 16) | (in.read() << 8) | in.read();
                    headerLength += 4;
                    break;
                case 3:
                    // bodyLen = 1 << (l & 0x1f);
                    throw new IOException("no support for partial bodies in test classes");
                default:
                    throw new IOException("unknown length type encountered");
            }
        }

        in.reset();

        // read the entire packet INCLUDING the header here
        byte[] buf = new byte[headerLength+bodyLen];
        if (in.read(buf) != headerLength+bodyLen) {
            throw new IOException("read length mismatch!");
        }
        RawPacket p = new RawPacket();
        p.tag = tag;
        p.headerLength = headerLength;
        p.length = bodyLen;
        p.buf = buf;
        return p;

    }

    public static <E> E getNth(Iterator<E> it, int position) {
        while(position-- > 0) {
            it.next();
        }
        return it.next();
    }

    public static long getSubkeyId(UncachedKeyRing ring, int position) {
        return getNth(ring.getPublicKeys(), position).getKeyId();
    }

    private void retrieveKeyAndExpectNotFound(ProviderHelper providerHelper, long masterKeyId) {
        try {
            providerHelper.read().getCanonicalizedPublicKeyRing(masterKeyId);
            throw new AssertionError("Was expecting the previous call to fail!");
        } catch (ProviderReader.NotFoundException expectedException) {
            // good
        }
    }

    public static <E> List<E> itToList(Iterator<E> it) {
        List<E> result = new ArrayList<E>();
        while(it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

}

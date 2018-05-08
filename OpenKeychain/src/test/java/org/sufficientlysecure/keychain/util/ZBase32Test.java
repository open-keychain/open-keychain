package org.sufficientlysecure.keychain.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZBase32Test {

    @Test
    public void encode() {
        assertEquals("yyyoryar", ZBase32.encode(new byte[]{0, 1, 2, 3, 4}));
    }

}

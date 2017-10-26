package org.sufficientlysecure.keychain.securitytoken;


import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.KeychainTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class CommandApduTest {
    static final byte[] DATA_LONG = new byte[500];
    static final byte[] DATA_SHORT = { 1, 2, 3 };
    static final int CLA = 1;
    static final int INS = 2;
    static final int P1 = 3;
    static final int P2 = 4;
    static final int NE_SHORT = 5;
    static final int NE_LONG = 500;
    static final int NE_SPECIAL = 256;

    @Test
    public void testCase1() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2);

        assertParsesCorrectly(commandApdu);
    }

    @Test
    public void testCase2s() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2, NE_SHORT);

        assertEquals(5, commandApdu.toBytes().length);

        assertParsesCorrectly(commandApdu);
    }

    @Test
    public void testCase2e() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2, NE_LONG);

        assertEquals(7, commandApdu.toBytes().length);

        assertParsesCorrectly(commandApdu);
    }

    @Test
    public void testCase2e_specialNe() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2, NE_SPECIAL);

        assertEquals(5, commandApdu.toBytes().length);

        assertParsesCorrectly(commandApdu);
    }

    @Test
    public void testCase3s() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2, DATA_SHORT);

        assertEquals(4 + 1 + DATA_SHORT.length, commandApdu.toBytes().length);

        assertParsesCorrectly(commandApdu);
    }

    @Test
    public void testCase3e() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2, DATA_LONG);

        assertEquals(4 + 3 + DATA_LONG.length, commandApdu.toBytes().length);

        assertParsesCorrectly(commandApdu);
    }

    @Test
    public void testCase4s() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2, DATA_SHORT, 5);

        assertArrayEquals(Hex.decode("010203040301020305"), commandApdu.toBytes());

        assertParsesCorrectly(commandApdu);
    }

    @Test
    public void testCase4e() throws Exception {
        CommandApdu commandApdu = CommandApdu.create(CLA, INS, P1, P2, DATA_LONG, 5);

        assertEquals(4 + 5 + DATA_LONG.length, commandApdu.toBytes().length);

        assertParsesCorrectly(commandApdu);
    }

    private void assertParsesCorrectly(CommandApdu commandApdu) {
        byte[] bytes = commandApdu.toBytes();
        CommandApdu parsedCommandApdu = CommandApdu.fromBytes(bytes);
        assertEquals(commandApdu, parsedCommandApdu);
    }
}
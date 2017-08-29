package org.sufficientlysecure.keychain.network;


import java.net.URISyntaxException;

import android.annotation.SuppressLint;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@SuppressWarnings("WeakerAccess")
@SuppressLint("DefaultLocale")
public class SktUriTest {
    static final String HOST = "127.0.0.1";
    static final int PORT = 1234;
    static final byte[] PRESHARED_KEY = { 1, 2 };
    static final String SSID = "ssid";

    static final String ENCODED_SKT = String.format("OPGPSKT:%s/%d/%s/SSID:%s",
            HOST, PORT, Hex.toHexString(PRESHARED_KEY), Hex.toHexString(SSID.getBytes()));

    @Test
    public void testCreate() {
        SktUri sktUri = SktUri.create(HOST, PORT, PRESHARED_KEY, null);

        assertEquals(HOST, sktUri.getHost());
        assertEquals(PORT, sktUri.getPort());
        assertArrayEquals(PRESHARED_KEY, sktUri.getPresharedKey());
        assertEquals(null, sktUri.getWifiSsid());
    }

    @Test
    public void testCreateWithSsid() {
        SktUri sktUri = SktUri.create(HOST, PORT, PRESHARED_KEY, SSID);

        assertEquals(HOST, sktUri.getHost());
        assertEquals(PORT, sktUri.getPort());
        assertArrayEquals(PRESHARED_KEY, sktUri.getPresharedKey());
        assertEquals(SSID, sktUri.getWifiSsid());
    }

    @Test
    public void testCreate_isAllUppercase() {
        SktUri sktUri = SktUri.create(HOST, PORT, PRESHARED_KEY, SSID);

        String encodedSktUri = sktUri.toUriString();
        assertEquals(encodedSktUri.toUpperCase(), encodedSktUri);
    }

    @Test
    public void testParse() throws URISyntaxException {
        SktUri sktUri = SktUri.parse(ENCODED_SKT);

        assertNotNull(sktUri);
        assertEquals(HOST, sktUri.getHost());
        assertEquals(PORT, sktUri.getPort());
        assertArrayEquals(PRESHARED_KEY, sktUri.getPresharedKey());
        assertEquals(SSID, sktUri.getWifiSsid());
    }

    @Test
    public void testBackAndForth() throws URISyntaxException {
        SktUri sktUri = SktUri.create(HOST, PORT, PRESHARED_KEY, null);
        String encodedSktUri = sktUri.toUriString();
        SktUri decodedSktUri = SktUri.parse(encodedSktUri);

        assertEquals(sktUri, decodedSktUri);
    }

    @Test
    public void testBackAndForthWithSsid() throws URISyntaxException {
        SktUri sktUri = SktUri.create(HOST, PORT, PRESHARED_KEY, SSID);
        String encodedSktUri = sktUri.toUriString();
        SktUri decodedSktUri = SktUri.parse(encodedSktUri);

        assertEquals(sktUri, decodedSktUri);
    }

    @Test(expected = URISyntaxException.class)
    public void testParse_withBadScheme_shouldFail() throws URISyntaxException {
        SktUri.parse(String.format("XXXGPSKT:%s/%d/%s/SSID:%s",
                HOST, PORT, Hex.toHexString(PRESHARED_KEY), Hex.toHexString(SSID.getBytes())));
    }

    @Test(expected = URISyntaxException.class)
    public void testParse_withBadPsk_shouldFail() throws URISyntaxException {
        SktUri.parse(String.format("OPGPSKT:%s/%d/xx%s/SSID:%s",
                HOST, PORT, Hex.toHexString(PRESHARED_KEY), Hex.toHexString(SSID.getBytes())));
    }

    @Test(expected = URISyntaxException.class)
    public void testParse_withBadPort_shouldFail() throws URISyntaxException {
        SktUri.parse(String.format("OPGPSKT:%s/x%d/%s/SSID:%s",
                HOST, PORT, Hex.toHexString(PRESHARED_KEY), Hex.toHexString(SSID.getBytes())));
    }
}
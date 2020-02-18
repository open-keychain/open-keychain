package org.sufficientlysecure.keychain.util;

import org.junit.Test;

import java.net.URL;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class WebKeyDirectoryUtilTest {

    @Test
    public void testWkd() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL("test-wkd@openkeychain.org", false);
        assertNotNull(url);
        assertEquals("openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/hu/4hg7tescnttreaouu4z1izeuuyibwww1", url.getPath());
        assertEquals("l=test-wkd", url.getQuery());
    }

    @Test
    public void testAdvancedWkd() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL("test-wkd@openkeychain.org", true);
        assertNotNull(url);
        assertEquals("openpgpkey.openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/openkeychain.org/hu/4hg7tescnttreaouu4z1izeuuyibwww1", url.getPath());
        assertEquals("l=test-wkd", url.getQuery());
    }

    @Test
    public void testWkdWithSpaces() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL(" test-wkd@openkeychain.org ", false);
        assertNotNull(url);
        assertEquals("openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/hu/4hg7tescnttreaouu4z1izeuuyibwww1", url.getPath());
        assertEquals("l=test-wkd", url.getQuery());
    }

    @Test
    public void testWkdAdvancedWithSpaces() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL(" test-wkd@openkeychain.org ", true);
        assertNotNull(url);
        assertEquals("openpgpkey.openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/openkeychain.org/hu/4hg7tescnttreaouu4z1izeuuyibwww1", url.getPath());
        assertEquals("l=test-wkd", url.getQuery());
    }

    @Test
    public void testWkdWithUnicode() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL("test\u2013wkd@openkeychain.org", false);
        assertNotNull(url);
        assertEquals("openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/hu/nb7e5p4jhz3i3micncnfy5dfkp1ug53i", url.getPath());
        assertEquals("l=test%E2%80%93wkd", url.getQuery());
    }

    @Test
    public void testWkdAdvancedWithUnicode() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL("test\u2013wkd@openkeychain.org", true);
        assertNotNull(url);
        assertEquals("openpgpkey.openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/openkeychain.org/hu/nb7e5p4jhz3i3micncnfy5dfkp1ug53i", url.getPath());
        assertEquals("l=test%E2%80%93wkd", url.getQuery());
    }

    @Test
    public void testWkdDirectUrl() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL("https://openkeychain.org/.well-known/openpgpkey/hu/4hg7tescnttreaouu4z1izeuuyibwww1?l=test-wkd", false);
        assertNotNull(url);
        assertEquals("openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/hu/4hg7tescnttreaouu4z1izeuuyibwww1", url.getPath());
        assertEquals("l=test-wkd", url.getQuery());
    }

    @Test
    public void testWkdAdvancedURL() {
        URL url = WebKeyDirectoryUtil.toWebKeyDirectoryURL("https://openpgpkey.openkeychain.org/.well-known/openpgpkey/openkeychain.org/hu/4hg7tescnttreaouu4z1izeuuyibwww1?l=test-wkd", false);
        assertNotNull(url);
        assertEquals("openpgpkey.openkeychain.org", url.getHost());
        assertEquals("https", url.getProtocol());
        assertEquals("/.well-known/openpgpkey/openkeychain.org/hu/4hg7tescnttreaouu4z1izeuuyibwww1", url.getPath());
        assertEquals("l=test-wkd", url.getQuery());
    }

}

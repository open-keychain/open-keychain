package org.sufficientlysecure.keychain.util;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebKeyDirectoryUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\s*([^\\s]+)@([^\\s]+)\\s*$");

    private WebKeyDirectoryUtil() {
    }

    /**
     * Tries to construct a Web Key Directory from a given name.
     * Returns {@code null} if unsuccessful.
     *
     * @see <a href="https://tools.ietf.org/html/draft-koch-openpgp-webkey-service-05#section-3.1">Key Discovery</a>
     */
    @Nullable
    public static URL toWebKeyDirectoryURL(String name, Boolean wkdMethodAdvanced) {
        if (name == null) {
            return null;
        }

        if (name.startsWith("https://") && name.contains("/.well-known/openpgpkey/"))  {
            try {
                return new URL(name);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        Matcher matcher = EMAIL_PATTERN.matcher(name);

        if (!matcher.matches()) {
            return null;
        }

        String localPart = matcher.group(1);
        String encodedPart = ZBase32.encode(toSHA1(localPart.toLowerCase().getBytes()));
        String domain = matcher.group(2);

        try {

            if(wkdMethodAdvanced) {
                // Advanced method
                return new URL("https://openpgpkey." + domain + "/.well-known/openpgpkey/" + domain + "/hu/" + encodedPart + "?l=" + URLEncoder.encode(localPart, "UTF-8"));
            }else{
                // Direct method
                return new URL("https://" + domain + "/.well-known/openpgpkey/hu/" + encodedPart + "?l=" + URLEncoder.encode(localPart, "UTF-8"));
            }
        } catch (MalformedURLException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        
    }

    private static byte[] toSHA1(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-1 should always be available");
        }
    }

}

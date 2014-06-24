package org.sufficientlysecure.keychain.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

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

    private static void appendToOutput(InputStream input, ByteArrayOutputStream output) {
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
}

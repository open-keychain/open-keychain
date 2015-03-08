package org.sufficientlysecure.keychain.util;

import java.util.Random;

public class TestingUtils {
    public static String genPassphrase() {
        return genPassphrase(false);
    }

    public static String genPassphrase(boolean noEmpty) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789!@#$%^&*()-_=";
        Random r = new Random();
        StringBuilder passbuilder = new StringBuilder();
        // 20% chance for an empty passphrase
        for(int i = 0, j = noEmpty || r.nextInt(10) > 2 ? r.nextInt(20)+1 : 0; i < j; i++) {
            passbuilder.append(chars.charAt(r.nextInt(chars.length())));
        }
        System.out.println("Generated passphrase: '" + passbuilder.toString() + "'");
        return passbuilder.toString();
    }
}

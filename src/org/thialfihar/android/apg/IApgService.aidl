package org.thialfihar.android.apg;

interface IApgService {
    String encrypt_with_passphrase(String msg, String passphrase);
    String decrypt_with_passphrase(String encrypted_msg, String passphrase);
}
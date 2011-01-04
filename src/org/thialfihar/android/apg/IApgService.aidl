package org.thialfihar.android.apg;

interface IApgService {
    String encrypt_with_passphrase(in List<String> params);
    String decrypt_with_passphrase(in List<String> params);
}
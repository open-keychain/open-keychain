package org.thialfihar.android.apg;

interface IApgService {

    /** All functions fill the return_vals Bundle with the following keys:
     *
     * ArrayList<String> "WARNINGS"  = Warnings, if any
     * ArrayList<String> "ERRORS"    = Human readable error descriptions, why function call failed
     * int "ERROR"                   = Numeric representation of error
     */

    /** Encrypt something with a symmetric key
     *
     * Bundle params' keys:
     *   (optional/required) TYPE "STRING KEY" = EXPLANATION / VALUES
     *
     *   (required) String  "MSG"             = Message to encrypt
     *   (required) String  "SYM_KEY"         = Symmetric key to use
     *   (optional) int     "ENCRYPTION_ALGO" = Encryption Algorithm
     *                                          7: AES-128, 8: AES-192, 9: AES-256, 
                                                4: Blowfish, 10: Twofish, 3: CAST5,
                                                6: DES, 2: Triple DES, 1: IDEA
     *   (optional) int     "HASH_ALGO"       = Hash Algorithm
                                                1: MD5, 3: RIPEMD-160, 2: SHA-1,
                                                11: SHA-224, 8: SHA-256, 9: SHA-384,
                                                10: SHA-512
     *   (optional) Boolean "ARMORED"         = Armor output
     *   (optional) Boolean "FORCE_V3_SIG"    = Force V3 Signatures
     *   (optional) int     "COMPRESSION"     = Compression to use
                                                0x21070001: none, 1: Zip, 2: Zlib,
                                                3: BZip2
     *
     * Bundle return_vals (in addition to the ERRORS/WARNINGS above):
     *              String  "RESULT"          = Encrypted MSG
     */
    boolean encrypt_with_passphrase(in Bundle params, out Bundle return_vals);

    /** Decrypt something with a symmetric key
     *
     * Bundle params:
     *   (required) String "MSG"      = Message to decrypt
     *   (required) String "SYM_KEY"  = Symmetric key to use
     *
     * Bundle return_vals:
     *   String "RESULT"              = Decrypted MSG
     */
        boolean decrypt_with_passphrase(in Bundle params, out Bundle return_vals);
}
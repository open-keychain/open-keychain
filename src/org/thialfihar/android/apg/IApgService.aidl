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
     * Bundle params:
     *   (optional/required) TYPE "STRING KEY" = EXPLANATION
     *
     *   (required) String "MSG"      = Message to encrypt
     *   (required) String "SYM_KEY"  = Symmetric key to use
     *
     * Bundle return_vals (in addition to the ERRORS/WARNINGS above):
     *   String "RESULT"              = Encrypted MSG
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
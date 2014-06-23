/*
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain;

import android.os.Build;
import android.os.Environment;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.remote.ui.AppsListActivity;
import org.sufficientlysecure.keychain.ui.DecryptActivity;
import org.sufficientlysecure.keychain.ui.EncryptActivity;
import org.sufficientlysecure.keychain.ui.KeyListActivity;

public final class Constants {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static final String TAG = "Keychain";

    public static final String PACKAGE_NAME = "org.sufficientlysecure.keychain";

    // as defined in http://tools.ietf.org/html/rfc3156, section 7
    public static final String NFC_MIME = "application/pgp-keys";

    // used by QR Codes (Guardian Project, Monkeysphere compatiblity)
    public static final String FINGERPRINT_SCHEME = "openpgp4fpr";

    // Not BC due to the use of Spongy Castle for Android
    public static final String SC = BouncyCastleProvider.PROVIDER_NAME;
    public static final String BOUNCY_CASTLE_PROVIDER_NAME = SC;

    public static final String INTENT_PREFIX = PACKAGE_NAME + ".action.";

    public static final String CUSTOM_CONTACT_DATA_MIME_TYPE = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.key";

    // TODO: Resource/Asset?
    public static final String SKS_KEYSERVERS_NET_CA =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFizCCA3OgAwIBAgIJAK9zyLTPn4CPMA0GCSqGSIb3DQEBBQUAMFwxCzAJBgNV\n" +
            "BAYTAk5PMQ0wCwYDVQQIDARPc2xvMR4wHAYDVQQKDBVza3Mta2V5c2VydmVycy5u\n" +
            "ZXQgQ0ExHjAcBgNVBAMMFXNrcy1rZXlzZXJ2ZXJzLm5ldCBDQTAeFw0xMjEwMDkw\n" +
            "MDMzMzdaFw0yMjEwMDcwMDMzMzdaMFwxCzAJBgNVBAYTAk5PMQ0wCwYDVQQIDARP\n" +
            "c2xvMR4wHAYDVQQKDBVza3Mta2V5c2VydmVycy5uZXQgQ0ExHjAcBgNVBAMMFXNr\n" +
            "cy1rZXlzZXJ2ZXJzLm5ldCBDQTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoC\n" +
            "ggIBANdsWy4PXWNUCkS3L//nrd0GqN3dVwoBGZ6w94Tw2jPDPifegwxQozFXkG6I\n" +
            "6A4TK1CJLXPvfz0UP0aBYyPmTNadDinaB9T4jIwd4rnxl+59GiEmqkN3IfPsv5Jj\n" +
            "MkKUmJnvOT0DEVlEaO1UZIwx5WpfprB3mR81/qm4XkAgmYrmgnLXd/pJDAMk7y1F\n" +
            "45b5zWofiD5l677lplcIPRbFhpJ6kDTODXh/XEdtF71EAeaOdEGOvyGDmCO0GWqS\n" +
            "FDkMMPTlieLA/0rgFTcz4xwUYj/cD5e0ZBuSkYsYFAU3hd1cGfBue0cPZaQH2HYx\n" +
            "Qk4zXD8S3F4690fRhr+tki5gyG6JDR67aKp3BIGLqm7f45WkX1hYp+YXywmEziM4\n" +
            "aSbGYhx8hoFGfq9UcfPEvp2aoc8u5sdqjDslhyUzM1v3m3ZGbhwEOnVjljY6JJLx\n" +
            "MxagxnZZSAY424ZZ3t71E/Mn27dm2w+xFRuoy8JEjv1d+BT3eChM5KaNwrj0IO/y\n" +
            "u8kFIgWYA1vZ/15qMT+tyJTfyrNVV/7Df7TNeWyNqjJ5rBmt0M6NpHG7CrUSkBy9\n" +
            "p8JhimgjP5r0FlEkgg+lyD+V79H98gQfVgP3pbJICz0SpBQf2F/2tyS4rLm+49rP\n" +
            "fcOajiXEuyhpcmzgusAj/1FjrtlynH1r9mnNaX4e+rLWzvU5AgMBAAGjUDBOMB0G\n" +
            "A1UdDgQWBBTkwyoJFGfYTVISTpM8E+igjdq28zAfBgNVHSMEGDAWgBTkwyoJFGfY\n" +
            "TVISTpM8E+igjdq28zAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4ICAQAR\n" +
            "OXnYwu3g1ZjHyley3fZI5aLPsaE17cOImVTehC8DcIphm2HOMR/hYTTL+V0G4P+u\n" +
            "gH+6xeRLKSHMHZTtSBIa6GDL03434y9CBuwGvAFCMU2GV8w92/Z7apkAhdLToZA/\n" +
            "X/iWP2jeaVJhxgEcH8uPrnSlqoPBcKC9PrgUzQYfSZJkLmB+3jEa3HKruy1abJP5\n" +
            "gAdQvwvcPpvYRnIzUc9fZODsVmlHVFBCl2dlu/iHh2h4GmL4Da2rRkUMlbVTdioB\n" +
            "UYIvMycdOkpH5wJftzw7cpjsudGas0PARDXCFfGyKhwBRFY7Xp7lbjtU5Rz0Gc04\n" +
            "lPrhDf0pFE98Aw4jJRpFeWMjpXUEaG1cq7D641RpgcMfPFvOHY47rvDTS7XJOaUT\n" +
            "BwRjmDt896s6vMDcaG/uXJbQjuzmmx3W2Idyh3s5SI0GTHb0IwMKYb4eBUIpQOnB\n" +
            "cE77VnCYqKvN1NVYAqhWjXbY7XasZvszCRcOG+W3FqNaHOK/n/0ueb0uijdLan+U\n" +
            "f4p1bjbAox8eAOQS/8a3bzkJzdyBNUKGx1BIK2IBL9bn/HravSDOiNRSnZ/R3l9G\n" +
            "ZauX0tu7IIDlRCILXSyeazu0aj/vdT3YFQXPcvt5Fkf5wiNTo53f72/jYEJd6qph\n" +
            "WrpoKqrwGwTpRUCMhYIUt65hsTxCiJJ5nKe39h46sg==\n" +
            "-----END CERTIFICATE-----";

    public static boolean KITKAT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    public static final class Path {
        public static final String APP_DIR = Environment.getExternalStorageDirectory()
                + "/OpenKeychain";
        public static final String APP_DIR_FILE = APP_DIR + "/export.asc";
    }

    public static final class Pref {
        public static final String DEFAULT_ENCRYPTION_ALGORITHM = "defaultEncryptionAlgorithm";
        public static final String DEFAULT_HASH_ALGORITHM = "defaultHashAlgorithm";
        public static final String DEFAULT_ASCII_ARMOR = "defaultAsciiArmor";
        public static final String DEFAULT_MESSAGE_COMPRESSION = "defaultMessageCompression";
        public static final String DEFAULT_FILE_COMPRESSION = "defaultFileCompression";
        public static final String PASSPHRASE_CACHE_TTL = "passphraseCacheTtl";
        public static final String LANGUAGE = "language";
        public static final String FORCE_V3_SIGNATURES = "forceV3Signatures";
        public static final String KEY_SERVERS = "keyServers";
    }

    public static final class Defaults {
        public static final String KEY_SERVERS = "pool.sks-keyservers.net, subkeys.pgp.net, pgp.mit.edu";
    }

    public static final class DrawerItems {
        public static final Class KEY_LIST = KeyListActivity.class;
        public static final Class ENCRYPT = EncryptActivity.class;
        public static final Class DECRYPT = DecryptActivity.class;
        public static final Class REGISTERED_APPS_LIST = AppsListActivity.class;
        public static final Class[] ARRAY = new Class[]{
                KEY_LIST,
                ENCRYPT,
                DECRYPT,
                REGISTERED_APPS_LIST
        };
    }

    public static final class choice {
        public static final class algorithm {
            // TODO: legacy reasons :/ better: PublicKeyAlgorithmTags
            public static final int dsa = 0x21070001;
            public static final int elgamal = 0x21070002;
            public static final int rsa = 0x21070003;
        }

        public static final class compression {
            // TODO: legacy reasons :/ better: CompressionAlgorithmTags.UNCOMPRESSED
            public static final int none = 0x21070001;
            public static final int zlib = CompressionAlgorithmTags.ZLIB;
            public static final int bzip2 = CompressionAlgorithmTags.BZIP2;
            public static final int zip = CompressionAlgorithmTags.ZIP;
        }
    }

    public static final class key {
        public static final int none = 0;
        public static final int symmetric = -1;
    }
}

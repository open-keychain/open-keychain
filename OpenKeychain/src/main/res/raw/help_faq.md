[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

# Frequently Asked Questions

## Are my secret keys safe on my mobile device?

This is a very common question, and it's not an easy one. In the end it comes down to how much you trust your mobile device.
The real question usually isn't, "how safe are they", but rather "are they less safe than on my laptop"? The answer depends on three factors:

 1. Do you trust the hardware? Obviously, there are no guarantees that the vendor of your phone hardware didn't add some kind of backdoor.
    Then again, the same applies to your laptop's hardware, so it's about even.
 2. How easily can the device be stolen? This depends a lot on how careful you are, but this too is probably about even with your laptop.
 3. Do you trust the software? The Android operating system actually offers a lot more in the way of security between applications than desktop operating systems.
    No app without root privileges besides OpenKeychain can ever access the keys stored in OpenKeychain's database.
    By comparison, any program you run on your computer can just upload your gnupg keyring, if those files belong to the same user.
    As long as Android as a platform is trustworthy, your keys are safe from malware apps.

In conclusion, we believe that secret keys are not notably less safe on your mobile than they would be on your laptop.
If your security requirements are high enough that you don't keep your keys on your laptop, you probably shouldn't put them on your mobile either.
Otherwise, they should be fine.

## What is the best way to transfer my own key to OpenKeychain?

Ideally, put the key on an sd card, import, then erase from the sd card.
If your mobile does not have an sd card reader, read on.

Our recommended method is to transfer the exported key "through the cloud", but with a super-safe passphrase which is only used during the transfer.
Your key is **encrypted with its passphrase**, the only visible parts in the exported file are your public key.

So is this really safe? The answer is: Yes, IF you use a good passphrase.
If your passphrase is as difficult to guess as your key, an attacker will gain no useful information from your exported key file.
To give you a (very!) rough impression, the passphrase "J0hnnnyy1995" is about a third as difficult to guess as a 2048 bit RSA key, while "%aBbaf11!o9$pP2,o9/=" is about the same.

 1. Make up a long and complex passphrase to use during the transfer.
    It should be at least 20 characters (more is better, although more than 50 is overkill), with varying capitalization, many special characters and *no words from the dictionary*.
    Yes, it is annoying to type, but you'll only use it once!
    You can also write it down, but make sure to destroy the note afterwards, and make sure it is never transferred over the internet!
 2. Change the passphrase of your key to that one, then export
 3. Transfer the key file to your mobile by whatever way is most convenient to you (Mail to yourself, PushBullet, Dropbox, ...)
 4. Import the key with OpenKeychain, then delete the file from your storage.
 5. **Change the passphrase** to an easier one which is still safe, but more reasonable to type.
    
## Should I certify a key without manually comparing fingerprints?

To certify someone's key, you should make sure that it's really that same key the other person wants you to certify with their name on it.

Since keys are usually obtained from a keyserver, it is necessary to double-check that the keyserver gave you the correct key.
This is traditionally done by manually comparing the key's entire fingerprint, character by character.

However, scanning a QR code, receiving a key via NFC, or exchanging keys via SafeSlinger all have that same check already built-in, so as long as you trust the method used for key exchange, there is no reason to check the fingerprint again manually.

## Can I mark public keys as trusted without certifying them with my own key?

No. You can, however, simply create a new key just for certification, which will essentially be the same thing.
    

# Avanced Questions

## Why is OpenKeychain's database not password protected?

Your keys are already encrypted with their passphrase - that's the reason you have to input it for every crypto operation.
There is no point in encrypting those keys again with another password, so password protecting the entire database would only protect the list of public keys.
If this is important to you, consider using [full disk encryption](https://source.android.com/devices/tech/security/encryption/).

## How can I specify connection port for Keyserver?

Add a new Keyserver (or modify existing one) by going to Preferences -> General -> Keyservers. Enter the port number after the Keyserver address and preceded it by a colon.
For example, "p80.pool.sks-keyservers.net:80" (without quotation marks) means that server "p80.pool.sks-keyservers.net" is working on a port 80.
Default connection port is 11371 and it doesn't need to be specified.

## I have more than one subkey capable of singing. Which one is selected when signing with this OpenPGP key?

OpenKeychain assumes that OpenPGP keys hold one usable signing subkey only and selects the first non-revoked non-expired non-stripped one it finds in the unordered list of subkeys.
We consider having more than one valid signing subkey an advanced usecase. You can either strip subkeys that should not be used using OpenKeychain's edit key screen or explicitly select the right subkeys when exporting from gpg with ``gpg --export-secret-subkeys``.

## How to prepare a YubiKey NEO for OpenKeychain?

  1. [Buy a YubiKey NEO](http://www.yubico.com/support/resellers/)
  2. [Prepare it for usage with OpenPGP using GnuPG and Yubico's tools](http://www.yubico.com/2012/12/yubikey-neo-openpgp/).
  3. Export the keypair from GnuPG with
     ```
     gpg -a --output gpg-secret-key.asc --export-secret-keys <insert key id or name>
     ```
     and transfer the file to your Android device.
  4. In OpenKeychain, select "Import from file", select the file and import the keypair. It will be automatically detect that this is a keypair that works with a YubiKey only.

You can now use your YubiKey with OpenKeychain and compatible [apps](http://www.openkeychain.org/apps/). A screen will appear when you need to hold your YubiKey against the NFC antenna.

## How to use a different YubiKey PIN?
  1. Deselect "Use default YubiKey PIN" in OpenKeychain's advanced settings screen
  2. Follow [https://developers.yubico.com/ykneo-openpgp/CardEdit.html](https://developers.yubico.com/ykneo-openpgp/CardEdit.html)

## How to import an existing key onto the YubiKey?
Follow [https://developers.yubico.com/ykneo-openpgp/KeyImport.html](https://developers.yubico.com/ykneo-openpgp/KeyImport.html)

## Advanced YubiKey Infos
  * [https://developers.yubico.com/ykneo-openpgp](https://developers.yubico.com/ykneo-openpgp)
  * [https://github.com/Yubico/ykneo-openpgp](https://github.com/Yubico/ykneo-openpgp)

## Where can I find more information about OpenKeychain's security model and design decisions?

Head over to our [Wiki](https://github.com/open-keychain/open-keychain/wiki).



# Known Issues

### Importing secret key fails

Before posting a new bug report, please check if you are using gpg prior to 2.1.0 and changed the expiry date before exporting the secret key.

Changing the expiry date of a key in gpg prior to version 2.1.0 breaks the secret key in a way which emerges only on export.
It's not a problem with OpenKeychain, we correctly reject the key because its self-certificates are either invalid, or have wrong flags.

This issue has been reported before ([#996](https://github.com/open-keychain/open-keychain/issues/996), [#1003](https://github.com/open-keychain/open-keychain/issues/1003), [#1026](https://github.com/open-keychain/open-keychain/issues/1026)), and can be assumed to affect a large number of users.
The bug in gpg has been fixed in gpg 2.1.0, but that version is as of now [only deployed in debian experimental](https://packages.debian.org/search?keywords=gnupg2), not even sid.
Another [bug report](https://bugs.g10code.com/gnupg/issue1817) has been opened to backport the fix, so we hope this gets fixed soonish.

## A wrong primary user id is shown when searching on a Keyserver

Unfortunately, this is a bug in the SKS Keyserver software. Its machine-readable output returns the user ids in an arbitrary order. Read the [related bug](https://bitbucket.org/skskeyserver/sks-keyserver/issue/28/primary-uid-in-machine-readable-index) report for more information.

### Not working with AOSP Mail

For now, OpenKeychain will not support AOSP Mail due to bugs in AOSP were we cannot work around ([#290](https://github.com/open-keychain/open-keychain/issues/290)).


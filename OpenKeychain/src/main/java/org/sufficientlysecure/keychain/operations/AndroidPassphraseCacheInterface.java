package org.sufficientlysecure.keychain.operations;


import android.content.Context;

import org.sufficientlysecure.keychain.Constants.key;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.pgp.PassphraseCacheInterface;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.Passphrase;


public class AndroidPassphraseCacheInterface implements PassphraseCacheInterface {
    public static AndroidPassphraseCacheInterface getInstance(Context context) {
        KeyRepository keyRepository = KeyRepository.create(context);
        return new AndroidPassphraseCacheInterface(context, keyRepository);
    }

    private Context context;
    private KeyRepository keyRepository;

    private AndroidPassphraseCacheInterface(Context context, KeyRepository keyRepository) {
        this.context = context;
        this.keyRepository = keyRepository;
    }

    @Override
    public Passphrase getCachedPassphrase(long subKeyId) {
        if (subKeyId != key.symmetric) {
            Long masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(subKeyId);
            if (masterKeyId == null) {
                return null;
            }
            return getCachedPassphrase(masterKeyId, subKeyId);
        }
        return getCachedPassphrase(key.symmetric, key.symmetric);
    }

    @Override
    public Passphrase getCachedPassphrase(long masterKeyId, long subKeyId) {
        try {
            return PassphraseCacheService.getCachedPassphrase(context, masterKeyId, subKeyId);
        } catch (PassphraseCacheService.KeyNotFoundException e) {
            return null;
        }
    }
}

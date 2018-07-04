package org.sufficientlysecure.keychain.model;


import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.KeyRingsPublicModel;

@AutoValue
public abstract class KeyRingPublic implements KeyRingsPublicModel {
    public static final Factory<KeyRingPublic> FACTORY = new Factory<>(AutoValue_KeyRingPublic::new);

    public static final Mapper<KeyRingPublic> MAPPER = new Mapper<>(FACTORY);
}

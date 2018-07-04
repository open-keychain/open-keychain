package org.sufficientlysecure.keychain.model;


import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.KeySignaturesModel;


@AutoValue
public abstract class KeySignature implements KeySignaturesModel {
    public static final Factory<KeySignature> FACTORY = new Factory<>(AutoValue_KeySignature::new);

    public static final Mapper<KeySignature> MAPPER = new Mapper<>(FACTORY);
}

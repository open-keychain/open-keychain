package org.sufficientlysecure.keychain.model;


import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.KeySignaturesModel;
import org.sufficientlysecure.keychain.OverriddenWarningsModel;


@AutoValue
public abstract class OverriddenWarning implements OverriddenWarningsModel {
    public static final Factory<OverriddenWarning> FACTORY = new Factory<>(AutoValue_OverriddenWarning::new);

    public static final Mapper<OverriddenWarning> MAPPER = new Mapper<>(FACTORY);
}

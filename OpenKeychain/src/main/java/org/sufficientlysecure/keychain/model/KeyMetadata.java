package org.sufficientlysecure.keychain.model;


import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.KeyMetadataModel;


@AutoValue
public abstract class KeyMetadata implements KeyMetadataModel {
    public static final Factory<KeyMetadata> FACTORY = new Factory<>(
            AutoValue_KeyMetadata::new, CustomColumnAdapters.DATE_ADAPTER);

    public boolean hasBeenUpdated() {
        return last_updated() != null;
    }

    public boolean isPublished() {
        if (last_updated() == null) {
            throw new IllegalStateException("Cannot get publication state if key has never been updated!");
        }
        Boolean seenOnKeyservers = seen_on_keyservers();
        return seenOnKeyservers != null && seenOnKeyservers;
    }
}

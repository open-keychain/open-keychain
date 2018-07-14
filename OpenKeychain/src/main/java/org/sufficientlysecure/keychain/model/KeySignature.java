package org.sufficientlysecure.keychain.model;


import android.arch.persistence.db.SupportSQLiteDatabase;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.KeySignaturesModel;


@AutoValue
public abstract class KeySignature implements KeySignaturesModel {
    public static final Factory<KeySignature> FACTORY = new Factory<>(AutoValue_KeySignature::new);

    public static final Mapper<KeySignature> MAPPER = new Mapper<>(FACTORY);

    public static InsertKeySignature createInsertStatement(SupportSQLiteDatabase db) {
        return new InsertKeySignature(db);
    }

    public void bindTo(InsertKeySignature statement) {
        statement.bind(master_key_id(), signer_key_id());
    }

    public static KeySignature create(long masterKeyId, long certId) {
        return new AutoValue_KeySignature(masterKeyId, certId);
    }
}

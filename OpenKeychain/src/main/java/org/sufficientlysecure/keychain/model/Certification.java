package org.sufficientlysecure.keychain.model;


import java.util.Date;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.CertsModel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;


@AutoValue
public abstract class Certification implements CertsModel {
    public static final CertsModel.Factory<Certification> FACTORY =
            new CertsModel.Factory<>(AutoValue_Certification::new, CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER);

    public static final SelectVerifyingCertDetailsMapper<CertDetails> CERT_DETAILS_MAPPER =
            new SelectVerifyingCertDetailsMapper<>(AutoValue_Certification_CertDetails::new);

    public static Certification create(long masterKeyId, long rank, long keyIdCertifier, long type,
            VerificationStatus verified, Date creation, byte[] data) {
        long creationUnixTime = creation.getTime() / 1000;
        return new AutoValue_Certification(masterKeyId, rank, keyIdCertifier, type, verified, creationUnixTime, data);
    }

    public static InsertCert createInsertStatement(SupportSQLiteDatabase db) {
        return new InsertCert(db, FACTORY);
    }

    public void bindTo(InsertCert statement) {
        statement.bind(master_key_id(), rank(), key_id_certifier(), type(), verified(), creation(), data());
    }

    @AutoValue
    public static abstract class CertDetails implements CertsModel.SelectVerifyingCertDetailsModel {

    }
}

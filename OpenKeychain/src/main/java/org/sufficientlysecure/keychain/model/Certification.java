package org.sufficientlysecure.keychain.model;


import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.CertsModel;


@AutoValue
public abstract class Certification implements CertsModel {
    public static final CertsModel.Factory<Certification> FACTORY =
            new CertsModel.Factory<>(AutoValue_Certification::new, CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER);

    public static final SelectVerifyingCertDetailsMapper<CertDetails> CERT_DETAILS_MAPPER =
            new SelectVerifyingCertDetailsMapper<>(AutoValue_Certification_CertDetails::new);

    @AutoValue
    public static abstract class CertDetails implements CertsModel.SelectVerifyingCertDetailsModel {

    }
}

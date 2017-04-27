package org.sufficientlysecure.keychain.remote.ui;


import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

import org.sufficientlysecure.keychain.pgp.DecryptVerifySecurityProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureBitStrength;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureSigningAlgorithm;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureEncryptionAlgorithm;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.MissingMdc;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.NotWhitelistedCurve;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.EncryptionAlgorithmProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.UnidentifiedKeyProblem;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;


class SecurityProblemPresenter {
    private final Context context;
    private final PackageManager packageManager;


    private RemoteSecurityProblemView view;
    private Long viewKeyMasterKeyId;


    SecurityProblemPresenter(Context context) {
        this.context = context;
        packageManager = context.getPackageManager();
    }

    public void setView(RemoteSecurityProblemView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, Serializable securityProblem) {
        if (securityProblem instanceof DecryptVerifySecurityProblem) {
            setupFromDecryptVerifySecurityProblem((DecryptVerifySecurityProblem) securityProblem);
        } else {
            throw new IllegalArgumentException("Unhandled security problem type!");
        }

        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            throw new IllegalStateException("Unable to find info of calling app!", e);
        }
    }

    private void setupFromDecryptVerifySecurityProblem(DecryptVerifySecurityProblem securityProblem) {
        if (securityProblem.encryptionKeySecurityProblem != null) {
            setupFromEncryptionKeySecurityProblem(securityProblem.encryptionKeySecurityProblem);
        } else if (securityProblem.signingKeySecurityProblem != null) {
            setupFromSigningKeySecurityProblem(securityProblem.signingKeySecurityProblem);
        } else if (securityProblem.symmetricSecurityProblem != null) {
            setupFromEncryptionAlgorithmSecurityProblem(securityProblem.symmetricSecurityProblem);
        } else if (securityProblem.signatureSecurityProblem != null) {
            setupFromSignatureSecurityProblem(securityProblem.signatureSecurityProblem);
        }
    }

    private void setupFromEncryptionKeySecurityProblem(KeySecurityProblem keySecurityProblem) {
        viewKeyMasterKeyId = keySecurityProblem.masterKeyId;
        view.showViewKeyButton();

        if (keySecurityProblem instanceof InsecureBitStrength) {
            InsecureBitStrength problem = (InsecureBitStrength) keySecurityProblem;
            view.showLayoutEncryptInsecureBitsize(problem.algorithm, problem.bitStrength);
        } else if (keySecurityProblem instanceof NotWhitelistedCurve) {
            NotWhitelistedCurve problem = (NotWhitelistedCurve) keySecurityProblem;
            view.showLayoutEncryptNotWhitelistedCurve(problem.curveOid);
        } else if (keySecurityProblem instanceof UnidentifiedKeyProblem) {
            view.showLayoutEncryptUnidentifiedKeyProblem();
        } else {
            throw new IllegalArgumentException("Unhandled key security problem type!");
        }
    }

    private void setupFromSigningKeySecurityProblem(KeySecurityProblem keySecurityProblem) {
        viewKeyMasterKeyId = keySecurityProblem.masterKeyId;
        view.showViewKeyButton();

        if (keySecurityProblem instanceof InsecureBitStrength) {
            InsecureBitStrength problem = (InsecureBitStrength) keySecurityProblem;
            view.showLayoutSignInsecureBitsize(problem.algorithm, problem.bitStrength);
        } else if (keySecurityProblem instanceof NotWhitelistedCurve) {
            NotWhitelistedCurve problem = (NotWhitelistedCurve) keySecurityProblem;
            view.showLayoutSignNotWhitelistedCurve(problem.curveOid);
        } else if (keySecurityProblem instanceof UnidentifiedKeyProblem) {
            view.showLayoutSignUnidentifiedKeyProblem();
        } else {
            throw new IllegalArgumentException("Unhandled key security problem type!");
        }
    }

    private void setupFromEncryptionAlgorithmSecurityProblem(EncryptionAlgorithmProblem securityProblem) {
        if (securityProblem instanceof MissingMdc) {
            view.showLayoutMissingMdc();
        } else if (securityProblem instanceof InsecureEncryptionAlgorithm) {
            InsecureEncryptionAlgorithm insecureSymmetricAlgorithm = (InsecureEncryptionAlgorithm) securityProblem;
            view.showLayoutInsecureSymmetric(insecureSymmetricAlgorithm.symmetricAlgorithm);
        } else {
            throw new IllegalArgumentException("Unhandled symmetric algorithm problem type!");
        }
    }

    private void setupFromSignatureSecurityProblem(InsecureSigningAlgorithm signatureSecurityProblem) {
        view.showLayoutInsecureHashAlgorithm(signatureSecurityProblem.hashAlgorithm);
    }

    private void setPackageInfo(String packageName) throws NameNotFoundException {
        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        // CharSequence appName = packageManager.getApplicationLabel(applicationInfo);

        view.setTitleClientIcon(appIcon);
    }

    void onClickGotIt() {
        view.finishAsCancelled();
    }

    void onClickViewKey() {
        Intent viewKeyIntent = new Intent(context, ViewKeyActivity.class);
        viewKeyIntent.setData(KeyRings.buildGenericKeyRingUri(viewKeyMasterKeyId));
        context.startActivity(viewKeyIntent);
    }

    void onCancel() {
        view.finishAsCancelled();
    }

    interface RemoteSecurityProblemView {
        void finishAsCancelled();
        void setTitleClientIcon(Drawable drawable);

        void showLayoutEncryptInsecureBitsize(int algorithmId, int bitStrength);
        void showLayoutEncryptNotWhitelistedCurve(String curveOid);
        void showLayoutEncryptUnidentifiedKeyProblem();
        void showLayoutSignInsecureBitsize(int algorithmId, int bitStrength);
        void showLayoutSignNotWhitelistedCurve(String curveOid);
        void showLayoutSignUnidentifiedKeyProblem();

        void showLayoutMissingMdc();
        void showLayoutInsecureSymmetric(int symmetricAlgorithm);

        void showLayoutInsecureHashAlgorithm(int hashAlgorithm);

        void showViewKeyButton();
    }
}

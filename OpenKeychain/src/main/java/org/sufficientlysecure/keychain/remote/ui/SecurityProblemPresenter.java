package org.sufficientlysecure.keychain.remote.ui;


import java.io.Serializable;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureBitStrength;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureHashAlgorithm;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureSymmetricAlgorithm;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.MissingMdc;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.NotWhitelistedCurve;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.SymmetricAlgorithmProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.UnidentifiedKeyProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.UsageType;
import org.sufficientlysecure.keychain.util.Log;


class SecurityProblemPresenter {
    private final PackageManager packageManager;


    private RemoteSecurityProblemView view;


    SecurityProblemPresenter(Context context) {
        packageManager = context.getPackageManager();
    }

    public void setView(RemoteSecurityProblemView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, Serializable securityProblem) {

        if (securityProblem instanceof KeySecurityProblem) {
//            setupFromKeySecurityProblem((KeySecurityProblem) securityProblem);
        } else if (securityProblem instanceof SymmetricAlgorithmProblem) {
            setupFromNonKeySecurityProblem((SymmetricAlgorithmProblem) securityProblem);
        } else if (securityProblem instanceof InsecureHashAlgorithm) {
            setupFromInsecureHashAlgorithm((InsecureHashAlgorithm) securityProblem);
        } else {
            throw new IllegalArgumentException("Unhandled security problem type!");
        }

        try {
            setPackageInfo(packageName);
        } catch (NameNotFoundException e) {
            throw new IllegalStateException("Unable to find info of calling app!", e);
        }
    }

    /*
    private void setupFromKeySecurityProblem(KeySecurityProblem keySecurityProblem) {
        if (keySecurityProblem instanceof InsecureBitStrength) {
            InsecureBitStrength problem = (InsecureBitStrength) keySecurityProblem;
            if (problem.usageType == UsageType.ENCRYPT) {
                view.showLayoutEncryptInsecureBitsize(problem.algorithm, problem.bitStrength);
            } else if (problem.usageType == UsageType.SIGN) {
                view.showLayoutSignInsecureBitsize(problem.algorithm, problem.bitStrength);
            } else {
                throw new IllegalStateException("Should never happen here!");
            }
        } else if (keySecurityProblem instanceof NotWhitelistedCurve) {
            NotWhitelistedCurve problem = (NotWhitelistedCurve) keySecurityProblem;
            if (problem.usageType == UsageType.ENCRYPT) {
                view.showLayoutEncryptNotWhitelistedCurve(problem.curveOid);
            } else if (problem.usageType == UsageType.SIGN) {
                view.showLayoutSignNotWhitelistedCurve(problem.curveOid);
            } else {
                throw new IllegalStateException("Should never happen here!");
            }
        } else if (keySecurityProblem instanceof UnidentifiedKeyProblem) {
            if (keySecurityProblem.usageType == UsageType.ENCRYPT) {
                view.showLayoutEncryptUnidentifiedKeyProblem();
            } else if (keySecurityProblem.usageType == UsageType.SIGN) {
                view.showLayoutSignUnidentifiedKeyProblem();
            } else {
                throw new IllegalStateException("Should never happen here!");
            }
        } else {
            throw new IllegalArgumentException("Unhandled key security problem type!");
        }
    }
    */

    private void setupFromNonKeySecurityProblem(SymmetricAlgorithmProblem securityProblem) {
        if (securityProblem instanceof MissingMdc) {
            view.showLayoutMissingMdc();
        } else if (securityProblem instanceof InsecureSymmetricAlgorithm) {
            InsecureSymmetricAlgorithm insecureSymmetricAlgorithm = (InsecureSymmetricAlgorithm) securityProblem;
            view.showLayoutInsecureSymmetric(insecureSymmetricAlgorithm.symmetricAlgorithm);
        } else {
            throw new IllegalArgumentException("Unhandled symmetric algorithm problem type!");
        }
    }

    private void setupFromInsecureHashAlgorithm(InsecureHashAlgorithm securityProblem) {
        view.showLayoutInsecureHashAlgorithm(securityProblem.hashAlgorithm);
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
    }
}

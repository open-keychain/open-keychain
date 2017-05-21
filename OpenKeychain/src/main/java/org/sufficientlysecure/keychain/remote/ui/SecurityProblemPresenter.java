/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import org.sufficientlysecure.keychain.provider.OverriddenWarningsRepository;
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity;


class SecurityProblemPresenter {
    private static final int OVERRIDE_REQUIRED_COUNT = 3;


    private final Context context;
    private final PackageManager packageManager;
    private final OverriddenWarningsRepository overriddenWarningsRepository;


    private RemoteSecurityProblemView view;
    private Long viewKeyMasterKeyId;
    private int overrideCounter;
    private String securityProblemIdentifier;

    private String packageName;
    private Serializable securityProblem;
    private boolean supportOverride;


    SecurityProblemPresenter(Context context) {
        this.context = context;
        packageManager = context.getPackageManager();
        overriddenWarningsRepository = OverriddenWarningsRepository.createOverriddenWarningsRepository(context);
    }

    public void setView(RemoteSecurityProblemView view) {
        this.view = view;
    }

    void setupFromIntentData(String packageName, Serializable securityProblem, boolean supportOverride) {
        this.packageName = packageName;
        this.securityProblem = securityProblem;
        this.supportOverride = supportOverride;

        refreshSecurityProblemDisplay();
        refreshPackageInfo();
    }

    private void refreshSecurityProblemDisplay() {
        if (securityProblem instanceof DecryptVerifySecurityProblem) {
            setupFromDecryptVerifySecurityProblem((DecryptVerifySecurityProblem) securityProblem);
        } else {
            throw new IllegalArgumentException("Unhandled security problem type!");
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

        if (keySecurityProblem.isIdentifiable()) {
            securityProblemIdentifier = keySecurityProblem.getIdentifier();
            refreshOverrideStatusView();
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

        if (keySecurityProblem.isIdentifiable()) {
            securityProblemIdentifier = keySecurityProblem.getIdentifier();
            refreshOverrideStatusView();
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

        if (securityProblem.isIdentifiable()) {
            securityProblemIdentifier = securityProblem.getIdentifier();
            refreshOverrideStatusView();
        }
    }

    private void refreshOverrideStatusView() {
        if (supportOverride) {
            if (overriddenWarningsRepository.isWarningOverridden(securityProblemIdentifier)) {
                view.showOverrideUndoButton();
            } else {
                view.showOverrideButton();
            }
        }
    }

    private void setupFromSignatureSecurityProblem(InsecureSigningAlgorithm signatureSecurityProblem) {
        view.showLayoutInsecureHashAlgorithm(signatureSecurityProblem.hashAlgorithm);
    }

    private void refreshPackageInfo() {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            throw new IllegalStateException("Could not retrieve package info!");
        }
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        // CharSequence appName = packageManager.getApplicationLabel(applicationInfo);

        view.setTitleClientIcon(appIcon);
    }

    private void incrementOverrideAndDisplayOrTrigger() {
        int overrideCountLeft = OVERRIDE_REQUIRED_COUNT - overrideCounter;
        if (overrideCountLeft > 0) {
            overrideCounter++;
            view.showOverrideMessage(overrideCountLeft);
        } else {
            overriddenWarningsRepository.putOverride(securityProblemIdentifier);
            view.finishAsSuppressed();
        }
    }

    private void resetOverrideStatus() {
        overrideCounter = 0;
        overriddenWarningsRepository.deleteOverride(securityProblemIdentifier);
    }

    void onClickGotIt() {
        view.finishAsCancelled();
    }

    void onClickViewKey() {
        Intent viewKeyIntent = new Intent(context, ViewKeyActivity.class);
        viewKeyIntent.setData(KeyRings.buildGenericKeyRingUri(viewKeyMasterKeyId));
        context.startActivity(viewKeyIntent);
    }

    void onClickOverride() {
        incrementOverrideAndDisplayOrTrigger();
    }

    void onClickOverrideUndo() {
        resetOverrideStatus();
        refreshSecurityProblemDisplay();
    }

    void onClickOverrideBack() {
        resetOverrideStatus();
        refreshSecurityProblemDisplay();
    }

    void onClickOverrideConfirm() {
        incrementOverrideAndDisplayOrTrigger();
    }

    void onCancel() {
        view.finishAsCancelled();
    }

    interface RemoteSecurityProblemView {
        void finishAsCancelled();
        void finishAsSuppressed();
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

        void showOverrideMessage(int countdown);

        void showViewKeyButton();
        void showOverrideButton();
        void showOverrideUndoButton();
    }
}

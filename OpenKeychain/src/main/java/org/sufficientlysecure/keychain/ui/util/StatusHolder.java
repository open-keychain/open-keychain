package org.sufficientlysecure.keychain.ui.util;


import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public interface StatusHolder {

    ImageView getEncryptionStatusIcon();

    TextView getEncryptionStatusText();

    ImageView getSignatureStatusIcon();

    TextView getSignatureStatusText();

    View getSignatureLayout();

    TextView getSignatureUserName();

    TextView getSignatureUserEmail();

    TextView getSignatureAction();

    boolean hasEncrypt();

}

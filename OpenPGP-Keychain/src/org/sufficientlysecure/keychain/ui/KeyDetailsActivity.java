package org.sufficientlysecure.keychain.ui;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongycastle.openpgp.PGPPublicKey;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class KeyDetailsActivity extends SherlockActivity {

	private PGPPublicKey publicKey;
	private TextView mAlgorithm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		setContentView(R.layout.key_view);
		if (extras == null) {
			return;
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		long key = extras.getLong("key");
		
		KeyRings.buildPublicKeyRingsByMasterKeyIdUri(key + "");
		String[] projection = new String[]{""};

		this.publicKey = ProviderHelper.getPGPPublicKeyByKeyId(
				getApplicationContext(), key);

		TextView fingerprint = (TextView) this.findViewById(R.id.fingerprint);
		fingerprint.setText(PgpKeyHelper.shortifyFingerprint(PgpKeyHelper.getFingerPrint(getApplicationContext(), key)));
		String[] mainUserId = splitUserId("");
		
		TextView expiry = (TextView) this.findViewById(R.id.expiry);
		Date expiryDate = PgpKeyHelper.getExpiryDate(publicKey);
		if (expiryDate == null) {
			expiry.setText("");
		} else {
			expiry.setText(DateFormat.getDateFormat(getApplicationContext())
					.format(expiryDate));
		}

		TextView creation = (TextView) this.findViewById(R.id.creation);
		creation.setText(DateFormat.getDateFormat(getApplicationContext())
				.format(PgpKeyHelper.getCreationDate(publicKey)));
		mAlgorithm = (TextView) this.findViewById(R.id.algorithm);
		mAlgorithm.setText(PgpKeyHelper.getAlgorithmInfo(publicKey));

	}

	private String[] splitUserId(String userId) {

		String[] result = new String[]{"", "", ""};
		Log.v("UserID", userId);

		Pattern withComment = Pattern.compile("^(.*) [(](.*)[)] <(.*)>$");
		Matcher matcher = withComment.matcher(userId);
		if (matcher.matches()) {
			result[0] = matcher.group(1);
			result[1] = matcher.group(2);
			result[2] = matcher.group(3);
			return result;
		}

		Pattern withoutComment = Pattern.compile("^(.*) <(.*)>$");
		matcher = withoutComment.matcher(userId);
		if (matcher.matches()) {
			result[0] = matcher.group(1);
			result[1] = matcher.group(2);
			return result;
		}
		return result;
	}
}

package org.sufficientlysecure.keychain.pgp.affirmation.resources;

import android.content.Context;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.affirmation.AffirmationResource;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.net.URI;
import java.util.HashMap;
import java.util.Set;

import de.measite.minidns.Client;
import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.Record.TYPE;
import de.measite.minidns.record.TXT;

public class DnsResouce extends AffirmationResource {

    DnsResouce(Set<String> flags, HashMap<String,String> params, URI uri) {
        super(flags, params, uri);
    }

    public static String generate (Context context, byte[] fingerprint, String nonce) {

        return "pgpid+cookie:"
                + KeyFormattingUtils.convertFingerprintToHex(fingerprint) + ";" + nonce + "";

    }

    @Override
    protected String fetchResource (OperationLog log, int indent) {

        Client c = new Client();
        DNSMessage msg = c.query(new Question("mugenguild.com", TYPE.TXT));
        Record aw = msg.getAnswers()[0];
        TXT txt = (TXT) aw.getPayload();
        Log.d(Constants.TAG, txt.getText());
        return txt.getText();

    }

}

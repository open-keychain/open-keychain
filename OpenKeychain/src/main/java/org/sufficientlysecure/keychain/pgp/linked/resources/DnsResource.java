package org.sufficientlysecure.keychain.pgp.linked.resources;

import android.content.Context;

import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.linked.LinkedResource;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.measite.minidns.Client;
import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.Record.CLASS;
import de.measite.minidns.Record.TYPE;
import de.measite.minidns.record.TXT;

public class DnsResource extends LinkedResource {

    final static Pattern magicPattern =
            Pattern.compile("pgpid\\+cookie=([a-zA-Z0-9]+)(?:#|;)([a-zA-Z0-9]+)");

    String mFqdn;
    CLASS mClass;
    TYPE mType;

    DnsResource(Set<String> flags, HashMap<String, String> params, URI uri,
                String fqdn, CLASS clazz, TYPE type) {
        super(flags, params, uri);

        mFqdn = fqdn;
        mClass = clazz;
        mType = type;
    }

    public static String generateText (Context context, byte[] fingerprint, String nonce) {

        return "pgpid+cookie="
                + KeyFormattingUtils.convertFingerprintToHex(fingerprint) + ";" + nonce + "";

    }

    public static DnsResource createNew (String domain) {
        HashSet<String> flags = new HashSet<String>();
        HashMap<String,String> params = new HashMap<String,String>();
        URI uri = URI.create("dns:" + domain);
        return create(flags, params, uri);
    }

    public static DnsResource create(Set<String> flags, HashMap<String,String> params, URI uri) {
        if ( ! ("dns".equals(uri.getScheme())
                && (flags == null || flags.isEmpty())
                && (params == null || params.isEmpty()))) {
            return null;
        }

        //
        String spec = uri.getSchemeSpecificPart();
        // If there are // at the beginning, this includes an authority - we don't support those!
        if (spec.startsWith("//")) {
            return null;
        }

        String[] pieces = spec.split("\\?", 2);
        // In either case, part before a ? is the fqdn
        String fqdn = pieces[0];
        // There may be a query part
        if (pieces.length > 1) {
            // TODO parse CLASS and TYPE query paramters
        }

        CLASS clazz = CLASS.IN;
        TYPE type = TYPE.TXT;

        return new DnsResource(flags, params, uri, fqdn, clazz, type);
    }

    @Override
    protected String fetchResource (OperationLog log, int indent) {

        Client c = new Client();
        DNSMessage msg = c.query(new Question(mFqdn, mType, mClass));
        Record aw = msg.getAnswers()[0];
        TXT txt = (TXT) aw.getPayload();
        return txt.getText().toLowerCase();

    }

    @Override
    protected Matcher matchResource(OperationLog log, int indent, String res) {
        return magicPattern.matcher(res);
    }
}

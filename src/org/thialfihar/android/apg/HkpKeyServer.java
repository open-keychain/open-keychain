package org.thialfihar.android.apg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.Html;

public class HkpKeyServer extends KeyServer {
    private String mHost;
    private short mPort = 11371;

    // example:
    // pub  2048R/<a href="/pks/lookup?op=get&search=0x887DF4BE9F5C9090">9F5C9090</a> 2009-08-17 <a href="/pks/lookup?op=vindex&search=0x887DF4BE9F5C9090">Jörg Runge &lt;joerg@joergrunge.de&gt;</a>
    public static Pattern PUB_KEY_LINE =
            Pattern.compile("pub +([0-9]+)([a-z]+)/.*?0x([0-9a-z]+).*? +([0-9-]+) +(.+)[\n\r]+((?:    +.+[\n\r]+)*)",
                            Pattern.CASE_INSENSITIVE);
    public static Pattern USER_ID_LINE =
            Pattern.compile("^   +(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public HkpKeyServer(String host) {
        mHost = host;
    }

    public HkpKeyServer(String host, short port) {
        mHost = host;
        mPort = port;
    }

    @Override
    List<KeyInfo> search(String query)
            throws MalformedURLException, IOException {
        Vector<KeyInfo> results = new Vector<KeyInfo>();

        String url = "http://" + mHost + ":" + mPort + "/pks/lookup?op=index&search=" +
                     URLEncoder.encode(query, "utf8");
        URL realUrl = new URL(url);
        URLConnection conn = realUrl.openConnection();
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();

        byte buffer[] = new byte[1 << 16];
        int n = 0;
        while ((n = is.read(buffer)) != -1) {
            raw.write(buffer, 0, n);
        }

        String encoding = conn.getContentEncoding();
        if (encoding == null) {
            encoding = "utf8";
        }
        String data = raw.toString(encoding);
        Matcher matcher = PUB_KEY_LINE.matcher(data);
        while (matcher.find()) {
            KeyInfo info = new KeyInfo();
            info.size = Integer.parseInt(matcher.group(1));
            info.algorithm = matcher.group(2);
            info.keyId = Apg.keyFromHex(matcher.group(3));
            info.fingerPrint = Apg.getFingerPrint(info.keyId);
            String chunks[] = matcher.group(4).split("-");
            info.date = new GregorianCalendar(Integer.parseInt(chunks[0]),
                                              Integer.parseInt(chunks[1]),
                                              Integer.parseInt(chunks[2])).getTime();
            info.userIds = new Vector<String>();
            if (matcher.group(5).startsWith("*** KEY")) {
                info.revoked = matcher.group(5);
            } else {
                String tmp = matcher.group(5).replaceAll("<.*?>", "");
                tmp = Html.fromHtml(tmp).toString();
                info.userIds.add(tmp);
            }
            if (matcher.group(6).length() > 0) {
                Matcher matcher2 = USER_ID_LINE.matcher(matcher.group(6));
                while (matcher2.find()) {
                    String tmp = matcher2.group(1).replaceAll("<.*?>", "");
                    tmp = Html.fromHtml(tmp).toString();
                    info.userIds.add(tmp);
                }
            }
            results.add(info);
        }

        return results;
    }

    @Override
    String get(long keyId)
            throws MalformedURLException, IOException {
        String url = "http://" + mHost + ":" + mPort +
                     "/pks/lookup?op=get&search=0x" + Apg.keyToHex(keyId);
        URL realUrl = new URL(url);
        URLConnection conn = realUrl.openConnection();
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();

        byte buffer[] = new byte[1 << 16];
        int n = 0;
        while ((n = is.read(buffer)) != -1) {
            raw.write(buffer, 0, n);
        }

        String data = raw.toString();
        Matcher matcher = Apg.PGP_PUBLIC_KEY.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}

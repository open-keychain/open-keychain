package org.sufficientlysecure.keychain.util;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import org.sufficientlysecure.keychain.Constants;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by Michał Kępkowski on 11/03/16.
 */
public class OkHttpClientFactory {
    private static OkHttpClient client;

    public static OkHttpClient getSimpleClient(){
        if(client == null){
            client =  new OkHttpClient.Builder().build();
        }
        return client;
    }

    public static OkHttpClient getPinnedSimpleClient(CertificatePinner pinner){
        return new OkHttpClient.Builder()
                .certificatePinner(pinner)
                .build();
    }


    public static OkHttpClient getPinnedClient(URL url, Proxy proxy) throws IOException, TlsHelper.TlsHelperException {

        return new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .proxy(proxy)
                .connectTimeout(30000, TimeUnit.MILLISECONDS)
                .readTimeout(45000, TimeUnit.MILLISECONDS)
                .sslSocketFactory(TlsHelper.getPinnedSslSocketFactory(url))
                .build();
    }

    public static OkHttpClient getClient( Proxy proxy) throws IOException, TlsHelper.TlsHelperException {

        return new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .proxy(proxy)
                .connectTimeout(30000, TimeUnit.MILLISECONDS)
                .readTimeout(45000, TimeUnit.MILLISECONDS)
                .build();
    }

}

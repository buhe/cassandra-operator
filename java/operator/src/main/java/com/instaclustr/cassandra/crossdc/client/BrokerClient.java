package com.instaclustr.cassandra.crossdc.client;

import com.instaclustr.cassandra.operator.k8s.K8sResourceUtils;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.SeedList;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * A Broker Client
 */
@Singleton
public class BrokerClient {

    private ApiClient apiClient;
    public static String NAMESPACE;
    public static String CA;
    public static String TOKEN;

    @Inject
    public BrokerClient() {

        CA = System.getenv("CA");
        TOKEN = System.getenv("TOKEN");
        NAMESPACE = System.getenv("NAMESPACE");
        if (CA == null || TOKEN == null || NAMESPACE == null) {
            throw new IllegalStateException(String.format("CA %s , TOKEN %s , NAMESPACE %s requires \"broker client\" argument.", CA, TOKEN, NAMESPACE));
        }

        try {
            apiClient = ClientBuilder
                    .standard()
                    .setVerifyingSsl(true)
                    .setCertificateAuthority(CA.getBytes())
                    .setAuthentication(new AccessTokenAuthentication(TOKEN))
                    .setOverridePatchFormat("application/json-patch+json").build();
            apiClient.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ApiClient getBrokerClient() {
        return this.apiClient;
    }
}

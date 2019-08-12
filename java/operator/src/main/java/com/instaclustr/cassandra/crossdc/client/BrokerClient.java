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
    public static final String NAMESPACE = "cassandra-operator-broker";

    @Inject
    public BrokerClient() {
        InputStream kubeConfig = this.getClass().getClassLoader().getResourceAsStream("kube-config");
        try {
            apiClient = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new InputStreamReader(kubeConfig))).build();
            apiClient.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ApiClient getBrokerClient() {
        return this.apiClient;
    }
}

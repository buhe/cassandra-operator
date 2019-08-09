package com.instaclustr.cassandra.crossdc;

import com.google.inject.assistedinject.Assisted;
import com.instaclustr.cassandra.operator.configuration.Namespace;
import com.instaclustr.cassandra.operator.k8s.K8sResourceUtils;
import com.instaclustr.cassandra.operator.model.DataCenter;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.SeedSpec;
import com.instaclustr.cassandra.operator.model.key.ConfigMapKey;
import com.instaclustr.k8s.watch.ResourceCache;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ObjectMeta;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class LocalDataSync {
    private final ApiClient apiClient;
    private final CustomObjectsApi customObjectsApi;
    private final String namespace;

    @Inject
    public LocalDataSync(final ApiClient apiClient,
                         final CustomObjectsApi customObjectsApi,
                         @Namespace final String namespace) {
        this.apiClient = apiClient;
        this.customObjectsApi = customObjectsApi;
        this.namespace = namespace;
    }

    public void syncEndpointToCRD(List<String> address) {

        Seed seed = new Seed();
        seed.withMetadata(
                new V1ObjectMeta()
                        .namespace(namespace)
                        .name("seed-" + namespace));
        seed.withApiVersion("stable.instaclustr.com/v1").withKind("CassandraSeed");
        seed.withSpec(new SeedSpec().withAddress(address));

        try {
            K8sResourceUtils.createOrReplaceResource(() -> {
                customObjectsApi.createNamespacedCustomObject("stable.instaclustr.com", "v1", namespace, "cassandra-seeds", seed, null);
            }, () -> {

            });
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }
}

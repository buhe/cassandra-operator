package com.instaclustr.cassandra.crossdc;


import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.instaclustr.cassandra.crossdc.client.BrokerClient;
import com.instaclustr.cassandra.operator.configuration.Namespace;
import com.instaclustr.cassandra.operator.k8s.K8sResourceUtils;
import com.instaclustr.cassandra.operator.k8s.OperatorLabels;
import com.instaclustr.cassandra.operator.model.DataCenter;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.key.DataCenterKey;
import com.instaclustr.guava.EventBusSubscriber;
import com.instaclustr.k8s.watch.ResourceCache;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.models.V1ObjectMeta;

import javax.inject.Inject;

@EventBusSubscriber
public class DataSyncExchange extends AbstractExecutionThreadService {

    private final ResourceCache<DataCenterKey, DataCenter> dataCenterCache;
    private final String namespace;
    private final CustomObjectsApi brokerObjectsApi;
    private final CustomObjectsApi localObjectsApi;

    @Inject
    public DataSyncExchange(BrokerClient brokerClient,
                            ResourceCache<DataCenterKey, DataCenter> dataCenterCache,
                            CustomObjectsApi localObjectsApi,
                            @Namespace final String namespace) {
        this.brokerObjectsApi = new CustomObjectsApi(brokerClient.getBrokerClient());
        this.localObjectsApi = localObjectsApi;

        this.dataCenterCache = dataCenterCache;
        this.namespace = namespace;
    }

    private boolean isLocalCluster(Seed seed) throws InterruptedException {
        if (seed.getMetadata().getLabels() != null) {
            String dataCenter = seed.getMetadata().getLabels().get(OperatorLabels.DATACENTER);
            String namespace = seed.getMetadata().getNamespace();
            return dataCenterCache.get(new DataCenterKey(dataCenter, namespace)) != null;
        } else {
            return false;
        }

    }


    @Subscribe
    void localSeedCrdChanged(LocalSeedChangeEvent seedChangeEvent) {

//        System.out.println("local seed event");

        //1. only update remote local cluster crd
        seedChangeEvent.getSeeds().forEach((seed) -> {
            try {
                if (isLocalCluster(seed)) {
                    V1ObjectMeta oldMetadata = seed.getMetadata();
                    V1ObjectMeta newMetadata = new V1ObjectMeta();
                    newMetadata.setName(oldMetadata.getName());
                    newMetadata.setNamespace(BrokerClient.NAMESPACE);
                    newMetadata.setLabels(oldMetadata.getLabels());
                    newMetadata.setAnnotations(oldMetadata.getAnnotations());
                    seed.setMetadata(newMetadata);
                    // update remote crd
                    K8sResourceUtils.createOrReplaceResource(() -> {
                        brokerObjectsApi.createNamespacedCustomObject("stable.instaclustr.com", "v1", BrokerClient.NAMESPACE, "cassandra-seeds", seed, null);
                        System.out.println("create " + oldMetadata.getName() + " to broker");
                    }, () -> {
                        brokerObjectsApi.replaceNamespacedCustomObject("stable.instaclustr.com", "v1", BrokerClient.NAMESPACE, "cassandra-seeds", oldMetadata.getName(), seed);
                        System.out.println("sync " + oldMetadata.getName() + " to broker");
                    });
                } else {
//                    System.out.println("remote crd skip");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Subscribe
    void remoteSeedCrdChanged(RemoteSeedChangeEvent seedChangeEvent) {
        //1. filter local cluster seed and update other local seed
        seedChangeEvent.getSeeds().forEach((seed) -> {
            try {
                if (!isLocalCluster(seed)) {
                    // update local crd
                    V1ObjectMeta oldMetadata = seed.getMetadata();
                    V1ObjectMeta newMetadata = new V1ObjectMeta();
                    newMetadata.setNamespace(this.namespace);
                    newMetadata.setName(oldMetadata.getName());
                    newMetadata.setLabels(oldMetadata.getLabels());
                    newMetadata.setAnnotations(oldMetadata.getAnnotations());
                    seed.setMetadata(newMetadata);
                    K8sResourceUtils.createOrReplaceResource(() -> {
                        localObjectsApi.createNamespacedCustomObject("stable.instaclustr.com", "v1", namespace, "cassandra-seeds", seed, null);
                        System.out.println("create " + oldMetadata.getName() + " from broker to local");
                    }, () -> {
                        localObjectsApi.replaceNamespacedCustomObject("stable.instaclustr.com", "v1", namespace, "cassandra-seeds", oldMetadata.getName(), seed);
                        System.out.println("sync " + oldMetadata.getName() + " from broker to local");
                    });
                } else {
//                    System.out.println("local crd skip");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    protected void run() throws Exception {

    }
}

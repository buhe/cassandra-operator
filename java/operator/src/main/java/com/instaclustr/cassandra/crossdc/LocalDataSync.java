package com.instaclustr.cassandra.crossdc;

import com.google.common.eventbus.EventBus;
import com.instaclustr.cassandra.operator.configuration.Namespace;
import com.instaclustr.cassandra.operator.k8s.K8sResourceUtils;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.SeedList;
import com.instaclustr.cassandra.operator.model.SeedSpec;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.util.CallGeneratorParams;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.Callable;

@Singleton
public class LocalDataSync implements Callable<Void> {
    private final ApiClient apiClient;
    private final CustomObjectsApi customObjectsApi;
    private final String namespace;
    private final EventBus eventBus;
    private final SharedInformerFactory sharedInformerFactory;

    @Inject
    public LocalDataSync(final ApiClient apiClient,
                         final CustomObjectsApi customObjectsApi,
                         @Namespace final String namespace, EventBus eventBus, SharedInformerFactory sharedInformerFactory) {
        this.apiClient = apiClient;
        this.customObjectsApi = customObjectsApi;
        this.namespace = namespace;
        this.eventBus = eventBus;
        this.sharedInformerFactory = sharedInformerFactory;
    }


    // endpoint -> local crd
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
                // TODO
//                customObjectsApi.patchNamespacedCustomObject()
            });
        } catch (ApiException e) {
            e.printStackTrace();
        }

//        eventBus.post(new LocalSeedChangeEvent(Lists.newArrayList(seed)));
    }

    @Override
    public Void call() throws Exception {

        SharedIndexInformer<Seed> seedInformer =
                this.sharedInformerFactory.sharedIndexInformerFor(
                        (CallGeneratorParams params) -> {
                            try {
                                return customObjectsApi
                                        .listNamespacedCustomObjectCall(
                                                "stable.instaclustr.com",
                                                "v1",
                                                "cassandra-operator-broker",
                                                "cassandra-seeds",
                                                null,
                                                null,
                                                params.resourceVersion,
                                                params.timeoutSeconds,
                                                params.watch,
                                                null,
                                                null
                                        );
                            } catch (ApiException e) {
                                e.printStackTrace();
                                return null;
                            }
                        },
                        Seed.class,
                        SeedList.class);

        seedInformer.addEventHandler(
                new ResourceEventHandler<Seed>() {
                    @Override
                    public void onAdd(Seed node) {
//                        System.out.printf("%s node added!\n", node.getMetadata().getName());
//
//                        System.out.println("-----------currnet node is " + seedInformer.getIndexer().list());
                        eventBus.post(new LocalSeedChangeEvent(seedInformer.getIndexer().list()));
                    }

                    @Override
                    public void onUpdate(Seed oldNode, Seed newNode) {
//                        System.out.printf(
//                                "%s => %s node updated!\n",
//                                oldNode.getMetadata().getName(), newNode.getMetadata().getName());
//
//                        System.out.println("-------------currnet node is " + seedInformer.getIndexer().list());
                        eventBus.post(new LocalSeedChangeEvent(seedInformer.getIndexer().list()));
                    }

                    @Override
                    public void onDelete(Seed node, boolean deletedFinalStateUnknown) {
//                        System.out.printf("%s node deleted!\n", node.getMetadata().getName());
//
//                        System.out.println("-------------currnet node is " + seedInformer.getIndexer().list());
                        eventBus.post(new LocalSeedChangeEvent(seedInformer.getIndexer().list()));
                    }
                });

        sharedInformerFactory.startAllRegisteredInformers();

        System.out.println("--------local seed list----------" + seedInformer.getIndexer().list());

        return null;
    }
}

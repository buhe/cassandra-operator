package com.instaclustr.cassandra.crossdc;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.instaclustr.cassandra.operator.configuration.Namespace;
import com.instaclustr.cassandra.operator.k8s.K8sResourceUtils;
import com.instaclustr.cassandra.operator.k8s.OperatorLabels;
import com.instaclustr.cassandra.operator.k8s.PatchOperation;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.SeedList;
import com.instaclustr.cassandra.operator.model.SeedSpec;
import com.instaclustr.k8s.K8sLabels;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Singleton
public class LocalDataSync implements Callable<Void> {
    private final ApiClient apiClient;
    private final CustomObjectsApi customObjectsApi;
    private final String namespace;
    private final EventBus eventBus;
    private SharedInformerFactory sharedInformerFactory = new SharedInformerFactory();
    private final static Gson gson = new Gson();

    @Inject
    public LocalDataSync(final ApiClient apiClient,
                         final CustomObjectsApi customObjectsApi,
                         @Namespace final String namespace, EventBus eventBus) {
        this.apiClient = apiClient;
        this.customObjectsApi = customObjectsApi;
        this.namespace = namespace;
        this.eventBus = eventBus;
    }


    // endpoint -> local crd
    public void syncEndpointToCRD(String dataCenterName, List<String> address) {

        String seedName = "seed-" + dataCenterName + "-" + namespace;
        Seed seed = new Seed();
        seed.withMetadata(
                new V1ObjectMeta()
                        .namespace(namespace)
                        .putLabelsItem(OperatorLabels.DATACENTER, dataCenterName)
                        .putLabelsItem(K8sLabels.MANAGED_BY, OperatorLabels.OPERATOR_IDENTIFIER)
                        .name(seedName));
        seed.withApiVersion("stable.instaclustr.com/v1").withKind("CassandraSeed");
        seed.withSpec(new SeedSpec().withAddress(address));

        try {
            K8sResourceUtils.createOrReplaceResource(() -> {
                System.out.println("create local crd seed " + address);
                customObjectsApi.createNamespacedCustomObject("stable.instaclustr.com", "v1", namespace, "cassandra-seeds", seed, null);
            }, () -> {
                System.out.println("change local crd seed " + address);
                ArrayList<JsonObject> operations = new ArrayList<>();
                PatchOperation patchOperation = new PatchOperation("/spec/address", address);
                JsonElement element = gson.toJsonTree(patchOperation);
                operations.add((JsonObject) element);
                customObjectsApi.patchNamespacedCustomObject("stable.instaclustr.com", "v1", namespace, "cassandra-seeds", seedName, operations);
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
                                                namespace,
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

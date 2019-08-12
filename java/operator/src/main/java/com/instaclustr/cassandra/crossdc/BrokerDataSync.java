package com.instaclustr.cassandra.crossdc;

import com.google.common.eventbus.EventBus;
import com.instaclustr.cassandra.crossdc.client.BrokerClient;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.SeedList;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.util.CallGeneratorParams;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;

@Singleton
public class BrokerDataSync implements Callable {

    private SharedInformerFactory sharedInformerFactory;
    private BrokerClient brokerClient;
    private EventBus eventBus;

    @Inject
    public BrokerDataSync(SharedInformerFactory sharedInformerFactory, BrokerClient brokerClient, EventBus eventBus) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.brokerClient = brokerClient;
        this.eventBus = eventBus;
    }

    public Void call() throws Exception {

        CustomObjectsApi customObjectsApi = new CustomObjectsApi(this.brokerClient.getBrokerClient());

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
                        System.out.printf("%s node added!\n", node.getMetadata().getName());

                        System.out.println("-----------currnet node is " + seedInformer.getIndexer().list());
                        eventBus.post(new RemoteSeedChangeEvent(seedInformer.getIndexer().list()));
                    }

                    @Override
                    public void onUpdate(Seed oldNode, Seed newNode) {
                        System.out.printf(
                                "%s => %s node updated!\n",
                                oldNode.getMetadata().getName(), newNode.getMetadata().getName());

                        System.out.println("-------------currnet node is " + seedInformer.getIndexer().list());
                        eventBus.post(new RemoteSeedChangeEvent(seedInformer.getIndexer().list()));
                    }

                    @Override
                    public void onDelete(Seed node, boolean deletedFinalStateUnknown) {
                        System.out.printf("%s node deleted!\n", node.getMetadata().getName());

                        System.out.println("-------------currnet node is " + seedInformer.getIndexer().list());
                        eventBus.post(new RemoteSeedChangeEvent(seedInformer.getIndexer().list()));
                    }
                });

        sharedInformerFactory.startAllRegisteredInformers();

        System.out.println("--------remote seed list----------" + seedInformer.getIndexer().list());

        return null;

    }

    public void close() {

    }
}

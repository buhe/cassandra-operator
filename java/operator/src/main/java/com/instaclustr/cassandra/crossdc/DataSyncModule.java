package com.instaclustr.cassandra.crossdc;

import com.google.inject.AbstractModule;
import com.instaclustr.cassandra.crossdc.client.BrokerClient;
import io.kubernetes.client.informer.SharedInformerFactory;

public class DataSyncModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LocalDataSync.class);
        bind(BrokerClient.class);
        bind(BrokerDataSync.class);
        bind(SharedInformerFactory.class).toInstance(new SharedInformerFactory());
    }
}

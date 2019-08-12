package com.instaclustr.cassandra.crossdc;


import com.google.common.eventbus.Subscribe;
import com.instaclustr.cassandra.operator.model.Seed;

import javax.inject.Singleton;

@Singleton
public class DataSyncExchange {

    private boolean isLocalCluster(Seed seed) {
        return true;
    }

    @Subscribe
    void localSeedCrdChanged(LocalSeedChangeEvent seedChangeEvent) {
        //1. only update remote local cluster crd
        seedChangeEvent.getSeeds().forEach((seed) -> {
            if (isLocalCluster(seed)) {
                // update remote crd
            }
        });
    }

    @Subscribe
    void remoteSeedCrdChanged(RemoteSeedChangeEvent seedChangeEvent) {
        //1. filter local cluster seed and update other local seed
        seedChangeEvent.getSeeds().forEach((seed) -> {
            if (!isLocalCluster(seed)) {
                // update local crd
            }
        });

    }
}

package com.instaclustr.cassandra.crossdc;

import com.instaclustr.cassandra.operator.model.Seed;

import java.util.List;

public class RemoteSeedChangeEvent {
    private List<Seed> seeds;

    public RemoteSeedChangeEvent(List<Seed> seeds) {
        this.seeds = seeds;
    }

    public List<Seed> getSeeds() {
        return seeds;
    }
}

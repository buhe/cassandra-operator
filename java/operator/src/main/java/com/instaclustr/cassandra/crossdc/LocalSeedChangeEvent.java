package com.instaclustr.cassandra.crossdc;

import com.instaclustr.cassandra.operator.model.Seed;

import java.util.List;

public class LocalSeedChangeEvent {
    private List<Seed> seeds;

    public LocalSeedChangeEvent(List<Seed> seeds) {
        this.seeds = seeds;
    }

    public List<Seed> getSeeds() {
        return seeds;
    }
}

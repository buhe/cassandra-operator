package com.instaclustr.cassandra.k8s;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class CrossDCSeedProvider implements org.apache.cassandra.locator.SeedProvider {
    private static final Logger logger = LoggerFactory.getLogger(CrossDCSeedProvider.class);

    public CrossDCSeedProvider(final Map<String, String> args) {
    }

    @Override
    public List<InetAddress> getSeeds() {
        // watch cassandra seeds
        return ImmutableList.of();
    }
}

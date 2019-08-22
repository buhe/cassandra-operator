package com.instaclustr.cassandra.k8s;

import com.google.common.collect.ImmutableList;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.SeedList;
import com.squareup.okhttp.Call;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.ApiResponse;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class CrossDCSeedProvider implements org.apache.cassandra.locator.SeedProvider {
    private static final Logger logger = LoggerFactory.getLogger(CrossDCSeedProvider.class);
    private final ApiClient apiClient;
    private final String namespace;
    private final String service;

    private SharedInformerFactory sharedInformerFactory = new SharedInformerFactory();

    public CrossDCSeedProvider(final Map<String, String> args) {
        try {
            service = args.get("service");
            if (service == null) {
                throw new IllegalStateException(String.format("%s requires \"service\" argument.", SeedProvider.class));
            }
            namespace = args.get("namespace");
            if (namespace == null) {
                throw new IllegalStateException(String.format("%s requires \"namespace\" argument.", CrossDCSeedProvider.class));
            }
            apiClient = ClientBuilder.standard().build();
        } catch (Exception e) {
            logger.error("init CrossDCSeedProvider error", e);
            throw new RuntimeException("connect k8s error", e);
        }
    }

    @Override
    public List<InetAddress> getSeeds() {
        try {
            final ImmutableList<InetAddress> localSeedAddresses = ImmutableList.copyOf(InetAddress.getAllByName(service));

            logger.info("Discovered local dc {} seed nodes: {}", localSeedAddresses.size(), localSeedAddresses);

            CustomObjectsApi customObjectsApi = new CustomObjectsApi(apiClient);
            Call call = customObjectsApi.listNamespacedCustomObjectCall("stable.instaclustr.com", "v1", namespace, "cassandra-seeds", null, null, null, 30, false, null, null);
            ApiResponse<SeedList> apiResponse = apiClient.execute(call, SeedList.class);
            SeedList seedList = apiResponse.getData();
            Set<InetAddress> endpoints = new HashSet<>();
            for (Seed seed : seedList.getItems()) {
                if (seed.getSpec().getAddress() != null && !seed.getSpec().getAddress().isEmpty()) {
                    for (String endpoint : seed.getSpec().getAddress()) {
                        endpoints.add(InetAddress.getByName(endpoint));
                    }
                }
            }
            logger.info("Discovered cross dc seed nodes: {} ", endpoints);
            endpoints.addAll(localSeedAddresses);
            logger.info("Discovered all seed nodes: {} ", endpoints);
            return ImmutableList.copyOf(endpoints);
        } catch (Exception e) {
            logger.warn("fetch cross dc seed error ", e);
        }

        logger.info("cassandra seed empty");
        return ImmutableList.of();
    }
}

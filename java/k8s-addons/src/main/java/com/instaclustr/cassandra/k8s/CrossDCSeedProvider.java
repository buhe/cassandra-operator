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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrossDCSeedProvider implements org.apache.cassandra.locator.SeedProvider {
    private static final Logger logger = LoggerFactory.getLogger(CrossDCSeedProvider.class);
    private final ApiClient apiClient;
    private String namespace;

    private SharedInformerFactory sharedInformerFactory = new SharedInformerFactory();

    public CrossDCSeedProvider(final Map<String, String> args) {
        logger.info("init CrossDCSeedProvider");
        System.out.println("init CrossDCSeedProvider");
        try {
            namespace = args.get("namespace");
            if (namespace == null) {
                throw new IllegalStateException(String.format("%s requires \"namespace\" argument.", CrossDCSeedProvider.class));
            }
            apiClient = ClientBuilder.standard().build();
        } catch (Exception e) {
            logger.error("init CrossDCSeedProvider error", e);
            System.out.println("init CrossDCSeedProvider error");
            throw new RuntimeException("connect k8s error", e);
        }
        logger.info("init CrossDCSeedProvider end");
        System.out.println("init CrossDCSeedProvider end");
    }

    @Override
    public List<InetAddress> getSeeds() {
        try {
            CustomObjectsApi customObjectsApi = new CustomObjectsApi(apiClient);
            Call call = customObjectsApi.listNamespacedCustomObjectCall("stable.instaclustr.com", "v1", namespace, "cassandra-seeds", null, null, null, 30, false, null, null);
            ApiResponse<SeedList> apiResponse = apiClient.execute(call, SeedList.class);
            SeedList seedList = apiResponse.getData();
            List<InetAddress> endpoints = new ArrayList<>();
            for (Seed seed : seedList.getItems()) {
                if (seed.getSpec().getAddress() != null && !seed.getSpec().getAddress().isEmpty()) {
                    for (String endpoint : seed.getSpec().getAddress()) {
                        endpoints.add(InetAddress.getByName(endpoint));
                    }
                }
            }
            logger.info("cassandra seed is " + endpoints);
            return ImmutableList.copyOf(endpoints);
        } catch (Exception e) {
            logger.warn("listNamespacedCustomObject seed ", e);
            System.out.println("listNamespacedCustomObject error");
        }

        logger.info("cassandra seed empty");
        System.out.println("cassandra seed empty");
        return ImmutableList.of();
    }
}

package com.instaclustr.cassandra.operator.model.key;

import io.kubernetes.client.models.V1Endpoints;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1beta2StatefulSet;

public class EndpointKey extends Key<V1Endpoints> {
    public EndpointKey(final String name, final String namespace) {
        super(name, namespace);
    }

    public static EndpointKey forStatefulSet(final V1Endpoints endpoints) {
        final V1ObjectMeta metadata = endpoints.getMetadata();

        return new EndpointKey(metadata.getName(), metadata.getNamespace());
    }
}

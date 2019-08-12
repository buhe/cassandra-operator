package com.instaclustr.cassandra.operator.watch;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.instaclustr.cassandra.operator.configuration.Namespace;
import com.instaclustr.cassandra.operator.event.EndpointWatchEvent;
import com.instaclustr.cassandra.operator.k8s.OperatorLabels;
import com.instaclustr.cassandra.operator.model.key.EndpointKey;
import com.instaclustr.k8s.K8sLabels;
import com.instaclustr.k8s.LabelSelectors;
import com.instaclustr.k8s.watch.ResourceCache;
import com.instaclustr.k8s.watch.WatchService;
import com.squareup.okhttp.Call;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Endpoints;
import io.kubernetes.client.models.V1EndpointsList;
import io.kubernetes.client.models.V1ListMeta;
import io.kubernetes.client.models.V1ObjectMeta;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;

public class SeedWatchModule extends AbstractModule {
    @Singleton
    static class EnpointCache extends ResourceCache<EndpointKey, V1Endpoints> {
        @Inject
        public EnpointCache(final EventBus eventBus, final EndpointWatchEvent.Factory eventFactory) {
            super(eventBus, eventFactory);
        }

        @Override
        protected EndpointKey resourceKey(final V1Endpoints statefulSet) {
            return EndpointKey.forStatefulSet(statefulSet);
        }

        @Override
        protected V1ObjectMeta resourceMetadata(final V1Endpoints statefulSet) {
            return statefulSet.getMetadata();
        }
    }

    @Singleton
    static class EndpointWatchService extends WatchService<V1Endpoints, V1EndpointsList, EndpointKey> {
        private final CoreV1Api coreV1Api;
        private final String namespace;

        @Inject
        public EndpointWatchService(final ApiClient apiClient,
                                       final ResourceCache<EndpointKey, V1Endpoints> statefulSetCache,
                                       final CoreV1Api coreV1Api,
                                       @Namespace final String namespace) {
            super(apiClient, statefulSetCache);

            this.coreV1Api = coreV1Api;
            this.namespace = namespace;
        }

        @Override
        protected Call listResources(final String continueToken, final String resourceVersion, final boolean watch) throws ApiException {
            final String labelSelector = LabelSelectors.equalitySelector(K8sLabels.MANAGED_BY, OperatorLabels.OPERATOR_IDENTIFIER);

            return coreV1Api.listNamespacedEndpointsCall(namespace, null, null, continueToken, null, labelSelector, null, resourceVersion, null, watch, null, null);
        }

        @Override
        protected Collection<? extends V1Endpoints> resourceListItems(final V1EndpointsList statefulSetList) {
            return statefulSetList.getItems();
        }

        @Override
        protected V1ListMeta resourceListMetadata(final V1EndpointsList statefulSetList) {
            return statefulSetList.getMetadata();
        }
    }

    @Override
    protected void configure() {
        bind(new TypeLiteral<ResourceCache<EndpointKey, V1Endpoints>>() {
        }).to(EnpointCache.class);

        Multibinder.newSetBinder(binder(), Service.class).addBinding().to(EndpointWatchService.class);

        install(new FactoryModuleBuilder().build(EndpointWatchEvent.Factory.class));
    }
}

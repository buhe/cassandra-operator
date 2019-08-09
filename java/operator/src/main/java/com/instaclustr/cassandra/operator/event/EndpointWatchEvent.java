package com.instaclustr.cassandra.operator.event;

import com.google.inject.assistedinject.Assisted;
import io.kubernetes.client.models.V1Endpoints;
import io.kubernetes.client.models.V1beta2StatefulSet;

import javax.annotation.Nullable;
import javax.inject.Inject;

@SuppressWarnings("WeakerAccess")
public abstract class EndpointWatchEvent extends WatchEvent {
    public final V1Endpoints endpoints;

    protected EndpointWatchEvent(final V1Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    public interface Factory extends WatchEvent.Factory<V1Endpoints> {
        Added createAddedEvent(final V1Endpoints endpoints);
        Modified createModifiedEvent(@Nullable @Assisted("old") final V1Endpoints oldEndpoints, @Assisted("new") final V1Endpoints newEndpoints);
        Deleted createDeletedEvent(final V1Endpoints endpoints);
    }

    public static class Added extends EndpointWatchEvent implements WatchEvent.Added {
        @Inject
        public Added(@Assisted final V1Endpoints endpoints) {
            super(endpoints);
        }
    }

    public static class Modified extends EndpointWatchEvent implements WatchEvent.Modified {
        @Nullable
        public final V1Endpoints oldEndpoints;

        @Inject
        public Modified(@Nullable @Assisted("old") final V1Endpoints oldEndpoints, @Assisted("new") final V1Endpoints newEndpoints) {
            super(newEndpoints);
            this.oldEndpoints = oldEndpoints;
        }
    }

    public static class Deleted extends EndpointWatchEvent implements WatchEvent.Deleted {
        @Inject
        public Deleted(@Assisted final V1Endpoints endpoints) {
            super(endpoints);
        }
    }
}

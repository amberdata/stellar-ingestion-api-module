package io.amberdata.ingestion.api.modules.stellar.state.entities;

import io.amberdata.domain.BlockchainEntity;

public final class BlockchainEntityWithState<T extends BlockchainEntity> {
    private final ResourceState resourceState;
    private final T             entity;

    private BlockchainEntityWithState (T entity, ResourceState resourceState) {
        this.resourceState = resourceState;
        this.entity = entity;
    }

    public static <T extends BlockchainEntity> BlockchainEntityWithState<T> from (T entity, ResourceState resourceState) {
        return new BlockchainEntityWithState<>(entity, resourceState);
    }

    public ResourceState getResourceState () {
        return resourceState;
    }

    public T getEntity () {
        return entity;
    }

    @Override
    public String toString () {
        return "BlockchainEntityWithState{" +
            "resourceState=" + resourceState +
            ", entity=" + entity +
            '}';
    }
}

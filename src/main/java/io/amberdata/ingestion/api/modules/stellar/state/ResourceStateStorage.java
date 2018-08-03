package io.amberdata.ingestion.api.modules.stellar.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.amberdata.ingestion.api.modules.stellar.state.entities.BlockchainEntityWithState;
import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;
import io.amberdata.ingestion.api.modules.stellar.state.entities.ResourceState;

@Component
public class ResourceStateStorage {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceStateStorage.class);

    private final ResourceStateRepository resourceStateRepository;

    public ResourceStateStorage (ResourceStateRepository resourceStateRepository) {
        this.resourceStateRepository = resourceStateRepository;
    }

    public void storeState (BlockchainEntityWithState entityWithState) {
        ResourceState resourceState = entityWithState.getResourceState();

        LOG.info("Going to store state for {} with paging token {}",
            resourceState.getResourceType(),
            resourceState.getPagingToken()
        );

        resourceStateRepository.saveAndFlush(
            ResourceState.from(
                resourceState.getResourceType(),
                resourceState.getPagingToken()
            )
        );
    }

    public String getCursorPointer (Resource resourceType) {
        return resourceStateRepository
            .findById(resourceType)
            .map(ResourceState::getPagingToken)
            .orElse("now");
    }

}

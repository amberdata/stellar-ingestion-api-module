package io.amberdata.ingestion.api.modules.stellar.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.amberdata.domain.Block;
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

    public void storeState (BlockchainEntityWithState<Block> entityWithState) {
        LOG.info("Going to store state for entity {}", entityWithState);

        resourceStateRepository.saveAndFlush(
            ResourceState.from(
                entityWithState.getResourceState().getResourceType(),
                entityWithState.getResourceState().getPagingToken()
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

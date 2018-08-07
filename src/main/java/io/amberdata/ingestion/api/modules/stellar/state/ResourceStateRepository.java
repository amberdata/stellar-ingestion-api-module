package io.amberdata.ingestion.api.modules.stellar.state;

import org.springframework.data.jpa.repository.JpaRepository;

import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;
import io.amberdata.ingestion.api.modules.stellar.state.entities.ResourceState;

public interface ResourceStateRepository extends JpaRepository<ResourceState, Resource> {
}

package io.amberdata.ingestion.api.modules.stellar.state;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntityStateRepository extends JpaRepository<EntityState, String> {
}
package io.amberdata.ingestion.api.modules.stellar.state;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class EntityState {

    @Id
    private String entityType;

    @Column(nullable = false)
    private String lastId;

    private EntityState () {
    }

    private EntityState (String entityType, String lastId) {
        this.entityType = entityType;
        this.lastId = lastId;
    }

    public static EntityState from (String entityType, String lastId) {
        return new EntityState(entityType, lastId);
    }

    public String getEntityType () {
        return entityType;
    }

    public void setEntityType (String entityType) {
        this.entityType = entityType;
    }

    public String getLastId () {
        return lastId;
    }

    public void setLastId (String lastId) {
        this.lastId = lastId;
    }

    @Override
    public String toString () {
        return "EntityState{" +
            "entityType='" + entityType + '\'' +
            ", lastId='" + lastId + '\'' +
            '}';
    }
}

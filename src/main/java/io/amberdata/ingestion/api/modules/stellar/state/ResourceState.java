package io.amberdata.ingestion.api.modules.stellar.state;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ResourceState {

    @Id
    private Resource resourceType;

    @Column(nullable = false)
    private String pagingToken;

    protected ResourceState () {
    }

    private ResourceState (Resource resourceType, String pagingToken) {
        this.resourceType = resourceType;
        this.pagingToken = pagingToken;
    }

    public static ResourceState from (Resource resourceType, String pagingToken) {
        return new ResourceState(resourceType, pagingToken);
    }

    public Resource getResourceType () {
        return resourceType;
    }

    public void setResourceType (Resource resourceType) {
        this.resourceType = resourceType;
    }

    public String getPagingToken () {
        return pagingToken;
    }

    public void setPagingToken (String pagingToken) {
        this.pagingToken = pagingToken;
    }

    @Override
    public String toString () {
        return "ResourceState{" +
            "resourceType='" + resourceType + '\'' +
            ", pagingToken='" + pagingToken + '\'' +
            '}';
    }
}

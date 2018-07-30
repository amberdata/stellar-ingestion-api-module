package io.amberdata.ingestion.api.modules.stellar.mapper;

public final class StellarEntity<T> {
    private String id;
    private String type;
    private T entity;

    private StellarEntity (String id, T entity, String type) {
        this.id = id;
        this.entity = entity;
        this.type = type;
    }

    public static <T> StellarEntity from (String id, T entity) {
        return new StellarEntity<T>(id, entity, entity.getClass().getName());
    }

    public String getId () {
        return id;
    }

    public String getType () {
        return type;
    }

    public T getEntity () {
        return entity;
    }
}

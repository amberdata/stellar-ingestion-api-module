package io.amberdata.ingestion.api.modules.stellar.configuration.subscribers;

public class AccountWithTime {

    private String name;
    private Long timestamp;

    private AccountWithTime (String name, Long timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    public static AccountWithTime from (String name, Long timestamp) {
        return new AccountWithTime(name, timestamp);
    }

    public String getName () {
        return name;
    }

    public Long getTimestamp () {
        return timestamp;
    }
}

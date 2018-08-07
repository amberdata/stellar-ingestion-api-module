package io.amberdata.ingestion.api.modules.stellar.configuration.subscribers;

public class AccountWithTime {

    private String name;
    private String timestamp;

    private AccountWithTime (String name, String timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    public static AccountWithTime from (String name, String timestamp) {
        return new AccountWithTime(name, timestamp);
    }

    public String getName () {
        return name;
    }

    public String getTimestamp () {
        return timestamp;
    }
}

package io.amberdata.domain.operations;

import java.util.Objects;

public class SetOptionsOperation extends Operation {

    private String  inflationDestination;
    private Integer clearFlags;
    private Integer setFlags;
    private Integer masterKeyWeight;
    private Integer lowThreshold;
    private Integer mediumThreshold;
    private Integer highThreshold;
    private String  homeDomain;
    private String  signerKey;
    private Integer signerWeight;

    public SetOptionsOperation (String sourceAccount, String inflationDestination, Integer clearFlags, Integer
        setFlags, Integer masterKeyWeight, Integer lowThreshold, Integer mediumThreshold, Integer highThreshold,
                                String homeDomain, String signerKey, Integer signerWeight) {
        super(sourceAccount);
        this.inflationDestination = inflationDestination;
        this.clearFlags = clearFlags;
        this.setFlags = setFlags;
        this.masterKeyWeight = masterKeyWeight;
        this.lowThreshold = lowThreshold;
        this.mediumThreshold = mediumThreshold;
        this.highThreshold = highThreshold;
        this.homeDomain = homeDomain;
        this.signerKey = signerKey;
        this.signerWeight = signerWeight;
    }

    public String getInflationDestination () {
        return inflationDestination;
    }

    public void setInflationDestination (String inflationDestination) {
        this.inflationDestination = inflationDestination;
    }

    public Integer getClearFlags () {
        return clearFlags;
    }

    public void setClearFlags (Integer clearFlags) {
        this.clearFlags = clearFlags;
    }

    public Integer getSetFlags () {
        return setFlags;
    }

    public void setSetFlags (Integer setFlags) {
        this.setFlags = setFlags;
    }

    public Integer getMasterKeyWeight () {
        return masterKeyWeight;
    }

    public void setMasterKeyWeight (Integer masterKeyWeight) {
        this.masterKeyWeight = masterKeyWeight;
    }

    public Integer getLowThreshold () {
        return lowThreshold;
    }

    public void setLowThreshold (Integer lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public Integer getMediumThreshold () {
        return mediumThreshold;
    }

    public void setMediumThreshold (Integer mediumThreshold) {
        this.mediumThreshold = mediumThreshold;
    }

    public Integer getHighThreshold () {
        return highThreshold;
    }

    public void setHighThreshold (Integer highThreshold) {
        this.highThreshold = highThreshold;
    }

    public String getHomeDomain () {
        return homeDomain;
    }

    public void setHomeDomain (String homeDomain) {
        this.homeDomain = homeDomain;
    }

    public String getSignerKey () {
        return signerKey;
    }

    public void setSignerKey (String signerKey) {
        this.signerKey = signerKey;
    }

    public Integer getSignerWeight () {
        return signerWeight;
    }

    public void setSignerWeight (Integer signerWeight) {
        this.signerWeight = signerWeight;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SetOptionsOperation that = (SetOptionsOperation) o;
        return Objects.equals(inflationDestination, that.inflationDestination) &&
            Objects.equals(clearFlags, that.clearFlags) &&
            Objects.equals(setFlags, that.setFlags) &&
            Objects.equals(masterKeyWeight, that.masterKeyWeight) &&
            Objects.equals(lowThreshold, that.lowThreshold) &&
            Objects.equals(mediumThreshold, that.mediumThreshold) &&
            Objects.equals(highThreshold, that.highThreshold) &&
            Objects.equals(homeDomain, that.homeDomain) &&
            Objects.equals(signerKey, that.signerKey) &&
            Objects.equals(signerWeight, that.signerWeight);
    }

    @Override
    public int hashCode () {
        return Objects.hash(inflationDestination,
            clearFlags, setFlags, masterKeyWeight,
            lowThreshold, mediumThreshold, highThreshold,
            homeDomain, signerKey, signerWeight);
    }
}

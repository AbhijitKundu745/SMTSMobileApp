package com.psllab.smtsmobileapp.databases;

public class LocationMaster {
    String locationId,locationName,locationRfid;

    public String getLocationRfid() {
        return locationRfid;
    }

    public void setLocationRfid(String locationRfid) {
        this.locationRfid = locationRfid;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}

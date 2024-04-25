package com.psllab.smtsmobileapp.databases;

public class InventoryMaster {
    String assetEpc,assetId,assetName,assetStatus;

    public String getAssetEpc() {
        return assetEpc;
    }

    public void setAssetEpc(String assetEpc) {
        this.assetEpc = assetEpc;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetStatus() {
        return assetStatus;
    }

    public void setAssetStatus(String assetStatus) {
        this.assetStatus = assetStatus;
    }
}

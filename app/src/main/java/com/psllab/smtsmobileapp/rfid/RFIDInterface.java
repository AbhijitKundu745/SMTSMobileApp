package com.psllab.smtsmobileapp.rfid;

import com.seuic.uhf.EPC;

import java.util.List;

public interface RFIDInterface {

    public void handleTriggerPress(boolean pressed);
    public void RFIDInitializationStatus(boolean status);
    public void handleLocateTagResponse(int value,int size);
    public void onDataReceived(List<EPC> epcList);
}

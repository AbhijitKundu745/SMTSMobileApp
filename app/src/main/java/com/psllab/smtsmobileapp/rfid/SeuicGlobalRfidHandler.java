package com.psllab.smtsmobileapp.rfid;

import static com.seuic.uhf.UHFService.PARAMETER_HIDE_PC;

import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.psllab.smtsmobileapp.R;
import com.psllab.smtsmobileapp.helper.AssetUtils;
import com.seuic.scankey.IKeyEventCallback;
import com.seuic.scankey.ScanKeyService;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeuicGlobalRfidHandler {

    public UHFService mDevice;
    RFIDInterface rfidInterface;
    private Context context;

    public static final int MAX_LEN = 64;
    private Thread mInventoryThread;
    public boolean mInventoryStart = false;
    private InventoryRunable mInventoryRunable;
    private List<EPC> mEPCList;
    static int m_count = 0;

    private boolean searchFlag = false;
    private Thread mThread;

    // sound
    private static SoundPool mSoundPool;
    private static int soundID;

    public void onCreate(Context activity, RFIDInterface rfidInterface) {//RFIDReader reader) {
        // application context
        context = activity;
        stopApps(context, "com.seuic.uhftool");
        this.rfidInterface = rfidInterface;
        mInventoryRunable = new InventoryRunable();

        mEPCList = new ArrayList<>();

        mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
        soundID = mSoundPool.load(context, R.raw.scan, 1);
        InitSDK();
        // ConfigureReader();
    }

    private void InitSDK() {
        // new object
        mDevice = UHFService.getInstance();
        // open UHF
        boolean ret = mDevice.open();
        if (!ret) {
            rfidInterface.RFIDInitializationStatus(false);
        } else {
            // mDevice.setAntennaMode(1);
            rfidInterface.RFIDInitializationStatus(true);

            try {
                // mDevice.setParameters(UHFService.PARAMETER_CLEAR_EPCLIST, 1);
                mDevice.setPower(30);
                mDevice.setParameters(PARAMETER_HIDE_PC, 1);
                boolean z = mDevice.setParamBytes(UHFService.PARAMETER_TAG_FILTER, null);
                mDevice.setParamBytes(UHFService.PARAMETER_TAG_EMBEDEDDATA, null);
               /* mDevice.setParameters(UHFService.PARAMETER_CLEAR_EPCLIST, 1);
                mDevice.setParameters(UHFService.PARAMETER_CLEAR_EPCLIST_WHEN_START_INVENTORY, 1);
                mDevice.setParameters(UHFService.PARAMETER_EXTENSIONS_TAGFOCUS, 1);
                mDevice.setParameters(UHFService.PARAMETER_INVENTORY_SPEED, 0);*/
            } catch (Exception e) {

                Log.e("INITEXC",""+e.getMessage());
            }
        }
    }

    public void setRFPower(int powertoset){
        try{
            if(mDevice!=null){
                Log.e("POWER",""+powertoset);
                boolean po = mDevice.setPower(powertoset);
            }
        }catch (Exception e){
            Log.e("POWERERR",""+e.getMessage());
        }

    }

    public static void stopApps(Context context, String packageName) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            Method forceStopPackage = am.getClass().getDeclaredMethod("forceStopPackage", String.class);
            forceStopPackage.setAccessible(true);
            forceStopPackage.invoke(am, packageName);
            Log.i("zy", "TimerV force stop package " + packageName + " successful");
            System.out.println("TimerV force stop package " + packageName + " successful");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("TimerV force stop package " + packageName + " error!");
            Log.i("zy", "TimerV force stop package " + packageName + " error!");
        }
    }

    private ScanKeyService mScanKeyService = ScanKeyService.getInstance();

    private IKeyEventCallback mCallback = new IKeyEventCallback.Stub() {
        @Override
        public void onKeyDown(int keyCode) throws RemoteException {
            Log.e("TRIGGER", "onKeyUp: keyCode=" + keyCode);
            rfidInterface.handleTriggerPress(true);
        }

        @Override
        public void onKeyUp(int keyCode) throws RemoteException {
            Log.e("TRIGGER", "onKeyUp: keyCode=" + keyCode);
        }
    };

    public void onResume() {
        mScanKeyService.registerCallback(mCallback, "100,101,102,249,249,250");
        InitSDK();
    }

    public void onDestroy() {
        mScanKeyService.unregisterCallback(mCallback);
        mDevice.close();
    }

    public void onPause() {
        // mDevice.close();
        mScanKeyService.unregisterCallback(mCallback);
        mDevice.close();
    }

    private void refreshData() {

        if (mEPCList != null) {
            // Gets the number inside the list of all labels
            int count = 0;
            for (EPC item : mEPCList) {
                count += item.count;
            }
            if (count > m_count) {
                // playSound();
            }
            m_count = count;
        }
    }

    private void clearList() {
        if (mEPCList != null) {
            mEPCList.clear();
            m_count = 0;
        }
    }

    public void playSound() {
        if (mSoundPool == null) {
            mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 20);
            soundID = mSoundPool.load(context, R.raw.scan, 1);// "/system/media/audio/notifications/Antimony.ogg"
        }
        //TODO uncomment
        mSoundPool.play(soundID, 1, 1, 0, 0, 1);
    }

    private class InventoryRunable implements Runnable {

        @Override
        public void run() {
            while (mInventoryStart) {
                Message message = Message.obtain();// Avoid repeated application of memory, reuse of information
                message.what = 1;
                handler.sendMessage(message);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean stopInventory() {
        mInventoryStart = false;
        isCounitue = false;

        boolean isInventoryStopped = true;

        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        if (mInventoryThread != null) {
            mInventoryThread.interrupt();
            mInventoryThread = null;
        }
        System.out.println("begin stop!!");
        if (mDevice.inventoryStop()) {
            System.out.println("end stop!!");
            isInventoryStopped = true;

        } else {
            isInventoryStopped = false;
            System.out.println("RfidInventoryStop faild.");
        }
        return isInventoryStopped;
    }

    public boolean startInventory() {
        Log.e("COMMAND","INVENTORY");
        searchFlag = false;
        isCounitue = false;
        clearList();
        boolean isInventoryStarted = true;
        if (mInventoryThread != null && mInventoryThread.isAlive()) {
            System.out.println("Thread not null");
            isInventoryStarted = false;
        }

        if (mDevice.inventoryStart()) {
            System.out.println("RfidInventoryStart sucess.");

            mInventoryStart = true;
            mInventoryThread = new Thread(mInventoryRunable);
            mInventoryThread.start();
            isInventoryStarted = true;
            // boolean z = mDevice.setParamBytes(UHFService.PARAMETER_TAG_FILTER, null);

        } else {
            isInventoryStarted = false;
            System.out.println("RfidInventoryStart faild.");
        }
        return isInventoryStarted;
    }


    public boolean startInventoryOnce() {
        searchFlag = false;
        isCounitue = false;
        clearList();
        boolean isInventoryStarted = true;
        if (mInventoryThread != null && mInventoryThread.isAlive()) {
            System.out.println("Thread not null");
            isInventoryStarted = false;
        }

        EPC epc = new EPC();
        if (mDevice.inventoryOnce(epc, 100)) {
            System.out.println("RfidInventoryStart sucess.");

            mInventoryStart = true;
            mInventoryThread = new Thread(mInventoryRunable);
            mInventoryThread.start();
            isInventoryStarted = true;
            // boolean z = mDevice.setParamBytes(UHFService.PARAMETER_TAG_FILTER, null);

        } else {
            isInventoryStarted = false;
            System.out.println("RfidInventoryStart faild.");
        }
        return isInventoryStarted;
    }


    public boolean startSearchInventory(String epcfilter) {
        clearList();
        boolean isInventoryStarted = true;
        if (mInventoryThread != null && mInventoryThread.isAlive()) {
            System.out.println("Thread not null");
            isInventoryStarted = false;
        }

        if (mDevice.inventoryStart()) {
            searchFlag = true;
            isCounitue = true;
            System.out.println("RfidInventoryStart sucess.");

            mInventoryStart = true;
            mInventoryThread = new Thread(mInventoryRunable);
            mInventoryThread.start();

            mThread = new Thread(mRunnable);
            mThread.start();

            isInventoryStarted = true;
        } else {
            isInventoryStarted = false;
            System.out.println("RfidInventoryStart faild.");
        }
        return isInventoryStarted;
    }

    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            // Refresh listview
            switch (msg.what) {
                case 1:
                    synchronized (context) {
                        mEPCList = mDevice.getTagIDs();
                       // Log.e("SCAN", "yes");

                        rfidInterface.onDataReceived(mEPCList);
                    }

                    refreshData();
                    break;
                default:
                    break;
            }
        }

        ;
    };

    private int RSSI = 0;
    private int Times = 0;
    List<EPC> epcList;
    List<Integer> rssiArrayList = new ArrayList<>();
    public boolean isCounitue = false;
    int sz = 0;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                while (isCounitue) {
                    try {
                        synchronized (context) {
                            epcList = mDevice.getTagIDs();
                            //s.add("sss");
                        }
                        //  for (int i = 0; i < epcList.size(); i++) {

                        //30361F4B3802
                        //if (mEPCList.get(mSelectedIndex).getId().equals(epcList.get(i).getId())) {
                        if (epcList.size() > 0) {
                            if (!mInventoryStart) {
                                return;
                            }

                            Thread.sleep(100);
                            int tagSize = epcList.size();
                            if(rssiArrayList!=null){
                                rssiArrayList.clear();
                            }

                            for (int i = 0; i < epcList.size(); i++) {
                                int rssivalue = epcList.get(i).rssi;
                                rssiArrayList.add(rssivalue);
                            }
                            int rssi = Collections.max(rssiArrayList);

                            int plusrssi = (-1)*rssi;
                            int actualpercentage = AssetUtils.getPercentage(plusrssi);
                            rfidInterface.handleLocateTagResponse(actualpercentage, tagSize);
                           /* rfidInterface.handleLocateTagResponse(0, 0);

                            if (rssi == RSSI) {
                                if (Times <= 10) {
                                    Times++;
                                } else {
                                    rssi = -101;
                                    rfidInterface.handleLocateTagResponse(0, 0);

                                }
                            } else {
                                RSSI = rssi;
                                Times = 0;
                            }

                            if (RSSI > -100 && RSSI <= -68) {
                                if (rssi == RSSI) {
                                    if (Times <= 10) {
                                        Times++;
                                    } else {
                                        rssi = -101;
                                        rfidInterface.handleLocateTagResponse(0, 0);

                                    }
                                } else {
                                    RSSI = rssi;
                                    Times = 0;
                                    rfidInterface.handleLocateTagResponse(5, tagSize);
                                }

                            }
                            *//*rssi between -68 and  -64*//*
                            if (RSSI > -68 && RSSI <= -64) {

                                if (rssi == RSSI) {
                                    if (Times <= 10) {
                                        Times++;
                                    } else {
                                        rssi = -101;
                                        rfidInterface.handleLocateTagResponse(0, 0);

                                    }
                                } else {
                                    RSSI = rssi;
                                    Times = 0;

                                    rfidInterface.handleLocateTagResponse(10, tagSize);
                                }
                            }
                            if (RSSI > -64 && RSSI <= -60) {
                                if (rssi == RSSI) {
                                    if (Times <= 10) {
                                        Times++;
                                    } else {
                                        rssi = -101;
                                        rfidInterface.handleLocateTagResponse(0, 0);
                                    }
                                } else {
                                    RSSI = rssi;
                                    Times = 0;

                                    rfidInterface.handleLocateTagResponse(20, tagSize);
                                }
                            }
                            if (RSSI > -60 && RSSI <= -56) {
                                if (rssi == RSSI) {
                                    if (Times <= 10) {
                                        Times++;
                                    } else {
                                        rssi = -101;
                                        rfidInterface.handleLocateTagResponse(0, 0);

                                    }
                                } else {
                                    RSSI = rssi;
                                    Times = 0;

                                    rfidInterface.handleLocateTagResponse(28, tagSize);
                                }
                            }
                            if (RSSI > -56 && RSSI <= -52) {
                                if (rssi == RSSI) {
                                    if (Times <= 10) {
                                        Times++;
                                    } else {
                                        rssi = -101;
                                        rfidInterface.handleLocateTagResponse(0, 0);

                                    }
                                } else {
                                    RSSI = rssi;
                                    Times = 0;

                                    rfidInterface.handleLocateTagResponse(35, tagSize);
                                }
                            }
                            if (RSSI > -52 && RSSI <= -46) {

                                rfidInterface.handleLocateTagResponse(45, tagSize);
                            }
                            if (RSSI > -46 && RSSI <= -42) {

                                rfidInterface.handleLocateTagResponse(55, tagSize);
                            }
                            if (RSSI > -42 && RSSI <= -38) {

                                rfidInterface.handleLocateTagResponse(65, tagSize);
                            }
                            if (RSSI > -38 && RSSI <= -34) {

                                rfidInterface.handleLocateTagResponse(75, tagSize);
                            }
                            if (RSSI > -34 && RSSI <= -30) {

                                rfidInterface.handleLocateTagResponse(80, tagSize);
                            }
                            if (RSSI > -30 && RSSI <= -25) {

                                rfidInterface.handleLocateTagResponse(90, tagSize);
                            }
                            if (RSSI > -25 && RSSI <= -20) {

                                rfidInterface.handleLocateTagResponse(100, tagSize);
                            }
                            if (RSSI > -20) {
                                rfidInterface.handleLocateTagResponse(100, tagSize);
                            }
*/
                        }

                      //  Log.e("TAGSIZE", "" + epcList.size());
                        if (epcList != null) {
                            epcList.clear();
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };


}

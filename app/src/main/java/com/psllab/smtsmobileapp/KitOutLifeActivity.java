package com.psllab.smtsmobileapp;

import static com.psllab.smtsmobileapp.helper.AssetUtils.hideProgressDialog;
import static com.psllab.smtsmobileapp.helper.AssetUtils.showProgress;

import androidx.databinding.DataBindingUtil;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psllab.smtsmobileapp.databases.DatabaseHandler;
import com.psllab.smtsmobileapp.databinding.ActivityKitOutLifeBinding;
import com.psllab.smtsmobileapp.helper.APIConstants;
import com.psllab.smtsmobileapp.helper.AssetUtils;
import com.psllab.smtsmobileapp.helper.BaseUtil;
import com.psllab.smtsmobileapp.helper.BeepClass;
import com.psllab.smtsmobileapp.helper.SharedPreferencesManager;
import com.psllab.smtsmobileapp.rfid.BaseUhfActivity;
import com.seuic.scankey.IKeyEventCallback;
import com.seuic.scankey.ScanKeyService;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class KitOutLifeActivity extends BaseUhfActivity {
    private List<EPC> mEPCList;
    public static final int MAX_LEN = 128;

    private Context context = this;
    private DatabaseHandler db;
    ActivityKitOutLifeBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_kit_out_life);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setContentView(R.layout.activity_in_out_module);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_kit_out_life);
        getSupportActionBar().hide();
        mEPCList = new ArrayList<EPC>();


        db = new DatabaseHandler(context);
        binding.btnSave.setVisibility(View.GONE);

        binding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.edtKitId.setText("");
                binding.edtOutLifeStatus.setText("");
                binding.edtKitOutLife.setText("");
            }
        });
    }

    private boolean dataAPIIsInProgress = false;
    private ScanKeyService mScanKeyService = ScanKeyService.getInstance();

    private IKeyEventCallback mCallback = new IKeyEventCallback.Stub() {
        @Override
        public void onKeyDown(int keyCode) throws RemoteException {
            if (is_uhf_success) {
                    String kitid = binding.edtKitId.getText().toString().trim();
                    String status = binding.edtOutLifeStatus.getText().toString().trim();
                    if(kitid.equalsIgnoreCase("") || status.equalsIgnoreCase("")){
                        if(!dataAPIIsInProgress){
                            BtnOnce();
                        }

                    }
                    if(!kitid.equalsIgnoreCase("") && !status.equalsIgnoreCase("")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //if(!dataAPIIsInProgress)
                                //binding.btnSave.performClick();
                            }
                        });
                    }
            }
        }

        @Override
        public void onKeyUp(int keyCode) throws RemoteException {
            //Log.e("TRIGGER", "onKeyUp: keyCode=" + keyCode);
        }
    };

    private void clearList() {
        if (mEPCList != null) {
            mEPCList.clear();
            m_count = 0;
        }
    }

    private void BtnOnce() {
        clearList();
        try {
            EPC epc = new EPC();
            if (mDevice.inventoryOnce(epc, 1000)) {
                String id = epc.getId();
                final byte[] epcarray = epc.id;
                if (id != null && !"".equals(id)) {
                    boolean exist = false;
                    for (EPC item : mEPCList) {
                        if (item.equals(epc)) {
                            item.count++;
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        mEPCList.add(epc);
                        String readepc = id;
                        // id = "3000"+id;
                        if (id.length() > 24) {
                            readepc = id.substring(4);
                        }
                        final String epcid = readepc;
                        Log.e("EPC", readepc);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BeepClass.successbeep(context);
                            }
                        });
                        mDevice.setParameters(UHFService.PARAMETER_ALGORITHM_RETRYCOUNT, 1);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // playSound();
                                        hideProgressDialog();
                                        showProgress(context, "Please Wait\nScanning Tag");
                                        getEpcMemoory(epcid, epcarray);//uncommented 02042021+
                                    }
                                }, 100);
                            }
                        });


                    }
                    refreshData();
                }
            } else {
                Log.e("EXCEPTION", "2");
                hideProgressDialog();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BeepClass.errorbeep(context);
                        AssetUtils.showCommonBottomSheetErrorDialog(context,"No RFID Tag Found");
                    }
                });
            }
        } catch (Exception e) {

            hideProgressDialog();
            Log.e("EXCEPTION", e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BeepClass.errorbeep(context);
                    AssetUtils.showCommonBottomSheetErrorDialog(context,"No RFID Tag Found");

                }
            });

            if (e.getMessage().equalsIgnoreCase("Attempt to read from field 'short com.uhf.api.cls.o.cG' on a null object reference")) {
                //Tag Not found within 1 second
            }
        }

    }

    private void refreshData() {
        if (mEPCList != null) {
            // Gets the number inside the list of all labels
            int count = 0;
            for (EPC item : mEPCList) {
                count += item.count;
            }
            m_count = count;
        }
    }

    static int m_count = 0;

    private void getEpcMemoory(final String uii, final byte[] epcarray) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                Log.e("EPC", uii);
                int count = 1;
                int bank = 1;
                int address = 4;//offset
                int length = 12;
                //int length = 30;
                String str_password = "00000000";
                byte[] btPassword = new byte[16];
                BaseUtil.getHexByteArray(str_password, btPassword, btPassword.length);
                byte[] buffer = new byte[MAX_LEN];
                if (length > MAX_LEN) {
                    buffer = new byte[length];
                }

                boolean tidread1 = false;
                try {
                    tidread1 = mDevice.readTagData(BaseUtil.getHexByteArray(uii), btPassword, bank, address, length, buffer);
                } catch (RuntimeException e) {
                    //Log.e("EXC", e.getMessage());
                }

                if (tidread1) {
                    String datas = BaseUtil.getHexString(buffer, length, " ");

                    datas = datas.replaceAll("\\s+", "");
                    final String data = datas;
                    //textView.setText("EPC:" + data);
                    // playSound();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // playSound();

                                    hideProgressDialog();
                                    BeepClass.successbeep(context);
                                    validateRfidAndDoActions(data);
                                }
                            }, 100);
                        }
                    });

                } else {
                    count++;
                    if (count == 1) {
                        boolean tidread2 = mDevice.readTagData(BaseUtil.getHexByteArray(uii), btPassword, bank, address, length, buffer);
                        if (tidread2) {
                            String datas = BaseUtil.getHexString(buffer, length, " ");
                            datas = datas.replaceAll("\\s+", "");
                            final String data = datas;
                            // playSound();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            // playSound();
                                            hideProgressDialog();
                                            BeepClass.successbeep(context);
                                            validateRfidAndDoActions(data);
                                        }
                                    }, 100);
                                }
                            });

                        } else {
                            count++;
                            if (count == 1) {

                            } else {
                                hideProgressDialog();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("EXCEPTION", "3");
                                        BeepClass.errorbeep(context);
                                        AssetUtils.showCommonBottomSheetErrorDialog(context,"No RFID Tag Found");
                                    }
                                });
                            }
                        }
                    } else {
                        hideProgressDialog();
                        Log.e("EXCEPTION", "4");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BeepClass.errorbeep(context);
                                AssetUtils.showCommonBottomSheetErrorDialog(context,"No RFID Tag Found");
                            }
                        });

                    }
                }

            }
        });


    }

    public void validateRfidAndDoActions(String tidData) {
        //hideProgressDialog();
        if (tidData != null) {
            if (tidData.length() > 20) {
                Log.e("TID", tidData);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //String data = ConvertHexStringToBinaryString(tidData);
                            String data = tidData;
                            Log.e("DATA", data);

                            if (data.length() == 24) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.btnClear.performClick();
                                        binding.edtKitId.setText(data);
                                    }
                                });
                                getTagStatus(data);
                                //TODO call here api and get data from server
                                //binding.edtId.setText(data);
                                // textrfid.setText(PSLUtils.ConvertBinaryStringToAsciiSeven(veh).trim().replace("\0",""));
                            } else {
                                BeepClass.errorbeep(context);
                                Toast.makeText(context, "Invalid RFID", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            BeepClass.errorbeep(context);
                            Log.e("EXC", e.getMessage());
                        }
                    }
                });
            }
        } else {
            //allow_to_press_trigger = true;
        }
    }

    private void getTagStatus(String rfid) {
        try {
            if (rfid.length() >= 24) {
                String companycode = rfid.substring(0, 2);
                String assettpid = rfid.substring(2, 4);
                String serialnumber = rfid.substring(4, 12);
                dataAPIIsInProgress = true;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(APIConstants.K_READER_ID, "10.0.0.2");
                jsonObject.put(APIConstants.K_ANTENA_ID, 1);
                jsonObject.put(APIConstants.K_RSSI, 1);
                jsonObject.put(APIConstants.K_TAG_ID, rfid);
                jsonObject.put(APIConstants.K_SERIAL_NUMBER, serialnumber);
                jsonObject.put(APIConstants.K_TAG_TYPE, assettpid);
               /* jsonObject.put(APIConstants.K_TRANS_LOCATION_ID, location_id);
                jsonObject.put(APIConstants.K_TOUCH_POINT_ID, 8);
                jsonObject.put(APIConstants.K_TOUCH_POINT_TYPE, "T");*/
                jsonObject.put(APIConstants.K_TTRANSACTION_DATE_TIME, AssetUtils.getSystemDateTimeInFormatt());

                getData(jsonObject, APIConstants.M_GET_KIT_OUT_LIFE_STATUS, "Please wait...\n" + "Getting Kit Out Life Status");

            } else {
                AssetUtils.showCommonBottomSheetErrorDialog(context, "Invalid RFID Scanned");
            }

        } catch (JSONException e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mScanKeyService.registerCallback(mCallback, "100,101,102,249,249,250");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScanKeyService.unregisterCallback(mCallback);
    }

    public void getData(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        Log.e("URL", SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        Log.e("GETSTATUSREQ",loginRequestObject.toString());
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        hideProgressDialog();
                        if (result != null) {
                            try {
                                Log.e("STATUSRES",result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                   JSONObject dataObj = result.getJSONObject(APIConstants.K_DATA);
                                   String kitoutstatus = dataObj.getString(APIConstants.K_KIT_OUT_STATUS);
                                   if(dataObj.has(APIConstants.K_KIT_OUT_LIFE)){
                                       String kitoutlife = dataObj.getString(APIConstants.K_KIT_OUT_LIFE);
                                       binding.edtKitOutLife.setText(kitoutlife);
                                   }else{
                                       binding.edtKitOutLife.setText("");
                                   }

                                   binding.edtOutLifeStatus.setText(kitoutstatus);

                                } else {
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, message);
                                }
                            } catch (JSONException e) {
                                hideProgressDialog();
                                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.something_went_wrong_error));
                            }
                        } else {
                            hideProgressDialog();
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        hideProgressDialog();
                        Log.e("ERROR", anError.getErrorDetail());
                        if (anError.getErrorDetail().equalsIgnoreCase("responseFromServerError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        } else if (anError.getErrorDetail().equalsIgnoreCase("connectionError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        } else {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        }
                    }
                });
    }

}
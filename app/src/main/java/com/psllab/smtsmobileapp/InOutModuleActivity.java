package com.psllab.smtsmobileapp;

import static com.psllab.smtsmobileapp.helper.AssetUtils.hideProgressDialog;
import static com.psllab.smtsmobileapp.helper.AssetUtils.showProgress;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psllab.smtsmobileapp.databases.DatabaseHandler;
import com.psllab.smtsmobileapp.databases.LocationMaster;
import com.psllab.smtsmobileapp.databinding.ActivityInOutModuleBinding;
import com.psllab.smtsmobileapp.helper.APIConstants;
import com.psllab.smtsmobileapp.helper.AppConstants;
import com.psllab.smtsmobileapp.helper.AssetUtils;
import com.psllab.smtsmobileapp.helper.BaseUtil;
import com.psllab.smtsmobileapp.helper.BeepClass;
import com.psllab.smtsmobileapp.helper.SharedPreferencesManager;
import com.psllab.smtsmobileapp.rfid.BaseUhfActivity;
import com.seuic.scankey.IKeyEventCallback;
import com.seuic.scankey.ScanKeyService;
import com.seuic.uhf.EPC;
import com.seuic.uhf.UHFService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class InOutModuleActivity extends BaseUhfActivity
        implements AdapterView.OnItemSelectedListener{

    private List<EPC> mEPCList;
    public static final int MAX_LEN = 128;

    private Context context = this;
    private DatabaseHandler db;
    List<String> sources;
    String selected_source_item = "Select Location";
    String default_location = "Select Location";
    ActivityInOutModuleBinding binding;
    private boolean isLocationLocked = false;
    private String CURRENT_RFID = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setContentView(R.layout.activity_in_out_module);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_in_out_module);
        getSupportActionBar().hide();
        mEPCList = new ArrayList<EPC>();

        db = new DatabaseHandler(context);
        getAllLocations();
        if(db.getLocationMasterCount()==0){
            //AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.location_master_sync_error));
        }
        binding.spLocation.setOnItemSelectedListener(this);

        //Creating the ArrayAdapter instance having the country list
        sources = db.getAllLocationsForSearchSpinner();
        sources.add(0,default_location);

        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, sources);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        binding.spLocation.setAdapter(aa);

        binding.imgSyncLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isLocationLocked){
                    AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.location_locked_sync_not_allowed));
                }else{
                    getAllLocations();
                }
            }
        });

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //String rfid = binding.edtId.getText().toString().trim();
                String rfid = CURRENT_RFID;
                if(db.getLocationMasterCount()>0){
                    if(selected_source_item.equalsIgnoreCase(default_location)){
                        AssetUtils.showCommonBottomSheetErrorDialog(context,"Please select location");
                    }else if(rfid.equalsIgnoreCase("")){
                        AssetUtils.showCommonBottomSheetErrorDialog(context,"Please scan RFID");
                    }else{
                        //UPLOAD DATA
                        Log.e("UPLOAD","YES");
                        Log.e("LOCATION",""+selected_source_item);
                        String location_id = db.getLocationIdByLocationName(selected_source_item);
                        if(!location_id.equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)) {
                            //TODO upload data
                            //uploadData(location_id,rfid);
                            try {
                                if(rfid.length()>=24){
                                    String companycode = rfid.substring(0,2);
                                    String assettpid = rfid.substring(2,4);
                                    String serialnumber = rfid.substring(4,12);
                                    dataAPIIsInProgress = true;
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put(APIConstants.K_READER_ID, "10.0.0.2");
                                    jsonObject.put(APIConstants.K_ANTENA_ID, 1);
                                    jsonObject.put(APIConstants.K_RSSI, 1);
                                    jsonObject.put(APIConstants.K_TAG_ID, rfid);
                                    jsonObject.put(APIConstants.K_SERIAL_NUMBER, serialnumber);
                                    jsonObject.put(APIConstants.K_TAG_TYPE, assettpid);
                                    jsonObject.put(APIConstants.K_TRANS_LOCATION_ID, location_id);
                                    jsonObject.put(APIConstants.K_TOUCH_POINT_ID, 0);
                                    jsonObject.put(APIConstants.K_TOUCH_POINT_TYPE, "M");
                                    jsonObject.put(APIConstants.K_TTRANSACTION_DATE_TIME, AssetUtils.getSystemDateTimeInFormatt());

                                    uploadData(jsonObject, APIConstants.M_UPLOAD_INOUT, "Please wait...\n" + "Transaction Upload is in progress");

                                }else{
                                    AssetUtils.showCommonBottomSheetErrorDialog(context,"Invalid RFID Scanned");
                                }

                       /* Intent loginIntent = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(loginIntent);*/
                            } catch (JSONException e) {

                            }
                        }else{
                            AssetUtils.showCommonBottomSheetErrorDialog(context,"Invalid Location");

                        }
                    }
                }else{
                    AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.no_location));

                }
            }
        });
        binding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.edtId.setText("");
                if(isLocationLocked){
                    AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.location_locked_clear_not_allowed));
                }else{
                    clearAll();
                }
            }
        });
        binding.imgLockLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                lockUnlock();
            }
        });

        binding.btnClear.performClick();
    }

    private void getAllLocations() {
        dataAPIIsInProgress = true;
        getAllLocations(APIConstants.M_GET_LOCATION_MASTER, "Please wait...\n" + "Getting Location List");
    }

    public void uploadData(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        Log.e("URL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        Log.e("UPLOADINOUTREQ",loginRequestObject.toString());
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        dataAPIIsInProgress = false;
                        hideProgressDialog();
                        if (result != null) {
                            try {
                                Log.e("LOGINRESULT",result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    AssetUtils.showCommonBottomSheetSuccessDialog(context,"Transaction Saved Successfully");
                                    clearAfterUpload();

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
                        dataAPIIsInProgress = false;
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


    public void getAllLocations(String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        Log.e("URL", SharedPreferencesManager.getHostUrl(context) + METHOD_NAME);
        AndroidNetworking.get(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME)//.addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        hideProgressDialog();
                        dataAPIIsInProgress = false;
                        if (result != null) {
                            try {
                                Log.e("LocationResult", result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    JSONArray locationMaster = null;
                                    if (result.has(APIConstants.K_DATA)) {
                                        locationMaster = result.getJSONArray(APIConstants.K_DATA);
                                        List<LocationMaster> locationMasterList = new ArrayList<>();

                                            if(locationMaster.length()>0){
                                                for(int vendor = 0; vendor<locationMaster.length();vendor++){
                                                    LocationMaster locationmaster = new LocationMaster();
                                                    JSONObject locationObject = locationMaster.getJSONObject(vendor);
                                                    if(locationObject.has(APIConstants.K_LOCATION_ID)){
                                                        locationmaster.setLocationId(locationObject.getString(APIConstants.K_LOCATION_ID));
                                                    }

                                                    if(locationObject.has(APIConstants.K_LOCATION_NAME)){
                                                        locationmaster.setLocationName(locationObject.getString(APIConstants.K_LOCATION_NAME));
                                                    }
                                                    locationMasterList.add(locationmaster);
                                                }
                                            }

                                        if(locationMasterList.size()>0){
                                            db.deleteLocationMaster();
                                            db.storeLocationMaster(locationMasterList);

                                            //Creating the ArrayAdapter instance having the country list
                                            if(sources!=null){
                                                sources.clear();
                                            }
                                            sources = db.getAllLocationsForSearchSpinner();
                                            sources.add(0,default_location);
                                            ArrayAdapter aa = new ArrayAdapter(context, android.R.layout.simple_spinner_item, sources);
                                            aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                            //Setting the ArrayAdapter data on the Spinner
                                            binding.spLocation.setAdapter(aa);
                                        }
                                    }

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
                        //getAllLocoMaster(APIConstants.M_GET_ALL_LOCO_MASTER,"Please wait...\nGetting Loco Master");

                    }

                    @Override
                    public void onError(ANError anError) {
                        dataAPIIsInProgress = false;
                    //    getAllLocoMaster(APIConstants.M_GET_ALL_LOCO_MASTER,"Please wait...\nGetting Loco Master");

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
    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        selected_source_item = sources.get(position);
        if(selected_source_item.equalsIgnoreCase(default_location)){

        }else{

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }
    private void clearAll(){
        selected_source_item = default_location;
        binding.edtId.setText("");
        CURRENT_RFID="";
        if(db.getLocationMasterCount()>0){
            binding.spLocation.setSelection(0);
        }

        binding.imgLockLocation.setImageDrawable(getResources().getDrawable(R.drawable.unlocked));
        isLocationLocked = false;
        binding.spLocation.setEnabled(true);
        binding.spLocation.setEnabled(true);
        binding.spLocation.setClickable(true);
        binding.imgLockLocation.setImageDrawable(getResources().getDrawable(R.drawable.unlocked));
    }
    private void clearAfterUpload(){
        CURRENT_RFID="";
        binding.edtId.setText("");
        if(isLocationLocked){

        }else{
            binding.spLocation.setSelection(0);

        }
    }

    private void lockUnlock(){
        if(selected_source_item.equalsIgnoreCase(default_location)){
            isLocationLocked = false;
            binding.spLocation.setEnabled(true);
            binding.spLocation.setEnabled(true);
            binding.spLocation.setClickable(true);
            binding.imgLockLocation.setImageDrawable(getResources().getDrawable(R.drawable.unlocked));
            AssetUtils.showCommonBottomSheetErrorDialog(context,"Please select location");
        }else{
            if(isLocationLocked){
                isLocationLocked = false;
                binding.spLocation.setEnabled(true);
                binding.spLocation.setEnabled(true);
                binding.spLocation.setClickable(true);
                binding.imgLockLocation.setImageDrawable(getResources().getDrawable(R.drawable.unlocked));
            }else{
                if(db.getAllLocationsForSearchSpinner().size()>0){
                    binding.spLocation.setEnabled(false);
                    binding.spLocation.setEnabled(false);
                    binding.spLocation.setClickable(false);
                    binding.imgLockLocation.setImageDrawable(getResources().getDrawable(R.drawable.locked));
                    isLocationLocked = true;
                }else{
                    isLocationLocked = false;
                    binding.spLocation.setEnabled(true);
                    binding.spLocation.setEnabled(true);
                    binding.spLocation.setClickable(true);
                    binding.imgLockLocation.setImageDrawable(getResources().getDrawable(R.drawable.unlocked));
                    AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.location_locked_clear_not_allowed));
                }

            }
        }

    }

    private ScanKeyService mScanKeyService = ScanKeyService.getInstance();

    private IKeyEventCallback mCallback = new IKeyEventCallback.Stub() {
        @Override
        public void onKeyDown(int keyCode) throws RemoteException {
            if (is_uhf_success) {
                Log.e("TRIGGER","1");
                Log.e("TRIGGER","LOCATION : "+selected_source_item);
                if(selected_source_item.equalsIgnoreCase(default_location)){
                    Log.e("TRIGGER","2");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.select_location));

                        }
                    });
                }else{
                    Log.e("TRIGGER","3");
                    String rfid = binding.edtId.getText().toString().trim();
                    if(rfid.equalsIgnoreCase("")){
                        Log.e("TRIGGER","4");
                        Log.e("SCAN","nbn");
                        if(!dataAPIIsInProgress){
                            Log.e("TRIGGER","5");
                            BtnOnce();
                        }else{
                            Log.e("TRIGGER","51");
                        }

                    }else{
                       //upload
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("TRIGGER","6");
                                if(!dataAPIIsInProgress) {
                                    Log.e("TRIGGER", "7");
                                    binding.btnSave.performClick();
                                }else{
                                    Log.e("TRIGGER","71");
                                }
                            }
                        });

                    }
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
            CURRENT_RFID = "";
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
                               binding.edtId.setText(data);
                                CURRENT_RFID= data;
                                String assetName = db.getAssetNameByTagId(data);
                                binding.edtId.setText(assetName);
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
    private boolean dataAPIIsInProgress = false;
}
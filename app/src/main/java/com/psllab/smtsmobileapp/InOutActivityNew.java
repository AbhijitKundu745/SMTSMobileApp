package com.psllab.smtsmobileapp;

import static com.psllab.smtsmobileapp.helper.AssetUtils.hideProgressDialog;
import static com.psllab.smtsmobileapp.helper.AssetUtils.showProgress;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.gson.JsonNull;
import com.psllab.smtsmobileapp.adapters.InOutAdapter;
import com.psllab.smtsmobileapp.adapters.InventoryAdapter;
import com.psllab.smtsmobileapp.databases.DatabaseHandler;
import com.psllab.smtsmobileapp.databases.InOutAssets;
import com.psllab.smtsmobileapp.databases.InventoryMaster;
import com.psllab.smtsmobileapp.databases.LocationMaster;
import com.psllab.smtsmobileapp.databinding.ActivityInOutNewBinding;
import com.psllab.smtsmobileapp.helper.APIConstants;
import com.psllab.smtsmobileapp.helper.AppConstants;
import com.psllab.smtsmobileapp.helper.AssetUtils;
import com.psllab.smtsmobileapp.helper.ConnectionDetector;
import com.psllab.smtsmobileapp.helper.SharedPreferencesManager;
import com.psllab.smtsmobileapp.rfid.BaseUhfActivity;
import com.psllab.smtsmobileapp.rfid.RFIDInterface;
import com.psllab.smtsmobileapp.rfid.SeuicGlobalRfidHandler;
import com.seuic.uhf.EPC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class InOutActivityNew extends BaseUhfActivity implements AdapterView.OnItemSelectedListener {
    private Context context = this;
    private ActivityInOutNewBinding binding;
    private ConnectionDetector cd;
    private SeuicGlobalRfidHandler rfidHandler;
    List<String> epcList;
    private boolean isInventoryOn = false;
    private boolean allow_trigger_to_press = true;
    Dialog customConfirmationDialog;
    private InOutAdapter InOutadapter;
    private Timer beepTimer;
    private int valid_speed = 0;
    private DatabaseHandler db;
    List<String> sources;
    String selected_source_item = "Select Location";
    String default_location = "Select Location";
    public ArrayList<HashMap<String, String>> tagList = new ArrayList<>();
    HashMap<String, String> hashMap = new HashMap<>();
    private int TagCount = 0;
    private String SCANNED_EPC = "";
    private Map<String, Map<String, String>> missedTags = new HashMap<>();
    private AlertDialog alertDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_in_out_new);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_in_out_new);
        setTitle("In Out Module");
        getSupportActionBar().hide();
        hideProgressDialog();
        cd = new ConnectionDetector(context);
        db = new DatabaseHandler(context);
        epcList = new ArrayList<>();
        enableSpinner();
        binding.spLocation.setEnabled(false);
        //binding.list.setVisibility(View.GONE);
        getAllLocations();
        binding.spLocation.setOnItemSelectedListener(this);
        sources = db.getAllLocationsForSearchSpinner();
        sources.add(0,default_location);


        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, sources);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        binding.spLocation.setAdapter(aa);
        InOutadapter = new InOutAdapter(context, tagList);
        binding.list.setAdapter(InOutadapter);
        InOutadapter.notifyDataSetChanged();

        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tagList.size() > 0) {
//                        showCustomConfirmationDialog("Are you sure you want to upload", "UPLOAD");
                    getMissedLocationTagData();
                }

            }
        });

        binding.btnPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allow_trigger_to_press) {
                    AssetUtils.openPowerSettingDialog(context, rfidHandler);
                    //openPowerSettingDialog(context);
                }
            }
        });
        binding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInventoryOn) {
                    if (epcList.size() > 0) {
                        showCustomConfirmationDialog("Are you sure you want to clear inventory data", "CLEAR");
                    }
                }
            }
        });
        binding.btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allow_trigger_to_press) {
                    if (selected_source_item.equalsIgnoreCase(default_location)) {
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Please select Location");
                    } else {
                        binding.list.setVisibility(View.VISIBLE);
                            takeInventoryAction();
                    }
                }
            }
        });

        //SharedPreferencesManager.setPower(context, 30);
        binding.textTotalScanned.setText("0");

        beepTimer = new Timer();
        beepTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //Called each time when 1000 milliseconds (1 second) (the period parameter)
                if (isInventoryOn) {
                    disableSpinner();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.textInventoryIndicator.startAnimation(AnimationUtils.loadAnimation(context, R.anim.blink_text));
                            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                            binding.btnStartStop.setText("Stop");
                            if (valid_speed > 0) {
                                rfidHandler.playSound();
                            }
                            valid_speed = 0;
                        }
                    });

                } else {
                    enableSpinner();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                            binding.btnStartStop.setText("Start");
                        }
                    });
                }
            }

        }, 0, 1000);

        showProgress(context, getResources().getString(R.string.uhf_initialization));
        rfidHandler = new SeuicGlobalRfidHandler();
        rfidHandler.onCreate(context, new RFIDInterface() {
            @Override
            public void handleTriggerPress(boolean pressed) {
                runOnUiThread(() -> {
                    if (pressed) {
                        binding.list.setVisibility(View.VISIBLE);
                            binding.btnStartStop.performClick();
                    }
                });
            }

            @Override
            public void RFIDInitializationStatus(boolean status) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    if (status) {

                    } else {

                    }
                });
            }

            @Override
            public void handleLocateTagResponse(int value, int tagSize) {
                runOnUiThread(() -> {

                });
            }

            @Override
            public void onDataReceived(List<EPC> rfifList) {
                runOnUiThread(() -> {
                    if (rfifList != null) {
                        if (rfifList.size() > 0) {
                            int maxRssi = Integer.MIN_VALUE;
                            String maxRssiEpc = null;
                            for (int i = 0; i < rfifList.size(); i++) {
                                String epc = rfifList.get(i).getId();
                                int rssivalue = rfifList.get(i).rssi;
                                if (rssivalue > maxRssi) {
                                    maxRssi = rssivalue;
                                    maxRssiEpc = epc;
                                }

                            }
                            if (maxRssiEpc != null) {
                                if (!maxRssiEpc.equalsIgnoreCase("")) {
                                    if (maxRssiEpc.length() >= 24) {
                                        maxRssiEpc = maxRssiEpc.substring(0, 24);
                                        //doDataValidations(maxRssiEpc);
                                        SCANNED_EPC = maxRssiEpc;//added
                                    }
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        selected_source_item = sources.get(position);
        if (selected_source_item.equalsIgnoreCase(default_location)) {

        } else {

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
     // TODO Auto-generated method stub
    }
    private void doDataValidations() {
        allow_trigger_to_press = true;
        if (!this.epcList.contains(SCANNED_EPC)) {
            if(!db.getAssetNameByTagId(SCANNED_EPC).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)){
                //valid_speed++;
                epcList.add(SCANNED_EPC);
                hashMap = new HashMap<>();
                hashMap.put(AppConstants.ASSET_NAME, db.getAssetNameByTagId(SCANNED_EPC));
                tagList.add(hashMap);
            }
            binding.textTotalScanned.setText(String.valueOf(epcList.size()));
            TagCount = epcList.size();
            InOutadapter.notifyDataSetChanged();
        }
        else{
            AssetUtils.showCommonBottomSheetErrorDialog(context,"The Kit has already been scanned");
        }
    }
//    private void doDataValidations(String epc) {
//        if (!this.epcList.contains(epc)) {
//            if(!db.getAssetNameByTagId(epc).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)){
//                valid_speed++;
//                epcList.add(epc);
//                hashMap = new HashMap<>();
//                hashMap.put(AppConstants.ASSET_NAME, db.getAssetNameByTagId(epc));
//                tagList.add(hashMap);
//            }
//
//            binding.textTotalScanned.setText(String.valueOf(epcList.size()));
//            TagCount = epcList.size();
//            InOutadapter.notifyDataSetChanged();
//        }
//        else{
//            AssetUtils.showCommonBottomSheetErrorDialog(context,"The Kit has already been scanned");
//        }
//    }


    private void takeInventoryAction() {
//        allow_trigger_to_press = true;
//        if (isInventoryOn) {
//            isInventoryOn = false;
//            rfidHandler.stopInventory();
//            //binding.ll.setBackgroundColor(getResources().getColor(R.color.red4));
//            binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
//            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
//            binding.btnStartStop.setText("Stop");
//        } else {
//            isInventoryOn = true;
//            rfidHandler.startInventory();
//            //binding.ll.setBackgroundColor(getResources().getColor(R.color.green));
//            binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
//            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
//            binding.btnStartStop.setText("Start");
//        }
        startInventory();
        new Handler().postDelayed(() -> {
            hideProgressDialog();
            allow_trigger_to_press = true;
            stopInventory();
            doDataValidations();//added
        }, 500);
    }


    public void enableSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (epcList.size() == 0) {
                    binding.spLocation.setEnabled(true);
                    binding.spLocation.setClickable(true);
                }
            }
        });
    }

    public void disableSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.spLocation.setEnabled(false);
                binding.spLocation.setClickable(false);
                // binding.spLocation.hideEdit();
            }
        });
    }


    public void onListItemClicked(HashMap<String, String> hashmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (allow_trigger_to_press) {
                    showCustomConfirmationDialog("Are you sure you want to delete", "DELETE");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        rfidHandler.onResume();
        SharedPreferencesManager.setPower(context, 5);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (beepTimer != null) {
            beepTimer.cancel();
        }
        if (epcList != null) {
            epcList.clear();
        }
        if (db != null) {
            db.close();
        }

        rfidHandler.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (beepTimer != null) {
            beepTimer.cancel();
        }
        rfidHandler.onPause();
    }

    @Override
    public void onBackPressed() {
        rfidHandler.stopInventory();
        if (epcList.size() > 0) {

            showCustomConfirmationDialog("Do you want to clear and go back?", "BACK");

        } else {
            super.onBackPressed();
        }
    }

    public void showCustomConfirmationDialog(String msg, final String action) {
        if (customConfirmationDialog != null) {
            customConfirmationDialog.dismiss();
        }
        customConfirmationDialog = new Dialog(context);
        if (customConfirmationDialog != null) {
            customConfirmationDialog.dismiss();
        }
        customConfirmationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customConfirmationDialog.setCancelable(false);
        customConfirmationDialog.setContentView(R.layout.custom_alert_dialog_layout2);
        TextView text = (TextView) customConfirmationDialog.findViewById(R.id.text_dialog);
        text.setText(msg);
        Button dialogButton = (Button) customConfirmationDialog.findViewById(R.id.btn_dialog);
        Button dialogButtonCancel = (Button) customConfirmationDialog.findViewById(R.id.btn_dialog_cancel);
        dialogButton.setText("YES");
        dialogButtonCancel.setText("NO");
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customConfirmationDialog.dismiss();
                if (action.equals("UPLOAD")) {
                    allow_trigger_to_press = false;
                    uploadInventoryToServer();
                } else if (action.equals("CLEAR")) {
                    clearInventory();
                } else if (action.equals("BACK")) {
                    clearInventory();
                    finish();

                }else if (action.equals("DELETE")) {
                    //CURRENT_INDEX = 1;
                    ((InOutActivityNew) context).onListItemDeleted();
                    //Toast.makeText(context, hashmap.get("MESSAGE"), Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customConfirmationDialog.dismiss();
            }
        });
        // customConfirmationDialog.getWindow().getAttributes().windowAnimations = R.style.SlideBottomUpAnimation;
        customConfirmationDialog.show();
    }
    public void onListItemDeleted() {
        if (InOutadapter.CURRENT_INDEX != -1) {
            tagList.remove(InOutadapter.CURRENT_INDEX);
            epcList.remove(InOutadapter.CURRENT_INDEX);
            InOutadapter.CURRENT_INDEX = -1;
            binding.textTotalScanned.setText(String.valueOf(epcList.size()));
            InOutadapter.notifyDataSetChanged();
        }
    }

    public void clearInventory() {
        allow_trigger_to_press = true;
        selected_source_item = default_location;
        if (epcList != null) {
            epcList.clear();
        }
        ArrayList<HashMap<String, String>> arrayList = this.tagList;
        if (arrayList != null) {
            arrayList.clear();
        }
        if(tagList!= null){
            tagList.clear();
        }
        InOutadapter.notifyDataSetChanged();
        enableSpinner();
        binding.spLocation.setEnabled(false);

        //binding.list.setVisibility(View.GONE);
        binding.textTotalScanned.setText("0");
        binding.spLocation.setSelection(0);
    }

    private void uploadInventoryToServer() {
            if(selected_source_item.equalsIgnoreCase(default_location)){
                AssetUtils.showCommonBottomSheetErrorDialog(context,"Please select location");
            }else if(epcList.size()==0){
                AssetUtils.showCommonBottomSheetErrorDialog(context,"Please scan RFID");
            }else{
                    new InOutActivityNew.CollectData().execute("ABC");
            }
    }
    public class CollectData extends AsyncTask<String, String, JSONObject> {
        protected void onPreExecute() {
            showProgress(context, "Collectiong Data To Upload");
            super.onPreExecute();
        }
        protected JSONObject doInBackground(String... params) {
            if (epcList.size()> 0) {
                try {

                    JSONObject jsonobject = null;
                    jsonobject = new JSONObject();
                    //dataAPIIsInProgress = true;
                    String location_id = db.getLocationIdByLocationName(selected_source_item);
                    jsonobject.put(APIConstants.K_READER_ID, "10.0.0.2");
                    jsonobject.put(APIConstants.K_INVENTORY_COUNT, "" + TagCount);
                    jsonobject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
                    jsonobject.put(APIConstants.K_TRANS_LOCATION_ID, location_id);
                    jsonobject.put(APIConstants.K_TOUCH_POINT_ID, 0);
                    jsonobject.put(APIConstants.K_TOUCH_POINT_TYPE, "M");
                    jsonobject.put(APIConstants.K_TTRANSACTION_DATE_TIME, AssetUtils.getSystemDateTimeInFormatt());

                    JSONArray js = new JSONArray();
                    for (int i = 0; i < epcList.size(); i++) {
                        String epc = epcList.get(i);
                        if (!db.getAssetNameByTagId(epc).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)) {
                            js.put(epc);
                        }
                    }
                    jsonobject.put(APIConstants.K_DATA, js);

                    return jsonobject;

                } catch (JSONException e) {
                    return null;
                }
            } else {
               // dataAPIIsInProgress = false;
                return null;
            }
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);

            if (result != null) {
                if (ConnectionDetector.isConnectingToInternet()) {
                    try {
                        hideProgressDialog();
                        allow_trigger_to_press = false;
                        uploadKitActivity(result, APIConstants.M_UPLOAD_INOUT_KIT_ACTIVITY, "Please wait...\n" + "Data upload is in progress");


                    } catch (OutOfMemoryError e) {
                        hideProgressDialog();
                        allow_trigger_to_press = false;
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Huge Data cannot be uploaded");
                    }

                } else {
                    hideProgressDialog();
                    allow_trigger_to_press = true;
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                }
            } else {
                hideProgressDialog();
                allow_trigger_to_press = true;
                AssetUtils.showCommonBottomSheetErrorDialog(context, "Something went wrong");
            }

        }
    }
    public void uploadKitActivity(final JSONObject dataRequestObject, String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(dataRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        hideProgressDialog();
                        allow_trigger_to_press = true;
                        //dataAPIIsInProgress = true;
                        if (result != null) {
                            try {
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    allow_trigger_to_press = false;
                                    //db.deleteKitActivity();
                                   clearInventory();
                                    AssetUtils.showCommonBottomSheetSuccessDialog(context, "Data Uploaded Successfully");
                                    InOutadapter.notifyDataSetChanged();
                                } else {
                                    allow_trigger_to_press = true;
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, message);
                                }
                            } catch (JSONException e) {
                                hideProgressDialog();
                                allow_trigger_to_press = true;
                                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.something_went_wrong_error));
                            }
                        } else {
                            hideProgressDialog();
                            allow_trigger_to_press = true;
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        hideProgressDialog();
                        allow_trigger_to_press = true;
                        //dataAPIIsInProgress = false;
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

    private void getAllLocations() {
        //dataAPIIsInProgress = true;
        getAllLocations(APIConstants.M_GET_LOCATION_MASTER, "Please wait...\n" + "Getting Location List");
    }

    public void getAllLocations(String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();
        AndroidNetworking.get(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME)//.addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        hideProgressDialog();
                        //dataAPIIsInProgress = false;
                        if (result != null) {
                            try {
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
                        //dataAPIIsInProgress = false;
                        //    getAllLocoMaster(APIConstants.M_GET_ALL_LOCO_MASTER,"Please wait...\nGetting Loco Master");

                        hideProgressDialog();
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
    //private boolean dataAPIIsInProgress = false;
    private void startInventory() {
        if (allow_trigger_to_press) {
            showProgress(context, "Please wait...Scanning Rfid Tag");
            allow_trigger_to_press = false;
            binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
            binding.btnStartStop.setText("Start");
            setFilterandStartInventory();
        } else {
            hideProgressDialog();
        }
    }

    private void stopInventory() {
        rfidHandler.stopInventory();
        allow_trigger_to_press = true;
        binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
        binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
        binding.btnStartStop.setText("Stop");
        hideProgressDialog();
    }

    private void setFilterandStartInventory() {
        int rfpower = SharedPreferencesManager.getPower(context);
        rfidHandler.setRFPower(rfpower);
        rfidHandler.startInventory();
    }
    private void getMissedLocationTagData(){
        if (cd.isConnectingToInternet()) {
            if (epcList.size()> 0) {
                try {
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put(APIConstants.K_READER_ID, "20.0.0.10");
                    jSONObject.put(APIConstants.K_INVENTORY_COUNT, ""+TagCount);
                    jSONObject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
                    jSONObject.put(APIConstants.K_TRANS_LOCATION_ID, db.getLocationIdByLocationName(selected_source_item));
                    jSONObject.put(APIConstants.K_TOUCH_POINT_ID, 0);
                    jSONObject.put(APIConstants.K_TOUCH_POINT_TYPE, "M");
                    jSONObject.put(APIConstants.K_TTRANSACTION_DATE_TIME, AssetUtils.getSystemDateTimeInFormatt());
                    JSONArray jSONArray = new JSONArray();
                    for(int i = 0; i < epcList.size(); i++)
                    {
                        String epc = epcList.get(i);if (!db.getAssetNameByTagId(epc).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)) {
                        jSONArray.put(epc);
                        }
                    }
                    jSONObject.put(APIConstants.K_DATA, jSONArray);
                    collectTagAndValidate(jSONObject, APIConstants.M_GET_LOCATION_ENTRY,"Please wait...\n" + "Getting Asset Status");
                } catch (Exception e){
                    AssetUtils.showCommonBottomSheetErrorDialog(context, e.getMessage());
                }
            }
        }
        else {
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
        }
    }
    private void collectTagAndValidate(JSONObject res, String methodName, String progressMsg){
        if(missedTags != null){
            missedTags.clear();
        }
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();
        showProgress(context, progressMsg);
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + methodName).addJSONObjectBody(res)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build().getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                           hideProgressDialog();
                           if(response.has(APIConstants.K_STATUS)){
                               if(response.getBoolean(APIConstants.K_STATUS)){
                                   if(response.has(APIConstants.K_DATA)){
                                       Object data = response.get(APIConstants.K_DATA);
                                       if (data != JSONObject.NULL) {
                                           if (data instanceof JSONArray) {
                                               JSONArray dataArray = response.getJSONArray(APIConstants.K_DATA);
                                               if(dataArray.length()>0) {
                                                   for (int j = 0; j < dataArray.length(); j++) {
                                                       String missedTag = "";
                                                       String assetName = "";
                                                       String prevLocation = "";
                                                       JSONObject obj = dataArray.getJSONObject(j);
                                                       if (obj.has(APIConstants.K_TAG_ID)) {
                                                           missedTag = obj.getString(APIConstants.K_TAG_ID);
                                                       }
                                                       if (obj.has("AssetteName")) {
                                                           assetName = obj.getString("AssetteName");
                                                       }
                                                       if (obj.has("CurrentPresentLocationID")) {
                                                           prevLocation = obj.getString("CurrentPresentLocationID");
                                                       }
                                                       Map<String, String> tagDetails = new HashMap<>();
                                                       tagDetails.put("AssetName", assetName);
                                                       tagDetails.put("PreviousLocation", prevLocation);
                                                       missedTags.put(missedTag, tagDetails);
                                                   }
                                                   if (!missedTags.isEmpty()) {
//                                                      missedTags.clear();
//                                                      missedTags.putAll(tempMissedTags);
                                                       showAlertDialog(missedTags);
                                                   }
                                               }
                                               else{
                                                   showCustomConfirmationDialog("Are you sure you want to upload?", "UPLOAD");
                                               }
                                           }
                                       }
                                        else{
                                            showCustomConfirmationDialog("Are you sure you want to upload?", "UPLOAD");
                                        }
                                   }
                               }
                               else {
                                   String message = response.getString(APIConstants.K_MESSAGE);
                                   AssetUtils.showCommonBottomSheetErrorDialog(context, message);
                               }
                           }
                        }
                        catch (Exception ex){
                            AssetUtils.showCommonBottomSheetErrorDialog(context, ex.getMessage());
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        hideProgressDialog();
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Error Code: "+anError.getErrorCode()+"\nError Message: "+ anError.getErrorDetail());
                    }
                });
    }
    public void showAlertDialog(Map<String, Map<String, String>> missedTags) {
        if (alertDialog != null && alertDialog.isShowing()) {
            return; // Prevent multiple dialogs from opening
        }

        SpannableStringBuilder messageBuilder  = new SpannableStringBuilder ();

        for (Map.Entry<String, Map<String, String>> entry : missedTags.entrySet()) {
            Map<String, String> tagDetails = entry.getValue();

            if (tagDetails != null) {
                String assetName = tagDetails.get("AssetName");
                String prevLocation = tagDetails.get("PreviousLocation");
                String prevLocationName = db.getLocationNameByLocationId(prevLocation);

                messageBuilder.append("The Asset ");
                int start = messageBuilder.length();
                messageBuilder.append(assetName);
                int end = messageBuilder.length();
                messageBuilder.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageBuilder.append(" has not been moved ");
                start = messageBuilder.length();
                messageBuilder.append("OUT");
                end = messageBuilder.length();
                messageBuilder.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageBuilder.append(" from location: ");
                start = messageBuilder.length();
                messageBuilder.append(prevLocationName);
                end = messageBuilder.length();
                messageBuilder.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                messageBuilder.append("\n\n");
            }

        }
        messageBuilder.append("If you want to save transactions, \nplease do OUT from this location of the assets.");

        TextView textView = new TextView(context);
        textView.setText(messageBuilder);
        textView.setPadding(40, 20, 40, 20);
        textView.setTextSize(16);

        alertDialog =  new AlertDialog.Builder(context)
                .setTitle("Warning")
                .setView(textView)
                .setIcon(R.drawable.warning)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

}

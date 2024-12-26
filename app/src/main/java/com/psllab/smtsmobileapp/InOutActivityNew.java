package com.psllab.smtsmobileapp;

import static com.psllab.smtsmobileapp.helper.AssetUtils.hideProgressDialog;
import static com.psllab.smtsmobileapp.helper.AssetUtils.showProgress;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
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
                        showCustomConfirmationDialog("Are you sure you want to upload", "UPLOAD");
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
                            for (int i = 0; i < rfifList.size(); i++) {
                                String epc = rfifList.get(i).getId();
                                if (epc != null) {
                                    if (!epc.equalsIgnoreCase("")) {
                                        if (epc.length() >= 24) {
                                            epc = epc.substring(0, 24);
                                            doDataValidations(epc);
                                            Log.e("EPC", epc);
                                        }
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
    private void doDataValidations(String epc) {
        if (!this.epcList.contains(epc)) {
            Log.e("Here1", db.getAssetNameByTagId(epc));
            if(!db.getAssetNameByTagId(epc).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)){
                valid_speed++;
                epcList.add(epc);
                hashMap = new HashMap<>();
                hashMap.put(AppConstants.ASSET_NAME, db.getAssetNameByTagId(epc));
                tagList.add(hashMap);
            }
            binding.textTotalScanned.setText(String.valueOf(epcList.size()));
            TagCount = epcList.size();
            InOutadapter.notifyDataSetChanged();
        }
    }

    private void takeInventoryAction() {
        allow_trigger_to_press = true;
        if (isInventoryOn) {
            isInventoryOn = false;
            rfidHandler.stopInventory();
            //binding.ll.setBackgroundColor(getResources().getColor(R.color.red4));
            binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
            binding.btnStartStop.setText("Stop");
        } else {
            isInventoryOn = true;
            rfidHandler.startInventory();
            //binding.ll.setBackgroundColor(getResources().getColor(R.color.green));
            binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
            binding.btnStartStop.setText("Start");
        }

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
        SharedPreferencesManager.setPower(context, 15);
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
                    Log.e("Connection", "No internet");
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

        Log.e("UPLOADURL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        Log.e("UPLOADREQUEST",dataRequestObject.toString());
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
                                Log.e("UPLOADRESULT",result.toString());
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
                        //Log.e("ERROR", anError.getErrorDetail());
                        if (anError.getErrorDetail().equalsIgnoreCase("responseFromServerError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        } else if (anError.getErrorDetail().equalsIgnoreCase("connectionError")) {
                            Log.e("Connection1", "No internet");
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        } else {
                            Log.e("Connection2", "No internet");
                            Log.e("Error", anError.getErrorDetail());
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
                        //dataAPIIsInProgress = false;
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
                        //dataAPIIsInProgress = false;
                        //    getAllLocoMaster(APIConstants.M_GET_ALL_LOCO_MASTER,"Please wait...\nGetting Loco Master");

                        hideProgressDialog();
                        Log.e("ERROR", anError.getErrorDetail());
                        if (anError.getErrorDetail().equalsIgnoreCase("responseFromServerError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        } else if (anError.getErrorDetail().equalsIgnoreCase("connectionError")) {
                            Log.e("Connection3", "No internet");
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        } else {
                            Log.e("Connection4", "No internet");
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        }
                    }
                });
    }
    //private boolean dataAPIIsInProgress = false;
}

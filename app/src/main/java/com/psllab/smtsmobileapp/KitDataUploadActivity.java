package com.psllab.smtsmobileapp;

import static com.psllab.smtsmobileapp.helper.AssetUtils.hideProgressDialog;
import static com.psllab.smtsmobileapp.helper.AssetUtils.showProgress;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psllab.smtsmobileapp.adapters.InOutAdapter;
import com.psllab.smtsmobileapp.adapters.InventoryAdapter;
import com.psllab.smtsmobileapp.databases.DatabaseHandler;
import com.psllab.smtsmobileapp.databases.InventoryMaster;
import com.psllab.smtsmobileapp.databinding.ActivityKitDataUploadBinding;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class KitDataUploadActivity extends BaseUhfActivity implements AdapterView.OnItemSelectedListener {
    List<String> epcList;

    Dialog customConfirmationDialog, customConfirmationDialog1;
    private ActivityKitDataUploadBinding binding;
    private Context context = this;
    private ConnectionDetector cd;
    private SeuicGlobalRfidHandler rfidHandler;
    private boolean isInventoryOn = false;
    private boolean allow_trigger_to_press = true;
    private Timer beepTimer;
    private int valid_speed = 0;
    private InventoryAdapter adapter;
    private DatabaseHandler db;
    List<String> sources;
    String selected_source_item = "Select Autoclave Location";
    String default_location = "Select Autoclave Location";
    private boolean isStatusRetrieved = false;
    public ArrayList<HashMap<String, String>> tagList = new ArrayList<>();
    HashMap<String, String> hashMap = new HashMap<>();
    private int TagCount = 0;
    //List<String> missedTags = new ArrayList<>();
    Map<String, String> missedTags = new HashMap<>();
    private String SCANNED_EPC = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_kit_data_upload);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_kit_data_upload);
        setTitle("Kit Data Upload");
        getSupportActionBar().hide();
        //binding.btnSave.setText("Get Status");
        hideProgressDialog();
        cd = new ConnectionDetector(context);
        db = new DatabaseHandler(context);
        isStatusRetrieved = false;

        epcList = new ArrayList<>();
        enableSpinner();
        binding.spLocation.setEnabled(false);

        //binding.list.setVisibility(View.GONE);
        binding.spLocation.setOnItemSelectedListener(this);
        //Creating the ArrayAdapter instance having the country list
        sources = db.getAllAutoclaveLocationsForSearchSpinner();
        sources.add(0, default_location);
        //sources.add(1,"ABC");

        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, sources);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spLocation.setAdapter(aa);
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tagList.size() > 0) {
                    //showCustomConfirmationDialog("Are you sure you want to upload", "UPLOAD");
                    getTagStatus();
                }
//                int invMaster = db.getInventoryMasterCount();
//                if (invMaster > 0) {
//                    if (isStatusRetrieved) {
//                        showCustomConfirmationDialog("Are you sure you want to upload", "UPLOAD");
//                    }
//                    else {
//                        showCustomConfirmationDialog("Are you sure you want to get status", "GETSTATUS");
//                    }
//                }

            }
        });

        binding.btnPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allow_trigger_to_press) {
                    AssetUtils.openPowerSettingDialog(context, rfidHandler);
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
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Please select Autoclave Location");
                    } else {
                        if (!isStatusRetrieved) {
                            binding.list.setVisibility(View.VISIBLE);
                            takeInventoryAction();
                        }
                    }
                }
            }
        });



        adapter = new InventoryAdapter(context, tagList);
        binding.list.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        binding.textTotalScanned.setText("Total Kit ID's : 0");
//        binding.textProcessed.setText("Processed : 0");
//        binding.textUnprocessed.setText("Unprocessed : 0");

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
                        if (!isStatusRetrieved) {
                            binding.list.setVisibility(View.VISIBLE);
                            binding.btnStartStop.performClick();
                        }
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
                                if (maxRssiEpc != null) {
                                    if (!maxRssiEpc.equalsIgnoreCase("")) {
                                        if (maxRssiEpc.length() >= 24) {
                                            maxRssiEpc = maxRssiEpc.substring(0, 24);
                                            SCANNED_EPC = maxRssiEpc;//added
                                            //doDataValidations(maxRssiEpc);
                                            Log.e("EPC", maxRssiEpc);
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
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        selected_source_item = sources.get(position);
        if (selected_source_item.equalsIgnoreCase(default_location)) {

        } else {

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
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
//            adapter.notifyDataSetChanged();
//        }
//        else{
//            AssetUtils.showCommonBottomSheetErrorDialog(context,"The Kit has already been scanned");
//        }
//    }
private void doDataValidations() {
    allow_trigger_to_press = true;
    if (!this.epcList.contains(SCANNED_EPC)) {
        Log.e("Here1", db.getAssetNameByTagId(SCANNED_EPC));
        if(!db.getAssetNameByTagId(SCANNED_EPC).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)){
            //valid_speed++;
            epcList.add(SCANNED_EPC);
            hashMap = new HashMap<>();
            hashMap.put(AppConstants.ASSET_NAME, db.getAssetNameByTagId(SCANNED_EPC));
            tagList.add(hashMap);
        }
        binding.textTotalScanned.setText(String.valueOf(epcList.size()));
        TagCount = epcList.size();
        adapter.notifyDataSetChanged();
    }
    else{
        AssetUtils.showCommonBottomSheetErrorDialog(context,"The Kit has already been scanned");
    }
}

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

            showCustomConfirmationDialog("Are you sure you want to cancel inventory process", "CANCEL");

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
                } else if (action.equals("CANCEL")) {
                    cancelInventory();
//                } else if (action.equals("GETSTATUS")) {
//                    allow_trigger_to_press = false;
//                    //uploadInventoryToServerForStatus();
//
                }
                 else if (action.equals("CLEAR")) {
                    clearInventory();
                }
                else if (action.equals("DELETE")) {
                    ((KitDataUploadActivity) context).onListItemDeleted();
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
        if (adapter.KIT_INDEX != -1) {
            tagList.remove(adapter.KIT_INDEX);
            epcList.remove(adapter.KIT_INDEX);
            adapter.KIT_INDEX = -1;
            binding.textTotalScanned.setText(String.valueOf(epcList.size()));
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Cancel action and go back
     */
    public void cancelInventory() {
        if (epcList != null) {
            epcList.clear();
        }
        ArrayList<HashMap<String, String>> arrayList = tagList;
        if (arrayList != null) {
            arrayList.clear();
        }
        if(missedTags != null){
            missedTags.clear();
        }

        binding.list.setVisibility(View.GONE);
        allow_trigger_to_press = true;
        // binding.spLocation.setSelectedItem(0);
        finish();
    }

    /**
     * Cancel action and go back
     */
    public void clearInventory() {
//        binding.textProcessed.setText("Processed : 0");
//        binding.textUnprocessed.setText("Unprocessed : 0");

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
        adapter.notifyDataSetChanged();
        //binding.btnSave.setText("Get Status");
        isStatusRetrieved = false;
        enableSpinner();
        binding.spLocation.setEnabled(false);

        //binding.list.setVisibility(View.GONE);
        binding.textTotalScanned.setText("Total Kit ID's : 0");
        binding.spLocation.setSelection(0);
        if(missedTags != null){
            missedTags.clear();
        }
    }

    /**
     * collect inventory data and upload to server
     */
    private void uploadInventoryToServer() {
        //int inventoryCount = db.getInventoryMasterCount();
        //if (inventoryCount > 0) {
        if (epcList.size()> 0) {
            new CollectInventoryData().execute("ABC");
        } else {
            AssetUtils.showCommonBottomSheetErrorDialog(context, "No data to upload");
        }
    }

//    private void uploadInventoryToServerForStatus() {
//        int inventoryCount = db.getInventoryMasterCount();
//        if (inventoryCount > 0) {
//            new CollectInventoryDataForStatus().execute("ABC");
//        } else {
//            AssetUtils.showCommonBottomSheetErrorDialog(context, "No data to upload");
//        }
//
//    }

    public void uploadInventory(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        //Log.e("UPLOADURL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        Log.e("UPLOADREQUEST",loginRequestObject.toString());
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
                                //Log.e("UPLOADRESULT",result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    if (epcList != null) {
                                        epcList.clear();
                                    }
                                    //binding.btnSave.setText("Get Status");

                                    //db.deleteInventoryMaster();
                                    binding.textTotalScanned.setText("Total Kit ID's : " + epcList.size());
                                    allow_trigger_to_press = false;
                                   clearInventory();
//                                    binding.textProcessed.setText("Processed : 0");
//                                    binding.textUnprocessed.setText("Unprocessed : 0");

                                    // SELECTED_LOCATION_ITEM = "";
                                    // SELECTED_LOCATION_ID = "";
                                    //binding.textTouchPoint.setText(getResources().getString(R.string.select_track_point));
                                    //binding.spLocation.setSelectedItem(0);
                                    AssetUtils.showCommonBottomSheetSuccessDialog(context, "Data Uploaded Successfully");


                                    // binding.textTotalScanned.setText("" + epcList.size());
                                    adapter.notifyDataSetChanged();
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
                        //Log.e("ERROR", anError.getErrorDetail());
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

//    public void getInventoryStatus(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message) {
//        showProgress(context, progress_message);
//        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
//                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
//                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
//                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
//                .build();
//
//        //Log.e("UPLOADURL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
//        //Log.e("UPLOADREQUEST",loginRequestObject.toString());
//        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(loginRequestObject)
//                .setTag("test")
//                .setPriority(Priority.LOW)
//                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
//                .build()
//                .getAsJSONObject(new JSONObjectRequestListener() {
//                    @Override
//                    public void onResponse(JSONObject result) {
//                        hideProgressDialog();
//                        if (result != null) {
//                            try {
//                                Log.e("GETSTATUSRESULT", result.toString());
//                                String status = result.getString(APIConstants.K_STATUS);
//                                String message = result.getString(APIConstants.K_MESSAGE);
//
//                                if (status.equalsIgnoreCase("true")) {
//                                    allow_trigger_to_press = false;
//
    //                                    JSONObject masterObj = result.getJSONObject("data");
//
//                                    JSONArray dataArray = masterObj.getJSONArray("Data");
//                                    if (dataArray.length() == epcList.size()) {
//                                        List<InventoryMaster> inventoryMasterList = new ArrayList<>();
//
//                                        for (int i = 0; i < dataArray.length(); i++) {
//                                            JSONObject obj = dataArray.getJSONObject(i);
//                                            InventoryMaster inventoryMaster = new InventoryMaster();
//                                            inventoryMaster.setAssetEpc(obj.getString("TagId"));
//                                            if (obj.getString("StatusReturn").equalsIgnoreCase("processed")) {
//                                                inventoryMaster.setAssetStatus("P");
//                                            } else if (obj.getString("StatusReturn").equalsIgnoreCase("Unprocessed")) {
//                                                inventoryMaster.setAssetStatus("U");
//                                            } else {
//                                                inventoryMaster.setAssetStatus("NONE");
//                                            }
//
//                                            inventoryMaster.setAssetId(obj.getString("AssetteId"));
//                                            inventoryMaster.setAssetName(obj.getString("AssetteName"));
//                                            inventoryMasterList.add(inventoryMaster);
//                                        }
//                                        isStatusRetrieved = true;
//                                        binding.btnSave.setText("Save");
//                                        db.storeInventoryMaster(inventoryMasterList);
//                                        List<InventoryMaster> dbInvmasterlist = db.getAllInventoryMaster();
//                                        binding.list.setVisibility(View.VISIBLE);
//                                        adapter = new InventoryAdapter(context, dbInvmasterlist);
//                                        binding.list.setAdapter(adapter);
//
//                                        binding.textProcessed.setText("Processed : " + db.getProcessedInventoryMasterCount());
//                                        binding.textUnprocessed.setText("Unprocessed : " + db.getUnprocessedInventoryMasterCount());
//
//                                    } else {
//                                        allow_trigger_to_press = true;
//                                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Data mismatch can not proceed");
//                                    }
//
//                                } else {
//                                    allow_trigger_to_press = true;
//                                    AssetUtils.showCommonBottomSheetErrorDialog(context, message);
//                                }
//                            } catch (JSONException e) {
//                                hideProgressDialog();
//                                isStatusRetrieved = false;
//                                allow_trigger_to_press = true;
//                                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.something_went_wrong_error));
//                            }
//                        } else {
//                            hideProgressDialog();
//                            allow_trigger_to_press = true;
//                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
//                        }
//                    }
//
//                    @Override
//                    public void onError(ANError anError) {
//                        hideProgressDialog();
//                        allow_trigger_to_press = true;
//                        isStatusRetrieved = false;
//                        //Log.e("ERROR", anError.getErrorDetail());
//                        if (anError.getErrorDetail().equalsIgnoreCase("responseFromServerError")) {
//                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
//                        } else if (anError.getErrorDetail().equalsIgnoreCase("connectionError")) {
//                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
//                        } else {
//                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
//                        }
//                    }
//                });
//    }

    public class CollectInventoryData extends AsyncTask<String, String, JSONObject> {
        protected void onPreExecute() {
            showProgress(context, "Collectiong Data To Upload");
            super.onPreExecute();
        }

        protected JSONObject doInBackground(String... params) {
            //int inventoryCount = db.getInventoryMasterCount();
//            if (inventoryCount > 0) {
            if (epcList.size()> 0) {
                try {
                    JSONObject jSONObject = null;
                    jSONObject = new JSONObject();
                    jSONObject.put(APIConstants.K_INVENTORY_COUNT, "" + TagCount);
                    jSONObject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
                    jSONObject.put(APIConstants.K_TRANS_LOCATION_ID, db.getAutoclaveLocationIdByLocationName(selected_source_item));
                    jSONObject.put(APIConstants.K_INVENTORY_DATE_TIME, AssetUtils.getUTCSystemDateTimeInFormatt());
                    JSONArray jSONArray = new JSONArray();
                    for (int i = 0; i < epcList.size(); i++) {
                        String epc = epcList.get(i);
                        if (!db.getAssetNameByTagId(epc).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)) {
                            jSONArray.put(epc);
                        }
                    }
                    jSONObject.put(APIConstants.K_DATA, jSONArray);

                    return jSONObject;

                } catch (JSONException e) {

                    return null;
                }
            } else {
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
                        //Log.e("Result",result.toString());

                        allow_trigger_to_press = false;
                        uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + "Data upload is in progress");


                    } catch (OutOfMemoryError e) {
                        hideProgressDialog();
                        allow_trigger_to_press = true;
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

//    public class CollectInventoryDataForStatus extends AsyncTask<String, String, JSONObject> {
//        protected void onPreExecute() {
//            showProgress(context, "Collectiong Data To Upload");
//            super.onPreExecute();
//        }
//
//        protected JSONObject doInBackground(String... params) {
//            int inventoryCount = db.getInventoryMasterCount();
//            if (inventoryCount > 0) {
//                try {
//
//                    JSONObject jsonobject = null;
//                    jsonobject = new JSONObject();
//
//                    jsonobject.put(APIConstants.K_INVENTORY_COUNT, "" + inventoryCount);
//                    jsonobject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
//                    jsonobject.put(APIConstants.K_TRANS_LOCATION_ID, db.getAutoclaveLocationIdByLocationName(selected_source_item));
//                    jsonobject.put(APIConstants.K_INVENTORY_DATE_TIME, AssetUtils.getUTCSystemDateTimeInFormatt());
//
//                    JSONArray js = new JSONArray();
//                    List<InventoryMaster> inventoryMasterList = db.getAllInventoryMaster();
//                    for (int i = 0; i < inventoryMasterList.size(); i++) {
//                        JSONObject dataObject = new JSONObject();
//                        dataObject.put("TagId", inventoryMasterList.get(i).getAssetEpc());
//                        //dataObject.put("status",inventoryMasterList.get(i).getAssetStatus());
//                        //dataObject.put("assetId",inventoryMasterList.get(i).getAssetId());
//                        //dataObject.put("assetName",inventoryMasterList.get(i).getAssetName());
//                        //String epc = epcList.get(i);
//                        //js.put(epc);
//                        js.put(dataObject);
//                    }
//                    jsonobject.put("Data", js);
//
//                    return jsonobject;
//
//                } catch (JSONException e) {
//
//                    return null;
//                }
//            } else {
//                return null;
//            }
//        }
//
//        @Override
//        protected void onPostExecute(JSONObject result) {
//            super.onPostExecute(result);
//
//            if (result != null) {
//                if (cd.isConnectingToInternet()) {
//                    try {
//                        hideProgressDialog();
//                        //Log.e("Result",result.toString());
//                        isStatusRetrieved = false;
//                        allow_trigger_to_press = false;
//
//                        getInventoryStatus(result, APIConstants.M_INVENTORY_STATUS, "Please wait...\n" + "Inventory  status checking is in progress");
//
//
//                    } catch (OutOfMemoryError e) {
//                        hideProgressDialog();
//                        allow_trigger_to_press = true;
//                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Huge Data cannot be uploaded");
//                    }
//
//                } else {
//                    hideProgressDialog();
//                    allow_trigger_to_press = true;
//                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
//                }
//            } else {
//                hideProgressDialog();
//                allow_trigger_to_press = true;
//                AssetUtils.showCommonBottomSheetErrorDialog(context, "Something went wrong");
//            }
//
//        }
//
//    }
public void showCustomConfirmationDialogSpecial(Map<String, String> missedTags) {
    if (customConfirmationDialog1 != null) {
        customConfirmationDialog1.dismiss();
    }
    customConfirmationDialog1 = new Dialog(context);
    if (customConfirmationDialog1 != null) {
        customConfirmationDialog1.dismiss();
    }
    customConfirmationDialog1.requestWindowFeature(Window.FEATURE_NO_TITLE);
    customConfirmationDialog1.setCancelable(false);
    customConfirmationDialog1.setContentView(R.layout.custom_alert_dialog_layout2);
    TextView text = (TextView) customConfirmationDialog1.findViewById(R.id.text_dialog);
    TextView textHeader = (TextView) customConfirmationDialog1.findViewById(R.id.textView);
    ImageButton errorImage = (ImageButton) customConfirmationDialog1.findViewById(R.id.image_dialog);
    errorImage.setVisibility(View.VISIBLE);
    textHeader.setVisibility(View.VISIBLE);
    errorImage.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            customConfirmationDialog1.dismiss();
        }
    });
    // Join the missedTags list into a comma-separated string
    String missedTagsString = TextUtils.join(", ", missedTags.values());
    // Create a custom message
    String message = "The entries of the following kits were missed: \n\n" + missedTagsString +"\n\n Do you want to proceed?";
    text.setText(message);
    Button dialogButton = (Button) customConfirmationDialog1.findViewById(R.id.btn_dialog);
    Button dialogButtonCancel = (Button) customConfirmationDialog1.findViewById(R.id.btn_dialog_cancel);
    dialogButton.setText("YES");
    dialogButtonCancel.setText("NO");
    dialogButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                allow_trigger_to_press = false;
                uploadInventoryToServer();
            customConfirmationDialog1.dismiss();
        }
    });
    dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(!missedTags.isEmpty()){
                // Remove missed tags from epcList
                for (String missedTag : missedTags.keySet()) {
                    epcList.remove(missedTag); // Removes the tag if it exists
                }
            }
            adapter.notifyDataSetChanged();
            allow_trigger_to_press = false;
            uploadInventoryToServer();
            customConfirmationDialog1.dismiss();
        }
    });
    customConfirmationDialog1.show();
}
    private void getTagStatus() {
        if (cd.isConnectingToInternet()) {
            if (epcList.size()> 0) {
                try {
                    JSONObject jSONObject = null;
                    jSONObject = new JSONObject();
                    jSONObject.put(APIConstants.K_INVENTORY_COUNT, "" + TagCount);
                    jSONObject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
                    jSONObject.put(APIConstants.K_TRANS_LOCATION_ID, db.getAutoclaveLocationIdByLocationName(selected_source_item));
                    jSONObject.put(APIConstants.K_INVENTORY_DATE_TIME, AssetUtils.getUTCSystemDateTimeInFormatt());
                    JSONArray jSONArray = new JSONArray();
                    for (int i = 0; i < epcList.size(); i++) {
                        String epc = epcList.get(i);
                        if (!db.getAssetNameByTagId(epc).equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)) {
                            jSONArray.put(epc);
                        }
                    }
                    jSONObject.put(APIConstants.K_DATA, jSONArray);
                    collectTagStatusAndDoValidation(jSONObject, APIConstants.M_GET_MISSED_ASSETS,"Please wait...\n" + "Getting Asset Status");

                } catch (JSONException e) {

                }
            }
        } else {
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
        }
    }

    public void collectTagStatusAndDoValidation(JSONObject request, String METHOD_NAME, String progress_message) {
        if(missedTags != null){
            missedTags.clear();
        }
        Log.e("MissedTagReq", request.toString());
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();
        showProgress(context, progress_message);
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(request)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build().getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("MissedTagResponse", response.toString());
                        if (response != null) {
                            try {
                                hideProgressDialog();
                                if (response.has(APIConstants.K_STATUS)) {
                                    if (response.getBoolean(APIConstants.K_STATUS)) {
                                        if (response.has(APIConstants.K_DATA)) {
                                            if (response.getJSONArray(APIConstants.K_DATA).length()>0) {
                                                JSONArray dataArray;
                                                dataArray = response.getJSONArray(APIConstants.K_DATA);
                                                for(int i=0; i< dataArray.length(); i++){
                                                    String missedTag = "";
                                                    String AssetName = "";
                                                    JSONObject obj = dataArray.getJSONObject(i);
                                                    if(obj.has("TagID")){
                                                        missedTag = obj.getString("TagID");
                                                    }
                                                    if(obj.has("AssetteName")){
                                                        AssetName = obj.getString("AssetteName");
                                                    }
                                                    missedTags.put(missedTag, AssetName);

                                                }
                                                if(!missedTags.isEmpty()){
                                                    showCustomConfirmationDialogSpecial(missedTags);
                                                }
                                            } else{
                                                showCustomConfirmationDialog("Are you sure you want to upload?", "UPLOAD");
                                            }
                                        }

                                    } else {
                                        String message = response.getString(APIConstants.K_MESSAGE);
                                        AssetUtils.showCommonBottomSheetErrorDialog(context, message);
                                    }
                                }
                            } catch (JSONException e) {
                            }
                        } else {
                            hideProgressDialog();
                            // Toast.makeText(context,"Communication Error",Toast.LENGTH_SHORT).show();
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        }
                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
        //TODO CALL Without Barcode API
    }
    private void startInventory() {
        if (allow_trigger_to_press) {
            allow_trigger_to_press = false;
            binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
            binding.btnStartStop.setText("Start");
            showProgress(context, "Please wait...Scanning Rfid Tag");
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

}
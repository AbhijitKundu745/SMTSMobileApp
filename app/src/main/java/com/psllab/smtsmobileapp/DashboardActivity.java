package com.psllab.smtsmobileapp;

import static com.psllab.smtsmobileapp.helper.AssetUtils.showProgress;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psllab.smtsmobileapp.databases.AssetMaster;
import com.psllab.smtsmobileapp.databases.DatabaseHandler;
import com.psllab.smtsmobileapp.databinding.ActivityDashboardBinding;
import com.psllab.smtsmobileapp.helper.APIConstants;
import com.psllab.smtsmobileapp.helper.AppConstants;
import com.psllab.smtsmobileapp.helper.AssetUtils;
import com.psllab.smtsmobileapp.helper.ConnectionDetector;
import com.psllab.smtsmobileapp.helper.SharedPreferencesManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DashboardActivity extends AppCompatActivity {
    ActivityDashboardBinding binding;
    private ConnectionDetector cd;
    public Context context = this;
    public DatabaseHandler db;
    private Handler workOrderPollingApiHandler = new Handler();
    private Runnable workOrderPollingApiRunnable;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard);
        setTitle("Dashboard");
        getSupportActionBar().hide();
        SharedPreferencesManager.setPowerText(context, "LOW");
        //binding.btnKitOut.setVisibility(View.GONE);
        db = new DatabaseHandler(context);
        cd = new ConnectionDetector(context);

        binding.btnInOut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showProgress(context, "Processing..");
                Intent i = new Intent(DashboardActivity.this,InOutActivityNew.class);
                startActivity(i);
            }
        });
//        this.binding.btnKitOut.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View view) {
//            }
//        });
        binding.btnKitDataUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (DashboardActivity.this.db.getAutoclaveLocationMasterCount() > 0) {
                    Log.e("ASSETCOUNT", "" + db.getAssetMasterCount());
                    showProgress(context, "Processing..");
                    Intent i = new Intent(DashboardActivity.this,KitDataUploadActivity.class);
                    startActivity(i);
                }
                else {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, "Autoclave locations not found, cannot proceed");
                }
            }

        });
        binding.sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startWorkOrderPollingApiHandler();
                getAssets(APIConstants.M_GET_ASSETS, "GETTING ASSETS");

            }
        });
        //startWorkOrderPollingApiHandler();
    }
//    private void startWorkOrderPollingApiHandler() {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        CompletableFuture<Void> future = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            future = CompletableFuture.runAsync(() -> {
//                getAssets(APIConstants.M_GET_ASSETS, "GETTING ASSETS");
//            }, executor);
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            future.thenRun(() -> {
//                // Task completed
//                // Handle any post-processing or UI updates here
//            });
//        }
//
//        // Shutdown executor when the application is closing
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> executor.shutdown()));
//    }


    private void startWorkOrderPollingApiHandler() {
        if (workOrderPollingApiRunnable != null) {
            // Remove any existing callbacks
            workOrderPollingApiHandler.removeCallbacks(workOrderPollingApiRunnable);
        }
        workOrderPollingApiRunnable = new Runnable() {
            @Override
            public void run() {
                getAssets(APIConstants.M_GET_ASSETS, "GETTING ASSETS");
                workOrderPollingApiHandler.postDelayed(this, 5000);
            }
        };
        workOrderPollingApiHandler.postDelayed(workOrderPollingApiRunnable, 2000);
    }

    private void stopWorkOrderPollingApiHandler() {
        // Remove any pending callbacks and messages
        workOrderPollingApiHandler.removeCallbacks(workOrderPollingApiRunnable);
    }
        public void getAssets(String METHOD_NAME, String progress_message) {
        //showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        //Log.e("UPLOADURL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        //Log.e("UPLOADREQUEST",loginRequestObject.toString());
        AndroidNetworking.get(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME+"/"+SharedPreferencesManager.getDeviceId(context))//+"/"+SharedPreferencesManager.getSavedUserId(context))//.addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        if (result != null) {
                             try {
                            Log.e("GETASSETRES",result.toString());
                            boolean status = result.getBoolean("status");
                            if(status){
                                JSONArray dataArray = result.getJSONArray("data");
                                List<AssetMaster> assetList = new ArrayList<>();

                                for(int i=0;i<dataArray.length();i++){
                                    JSONObject dataObject = dataArray.getJSONObject(i);
                                    AssetMaster assetMaster = new AssetMaster();
                                    String assetId= "";
                                    String assetName = AppConstants.UNKNOWN_ASSET;
                                    String assetSrNo = "";
                                    String assetTagId = "";
                                    String assetDesc = "";
                                    String assetGroupCode = "";
                                    String assetCategoryId = "";
                                    String assetLevel = "";
                                    String assetCreatedOn = "";
                                    String assetOutLife = "";
                                    if(dataObject.has("AssettesId")){
                                        assetId = dataObject.getString("AssettesId");
                                    }
                                    if(dataObject.has("AssetteName")){
                                        assetName = dataObject.getString("AssetteName");
                                    }
                                    if(dataObject.has("SerialNo")){
                                        assetSrNo = dataObject.getString("SerialNo");
                                    }
                                    if(dataObject.has("TagId")){
                                        assetTagId = dataObject.getString("TagId");
                                    }
                                    if(dataObject.has("Description")){
                                        assetDesc = dataObject.getString("Description");
                                    }
                                    if(dataObject.has("GroupCode")){
                                        assetGroupCode = dataObject.getString("GroupCode");
                                    }
                                    if(dataObject.has("CategoryId")){
                                        assetCategoryId = dataObject.getString("CategoryId");
                                    }
                                    if(dataObject.has("AssetteLevel")){
                                        assetLevel = dataObject.getString("AssetteLevel");
                                    }
                                    if(dataObject.has("CreatedOn")){
                                        assetCreatedOn = dataObject.getString("CreatedOn");
                                    }
                                    if(dataObject.has("OutLife")){
                                        assetOutLife = dataObject.getString("OutLife");
                                    }

                                    assetMaster.setAssetId(assetId);
                                    assetMaster.setAssetName(assetName);
                                    assetMaster.setAssetSrNo(assetSrNo);
                                    assetMaster.setAssetTagId(assetTagId);
                                    assetMaster.setAssetDesc(assetDesc);
                                    assetMaster.setAssetGroupCode(assetGroupCode);
                                    assetMaster.setAssetCategoryId(assetCategoryId);
                                    assetMaster.setAssetLevel(assetLevel);
                                    assetMaster.setAssetCreatedOn(assetCreatedOn);
                                    assetMaster.setAssetOutLife(assetOutLife);

                                    assetList.add(assetMaster);

                                }
                                if(dataArray.length()>0){
                                    db.deleteAssetMaster();
                                }
                                db.storeAssetMaster(assetList);
                                //selectactivityandgo(type);

                            }else{
                                //selectactivityandgo(type);
                            }

                            } catch (JSONException e) {
                                 //selectactivityandgo(type);
                                //hideProgressDialog();
                                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.something_went_wrong_error));
                            }
                        } else {
                            //selectactivityandgo(type);
                           // hideProgressDialog();
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e("ASSETEXC",""+anError.getErrorDetail());

                        //selectactivityandgo(type);

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

//    public void getAssets(String str, String str2) {
//        AssetUtils.showProgress(this.context, str2);
//        OkHttpClient build = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build();
//        AndroidNetworking.get(SharedPreferencesManager.getHostUrl(this.context) + str + "/" + SharedPreferencesManager.getDeviceId(this.context)).setTag((Object) "test").setPriority(Priority.LOW).setOkHttpClient(build).build().getAsJSONObject(new JSONObjectRequestListener() {
//            public void onResponse(JSONObject jSONObject) {
//                JSONArray jSONArray;
//                String str;
//                String str2;
//                String str3;
//                String str4;
//                String str5;
//                String str6;
//                String str7;
//                String str8;
//                String str9;
//                String str10;
//                String str11;
//                String str12;
//                String str13;
//                String str14;
//                String str15;
//                JSONObject jSONObject2 = jSONObject;
//                String str16 = "CreatedOn";
//                String str17 = "AssetteLevel";
//                String str18 = "CategoryId";
//                String str19 = "GroupCode";
//                String str20 = "Description";
//                String str21 = "TagId";
//                String str22 = "SerialNo";
//                String str23 = "AssetteName";
//                AssetUtils.hideProgressDialog();
//                if (jSONObject2 != null) {
//                    try {
//                        Log.e("GETASSETRES", jSONObject.toString());
//                        if (jSONObject2.getBoolean("status")) {
//                            JSONArray jSONArray2 = jSONObject2.getJSONArray(APIConstants.K_DATA);
//                            ArrayList arrayList = new ArrayList();
//                            if (jSONArray2.length() > 0) {
//                                DashboardActivity.this.db.deleteAssetMaster();
//                            }
//                            int i = 0;
//                            while (i < jSONArray2.length()) {
//                                JSONObject jSONObject3 = jSONArray2.getJSONObject(i);
//                                AssetMaster assetMaster = new AssetMaster();
//                                String str24 = AppConstants.UNKNOWN_ASSET;
//                                String str25 = "";
//                                if (jSONObject3.has("AssettesId")) {
//                                    jSONArray = jSONArray2;
//                                    str = jSONObject3.getString("AssettesId");
//                                } else {
//                                    jSONArray = jSONArray2;
//                                    str = str25;
//                                }
//                                if (jSONObject3.has(str23)) {
//                                    str24 = jSONObject3.getString(str23);
//                                }
//                                String str26 = str23;
//                                String str27 = str24;
//                                if (jSONObject3.has(str22)) {
//                                    str2 = str22;
//                                    str3 = jSONObject3.getString(str22);
//                                } else {
//                                    str2 = str22;
//                                    str3 = str25;
//                                }
//                                if (jSONObject3.has(str21)) {
//                                    str4 = str21;
//                                    str5 = jSONObject3.getString(str21);
//                                } else {
//                                    str4 = str21;
//                                    str5 = str25;
//                                }
//                                if (jSONObject3.has(str20)) {
//                                    str6 = str20;
//                                    str7 = jSONObject3.getString(str20);
//                                } else {
//                                    str6 = str20;
//                                    str7 = str25;
//                                }
//                                if (jSONObject3.has(str19)) {
//                                    str8 = str19;
//                                    str9 = jSONObject3.getString(str19);
//                                } else {
//                                    str8 = str19;
//                                    str9 = str25;
//                                }
//                                if (jSONObject3.has(str18)) {
//                                    str10 = str18;
//                                    str11 = jSONObject3.getString(str18);
//                                } else {
//                                    str10 = str18;
//                                    str11 = str25;
//                                }
//                                if (jSONObject3.has(str17)) {
//                                    str12 = str17;
//                                    str13 = jSONObject3.getString(str17);
//                                } else {
//                                    str12 = str17;
//                                    str13 = str25;
//                                }
//                                if (jSONObject3.has(str16)) {
//                                    str14 = str16;
//                                    str15 = jSONObject3.getString(str16);
//                                } else {
//                                    str14 = str16;
//                                    str15 = str25;
//                                }
//                                if (jSONObject3.has(APIConstants.K_KIT_OUT_LIFE)) {
//                                    str25 = jSONObject3.getString(APIConstants.K_KIT_OUT_LIFE);
//                                }
//                                assetMaster.setAssetId(str);
//                                assetMaster.setAssetName(str27);
//                                assetMaster.setAssetSrNo(str3);
//                                assetMaster.setAssetTagId(str5);
//                                assetMaster.setAssetDesc(str7);
//                                assetMaster.setAssetGroupCode(str9);
//                                assetMaster.setAssetCategoryId(str11);
//                                assetMaster.setAssetLevel(str13);
//                                assetMaster.setAssetCreatedOn(str15);
//                                assetMaster.setAssetOutLife(str25);
//                                arrayList.add(assetMaster);
//
//                                i++;
//                                jSONArray2 = jSONArray;
//                                str22 = str2;
//                                str23 = str26;
//                                str21 = str4;
//                                str20 = str6;
//                                str19 = str8;
//                                str18 = str10;
//                                str17 = str12;
//                                str16 = str14;
//                            }
//                            DashboardActivity.this.db.storeAssetMaster(arrayList);
//                            DashboardActivity.this.startActivity(new Intent(DashboardActivity.this, KitDataUploadActivity.class));
//                            return;
//                        }
//                        DashboardActivity.this.startActivity(new Intent(DashboardActivity.this, KitDataUploadActivity.class));
//                    } catch (JSONException unused) {
//                        DashboardActivity.this.startActivity(new Intent(DashboardActivity.this, KitDataUploadActivity.class));
//                    }
//                } else {
//                    DashboardActivity.this.startActivity(new Intent(DashboardActivity.this, KitDataUploadActivity.class));
//                }
//            }
//
//            public void onError(ANError aNError) {
//                AssetUtils.hideProgressDialog();
//                Log.e("ASSETEXC", "" + aNError.getErrorDetail());
//                DashboardActivity.this.startActivity(new Intent(DashboardActivity.this, KitDataUploadActivity.class));
//            }
//        });
//    }
@Override
protected void onResume() {
    super.onResume();
    startWorkOrderPollingApiHandler();
}

    @Override
    protected void onPause() {
        super.onPause();
        stopWorkOrderPollingApiHandler();
    }

    @Override
    protected void onDestroy() {
        stopWorkOrderPollingApiHandler();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        stopWorkOrderPollingApiHandler();
        super.onBackPressed();
    }
}

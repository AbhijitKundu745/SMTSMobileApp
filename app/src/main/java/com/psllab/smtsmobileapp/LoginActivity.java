package com.psllab.smtsmobileapp;

import static com.psllab.smtsmobileapp.helper.AssetUtils.hideProgressDialog;
import static com.psllab.smtsmobileapp.helper.AssetUtils.showProgress;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psllab.smtsmobileapp.databases.DatabaseHandler;
import com.psllab.smtsmobileapp.databases.LocationMaster;
import com.psllab.smtsmobileapp.databinding.ActivityLoginBinding;
import com.psllab.smtsmobileapp.encrypt.PSLEncryption;
import com.psllab.smtsmobileapp.helper.APIConstants;
import com.psllab.smtsmobileapp.helper.AssetUtils;
import com.psllab.smtsmobileapp.helper.ConnectionDetector;
import com.psllab.smtsmobileapp.helper.SharedPreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import okhttp3.OkHttpClient;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private Context context = this;
    private DatabaseHandler db;
    private ConnectionDetector cd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        setTitle("USER LOGIN");
        getSupportActionBar().hide();

        cd = new ConnectionDetector(context);
        db = new DatabaseHandler(context);

        /*getAssets(APIConstants.M_GET_ASSETS,"GETTING ASSETS");*/

        String androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        androidID = androidID.toUpperCase();
        SharedPreferencesManager.setDeviceId(context, androidID);
        Log.e("DEVICEID", androidID);

        if (SharedPreferencesManager.getIsHostConfig(context)) {

        } else {
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.url_not_config));
        }

        if (SharedPreferencesManager.getIsLoginSaved(context)) {
            binding.chkRemember.setChecked(true);
            binding.edtUserName.setText(SharedPreferencesManager.getSavedUser(context));
            binding.edtPassword.setText(SharedPreferencesManager.getSavedPassword(context));
        } else {
            binding.chkRemember.setChecked(false);
            binding.edtUserName.setText("");
            binding.edtPassword.setText("");
        }

        binding.btnLogin.setOnClickListener(view -> {
           /* Intent loginIntent = new Intent(LoginActivity.this, DashboardActivity.class);
            startActivity(loginIntent);*/
            if (SharedPreferencesManager.getIsHostConfig(context)) {

                String user = binding.edtUserName.getText().toString().trim();
                String password = binding.edtPassword.getText().toString().trim();
                try {
                    password = PSLEncryption.encrypt(password, PSLEncryption.publicKey);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
                if (user.equalsIgnoreCase("") || password.equalsIgnoreCase("")) {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.login_data_validation));
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(APIConstants.K_USER, user);
                        password = binding.edtPassword.getText().toString().trim();
                        jsonObject.put(APIConstants.K_PASSWORD, password);
                        jsonObject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));

                        //dummyLogin();
                        userLogin(jsonObject, APIConstants.M_USER_LOGIN, "Please wait...\n" + "User login is in progress");


                       /* Intent loginIntent = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(loginIntent);*/
                    } catch (JSONException e) {

                    }
                }
            } else {
                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.url_not_config));
            }

        });

        binding.imgSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent configIntent = new Intent(LoginActivity.this, URLConfigActivity.class);
                startActivity(configIntent);

            }
        });
        binding.btnClear.setOnClickListener(view -> {
            binding.chkRemember.setChecked(false);
            binding.edtUserName.setText("");
            binding.edtPassword.setText("");
            SharedPreferencesManager.setIsLoginSaved(context, false);
            SharedPreferencesManager.setSavedUser(context, "");
            SharedPreferencesManager.setSavedPassword(context, "");
            binding.chkRemember.setChecked(false);
        });
        binding.textDeviceId.setText("Share device ID to admin for device registration\nDevice ID: " + SharedPreferencesManager.getDeviceId(context) + "\nIgnore if device already registered.");

    }

    public void userLogin(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        Log.e("URL", SharedPreferencesManager.getHostUrl(context) + METHOD_NAME);
        Log.e("LOGINREQUEST", loginRequestObject.toString());
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
                                Log.e("LOGINRESULT", result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    SharedPreferencesManager.setSavedUser(context, loginRequestObject.getString(APIConstants.K_USER));
                                    //SharedPreferencesManager.setSavedUserId(context,loginRequestObject.getString(APIConstants.K_USER_ID));
                                    SharedPreferencesManager.setSavedPassword(context, loginRequestObject.getString(APIConstants.K_PASSWORD));
                                    SharedPreferencesManager.setSavedPassword(context, binding.edtPassword.getText().toString().trim());

                                    if (binding.chkRemember.isChecked()) {
                                        SharedPreferencesManager.setIsLoginSaved(context, true);
                                    } else {
                                        SharedPreferencesManager.setIsLoginSaved(context, false);
                                    }
                                   /* Intent loginIntent1 = new Intent(LoginActivity.this, DashboardActivity.class);
                                    startActivity(loginIntent1);
                                    finish();*/

                                    // JSONArray dataObject = null;
                                    JSONArray locationMaster = null;
                                    if (result.has(APIConstants.K_DATA)) {
                                        locationMaster = result.getJSONArray(APIConstants.K_DATA);
                                        if (locationMaster != null) {
                                            List<LocationMaster> locationMasterList = new ArrayList<>();
                                            // if(dataObject.has(APIConstants.K_AUTOCLAVE_LOCATION_MASTER)){
                                            //JSONArray locationMaster = null;
                                            // locationMaster = dataObject.getJSONArray(APIConstants.K_AUTOCLAVE_LOCATION_MASTER);
                                            if (locationMaster.length() > 0) {
                                                for (int vendor = 0; vendor < locationMaster.length(); vendor++) {
                                                    LocationMaster locationmaster = new LocationMaster();
                                                    JSONObject locationObject = locationMaster.getJSONObject(vendor);
                                                    if (locationObject.has(APIConstants.K_LOCATION_ID)) {
                                                        locationmaster.setLocationId(locationObject.getString(APIConstants.K_LOCATION_ID));
                                                    }

                                                    if (locationObject.has(APIConstants.K_LOCATION_NAME)) {
                                                        locationmaster.setLocationName(locationObject.getString(APIConstants.K_LOCATION_NAME));
                                                    }
                                                    locationMasterList.add(locationmaster);
                                                }
                                            }

                                            // }
                                            if (locationMasterList.size() > 0) {
                                                db.deleteAutoclaveLocationMaster();
                                                db.storeAutoclaveLocationMaster(locationMasterList);
                                            }

                                        }
                                    }
                                    Intent loginIntent1 = new Intent(LoginActivity.this, DashboardActivity.class);
                                    startActivity(loginIntent1);
                                    finish();

                                } else {
                                    //dummyLogin();
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, "Please check credentials");
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

    private void dummyLogin() {
        try {
            String status = "true";
            String message = "";

            if (status.equalsIgnoreCase("true")) {
                SharedPreferencesManager.setSavedUser(context, "psladmin");
                //SharedPreferencesManager.setSavedUserId(context,loginRequestObject.getString(APIConstants.K_USER_ID));
                SharedPreferencesManager.setSavedPassword(context, "psladmin!23");
                SharedPreferencesManager.setSavedPassword(context, binding.edtPassword.getText().toString().trim());

                if (binding.chkRemember.isChecked()) {
                    SharedPreferencesManager.setIsLoginSaved(context, true);
                } else {
                    SharedPreferencesManager.setIsLoginSaved(context, false);
                }


                List<LocationMaster> locationMasterList = new ArrayList<>();
                LocationMaster locationmaster = new LocationMaster();
                locationmaster.setLocationId("123");
                locationmaster.setLocationName("Name 123");
                locationMasterList.add(locationmaster);

                LocationMaster locationmaster1 = new LocationMaster();
                locationmaster1.setLocationId("1234");
                locationmaster1.setLocationName("Name 1234");
                locationMasterList.add(locationmaster1);

                if (locationMasterList.size() > 0) {
                    Log.e("LOCATIONSIZE",""+locationMasterList.size());
                    db.deleteAutoclaveLocationMaster();
                    db.storeAutoclaveLocationMaster(locationMasterList);
                    db.deleteLocationMaster();
                    db.storeLocationMaster(locationMasterList);
                }

                // }
                Intent loginIntent1 = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(loginIntent1);
                finish();

            } else {
                AssetUtils.showCommonBottomSheetErrorDialog(context, message);
            }
        } catch (Exception e) {
            hideProgressDialog();
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.something_went_wrong_error));
        }
    }


}
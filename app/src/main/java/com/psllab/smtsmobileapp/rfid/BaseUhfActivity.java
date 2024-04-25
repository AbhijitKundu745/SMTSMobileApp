package com.psllab.smtsmobileapp.rfid;

import static com.seuic.uhf.UHFService.PARAMETER_HIDE_PC;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.psllab.smtsmobileapp.helper.SharedPreferencesManager;
import com.seuic.uhf.UHFService;

import java.lang.reflect.Method;

public class BaseUhfActivity extends AppCompatActivity {
    public UHFService mDevice;
    public boolean is_uhf_success= false;
    public static void stopApps(Context context, String packageName) {
        try {
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            Method forceStopPackage = am.getClass().getDeclaredMethod("forceStopPackage", String.class);
            forceStopPackage.setAccessible(true);
            forceStopPackage.invoke(am, packageName);
            Log.i("zy","TimerV force stop package "+packageName+" successful");
            System.out.println("TimerV force stop package "+packageName+" successful");
        }catch(Exception ex) {
            ex.printStackTrace();
            System.err.println("TimerV force stop package "+packageName+" error!");
            Log.i("zy","TimerV force stop package "+packageName+" error!");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stopApps(BaseUhfActivity.this,"com.seuic.uhftool");

        // new object
        /*mDevice = UHFService.getInstance();
        // open UHF
        boolean ret = mDevice.open();
        if (!ret) {
            Toast.makeText(this, "UHF Failed", Toast.LENGTH_SHORT).show();
        }else{
            // mDevice.setAntennaMode(1);
            is_uhf_success = true;
        }*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        // new object
        try {
            mDevice = UHFService.getInstance();
            // open UHF
            boolean ret = mDevice.open();
            if (!ret) {
                Toast.makeText(this, "UHF Failed", Toast.LENGTH_SHORT).show();
            } else {
                is_uhf_success = true;
                try {
                    // mDevice.setParameters(UHFService.PARAMETER_CLEAR_EPCLIST, 1);
                    mDevice.setPower(30);
                    mDevice.setParameters(PARAMETER_HIDE_PC, 1);
                    SharedPreferencesManager.setPower(this, 30);
                } catch (Exception e) {

                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // close UHF
        // mDevice.close();
        try {
            mDevice.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // close UHF
        // mDevice.close();
        try {
            mDevice.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


}
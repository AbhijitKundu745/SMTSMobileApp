package com.psllab.smtsmobileapp.rfid;

import static com.seuic.uhf.UHFService.PARAMETER_HIDE_PC;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.psllab.smtsmobileapp.R;
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
    Dialog powersettingDialog;
    public void openPowerSettingDialog(Context context) {
        if (powersettingDialog != null) {
            powersettingDialog.dismiss();
        }

        powersettingDialog = new Dialog(context);
        powersettingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        powersettingDialog.setCancelable(false);
        powersettingDialog.setContentView(R.layout.custom_setting_dialog_layout);
        ImageView img = (ImageView) powersettingDialog.findViewById(R.id.img);
        TextView image_dialog = (TextView) powersettingDialog.findViewById(R.id.image_dialog);

        image_dialog.setText("Set Reader Power");

        TextView tip = (TextView) powersettingDialog.findViewById(R.id.tip);

        TextView textClose = (TextView) powersettingDialog.findViewById(R.id.textClose);


        textClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                powersettingDialog.dismiss();
            }
        });

        if (SharedPreferencesManager.getPower(context)==30) {
            tip.setTextColor(context.getResources().getColor(R.color.green));
            tip.setText("Current  RF Power : HIGH");
            img.setImageDrawable(context.getResources().getDrawable(R.drawable.success));
        } else if (SharedPreferencesManager.getPower(context)==25){
            tip.setTextColor(context.getResources().getColor(R.color.boh));
            tip.setText("Current  RF Power : MEDIUM");
            img.setImageDrawable(context.getResources().getDrawable(R.drawable.success));
        }
        else if (SharedPreferencesManager.getPower(context)==20){
            tip.setTextColor(context.getResources().getColor(R.color.red));
            tip.setText("Current  RF Power : LOW");
            img.setImageDrawable(context.getResources().getDrawable(R.drawable.success));
        }else{
            tip.setTextColor(context.getResources().getColor(R.color.red));
            tip.setText("Current  RF Power : LOW");
            img.setImageDrawable(context.getResources().getDrawable(R.drawable.success));
        }

        Button btnHigh = (Button) powersettingDialog.findViewById(R.id.btnHigh);
        Button btnMedium = (Button) powersettingDialog.findViewById(R.id.btnMedium);
        Button btnLow = (Button) powersettingDialog.findViewById(R.id.btnLow);
        btnLow.setVisibility(View.VISIBLE);

        btnHigh.setText("HIGH");
        btnMedium.setText("MEDIUM");
        btnLow.setText("LOW");
        btnHigh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                powersettingDialog.dismiss();
                SharedPreferencesManager.setPower(context, 30);
                setAntennaPower(context,String.valueOf(30));
            }
        });
        btnMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                powersettingDialog.dismiss();
                SharedPreferencesManager.setPower(context, 25);
                setAntennaPower(context,String.valueOf(25));
            }
        });
        btnLow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                powersettingDialog.dismiss();
                SharedPreferencesManager.setPower(context, 20);
                setAntennaPower(context,String.valueOf(20));
            }
        });
        // successdialog.getWindow().getAttributes().windowAnimations = R.style.FadeInOutAnimation;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing()) {
                powersettingDialog.show();
                // startCountDownTimer();
            }
        }
    }


    private void setAntennaPower(Context context,String power) {
        try {
            if (TextUtils.isEmpty(power)) {
                Toast.makeText(context, "Power cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            int p = Integer.parseInt(power);
            int m = 30;
            if ((p < 0) || (p > m)) {
                Toast.makeText(context, context.getResources().getString(R.string.power_range), Toast.LENGTH_SHORT).show();
            } else {
                try {
                    // mDevice.setParameters(UHFService.PARAMETER_CLEAR_EPCLIST, 1);
                    boolean rv =  mDevice.setPower(p);
                    if (!rv) {
                        Toast.makeText(context, context.getResources().getString(R.string.set_power_fail), Toast.LENGTH_SHORT).show();
                    } else {
                        //Toast.makeText(context, context.getResources().getString(R.string.set_power_ok), Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){

                }

            }
        }catch (Exception ex){
            ex.printStackTrace();
        }


    }


}
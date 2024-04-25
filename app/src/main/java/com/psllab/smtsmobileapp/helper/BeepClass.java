package com.psllab.smtsmobileapp.helper;


import android.content.Context;
import android.media.MediaPlayer;

import com.psllab.smtsmobileapp.R;


/**
 * Created by Admin on 06/Nov/2017.
 */

public class BeepClass {

    public static void successbeep(Context context){
        try {

            MediaPlayer sound1 = MediaPlayer.create(context, R.raw.scan);
            if (sound1.isPlaying() == true) {
                sound1.pause();
            } else {
                sound1.start();
            }
            sound1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                }
            });
        }catch (Exception e){

        }
    }



    public static void errorbeep(Context context){
        try {

            MediaPlayer sound1 = MediaPlayer.create(context, R.raw.errorbeep);
            if (sound1.isPlaying() == true) {
                sound1.pause();
            } else {
                sound1.start();
            }
            sound1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                }
            });
        }catch (Exception e){

        }
    }


}

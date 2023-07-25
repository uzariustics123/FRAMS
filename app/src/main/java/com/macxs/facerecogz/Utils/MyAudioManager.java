package com.macxs.facerecogz.Utils;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.widget.Toast;

import com.macxs.facerecogz.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyAudioManager {
    Context context;
    Activity activity;
    SoundPool soundPool;
    private static HashMap<String, Integer> soundPoolMap = new HashMap<>();
    AudioManager audioManager;
    public static int getCloserAudio;
    public static int youvebeenAudio;
    public static int youalreadyAudio;
    public static int staystillAudio;
    public static int dingAudio;
    public static int buzzer;


    public MyAudioManager(Context context) {
        this.context = context;
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1).setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .build();
        getCloserAudio = soundPool.load(context, R.raw.speech1, 1);
        youvebeenAudio = soundPool.load(context, R.raw.speech2, 1);
        youalreadyAudio = soundPool.load(context, R.raw.speech3, 1);
        staystillAudio = soundPool.load(context, R.raw.speech4, 1);
        dingAudio = soundPool.load(context, R.raw.ding, 1);
        buzzer = soundPool.load(context, R.raw.buzzer, 1);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
 public void play(int soundID) {

     float volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
     soundPool.play(soundID, volume, volume, 1, 0, 1f);
 }
 public void release(){
        soundPool.release();
 }
}

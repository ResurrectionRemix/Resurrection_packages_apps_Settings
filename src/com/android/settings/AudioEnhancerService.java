package com.android.settings;

 import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import com.android.settings.AudioEnhancerUtils;

 public class AudioEnhancerService extends Service {

    public static AudioEnhancerUtils du;

    @Override
   public IBinder onBind(Intent arg0) {
      return null;
   }

    @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      // Let it continue running until it is stopped.
      du = new AudioEnhancerUtils();
      du.initialize();
      //Toast.makeText(this, "Audio Enhancer Started", Toast.LENGTH_LONG).show();
      return START_STICKY;
   }


    @Override
   public void onDestroy() {
      super.onDestroy();
      Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
   }
}
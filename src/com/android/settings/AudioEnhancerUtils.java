package com.android.settings;

 import android.content.Context;
import android.util.Log;
import android.util.Pair;
import java.util.List;
import java.util.*;
import com.android.settings.AudioEnhancer;

 public class AudioEnhancerUtils
{
  private AudioEnhancer mAudEnhncr;
  private boolean mInitialized;


   public int getHeadsetType(Context paramContext)
  {
    return mAudEnhncr.getHeadsetType();
  }

   public boolean hasInitialized()
  {
    return mInitialized;
  }

   public void initialize()
  {
    boolean enabled;
    int iEnabled;
    if (!mInitialized)
    {
      mInitialized = true;
      mAudEnhncr = new AudioEnhancer(0, 0);
      iEnabled = mAudEnhncr.getMusic();
      if (iEnabled == 1) {
        enabled = true;
      }else {
        enabled = false;
      }
      mAudEnhncr.setEnabled(enabled);
    }
  }

   public boolean isEnabled(Context paramContext)
  {
    int i =1;
    int j = 0;
    j = mAudEnhncr.getMusic();
    if (i == j)
    {
      return true;
    } else {
      return false;
    }
  }

   public void release()
  {
    if (mInitialized)
    {
      mAudEnhncr.release();
      mAudEnhncr = null;
      mInitialized = false;
    }
  }

   public void setEnabled(Context paramContext, boolean paramBoolean)
  {
    int i = 1;
    if (paramBoolean)
    {
      i = 1;
    } else {
      i = 0;
    }
	mAudEnhncr.setEnabled(paramBoolean);
    mAudEnhncr.setMusic(i);
    return;
  }

   public void setHeadsetType(Context paramContext, int paramInt)
  {
    mAudEnhncr.setHeadsetType(paramInt);
  }


   public void setMode(Context paramContext, int paramInt)
  {
    mAudEnhncr.setMode(paramInt);
  }

  public void setLevel(Context context, String preset) {
      String[] level = preset.split("\\s*,\\s*");

       for (int band = 0; band <= level.length - 1; band++) {
          mAudEnhncr.setLevel(band, Float.valueOf(level[band]));
      }
  }

   public String getLevel(Context context) {
      String selected = "";
      for (int band = 0; band <= 6; band++) {
          int temp = (int) mAudEnhncr.getLevel(band);
          selected += String.valueOf(temp);
          if (band != 6) selected += ",";
      }
      return selected;
  }

 }
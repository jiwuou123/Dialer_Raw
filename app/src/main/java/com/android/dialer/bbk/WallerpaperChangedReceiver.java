package com.android.dialer.bbk;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.android.incallui.utils.BitmapUntils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap.Config;
import android.graphics.Bitmap.CompressFormat;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WallerpaperChangedReceiver extends BroadcastReceiver{
	  private static final String WALLPAPER_KEY = "wallerpaper_key";
    private static final String WALLPAPER_SHARE = "wallerpaper_share";
    private static final String WALLPAPER_BLUR_PATH = "data/data/com.android.dialer/wallpaper";
    private static final String WALLPAPER_BLUR_DIR_PATH = "data/data/com.android.dialer/";
    private BitmapUntils  untils;
    private Bitmap mWallerpaperBlurBitmap = null;
    private int wallpaperBitmapHashCode;
    private WallpaperManager wallpaperManager;
    private Context mContext;
    private static final String TAG = "WallerpaperChangedReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
//		Log.e(TAG,"action:"+intent.getAction());
		mContext = context;
		saveWallerPaper(context);
	}
	   /**
     * saveWallerPaper
     */	
     private void saveWallerPaper(Context context) {
     	  
     	  if(wallpaperManager == null)
	      wallpaperManager = WallpaperManager.getInstance(context); 
//	     	mWallerpaperBlurBitmap = wallpaperManager.getBitmap();
         BitmapDrawable bitmap = (BitmapDrawable) wallpaperManager.getDrawable();
		 mWallerpaperBlurBitmap = bitmap.getBitmap();
	      wallpaperBitmapHashCode = mWallerpaperBlurBitmap.hashCode();
	     	saveBlurCurrent(mWallerpaperBlurBitmap,wallpaperBitmapHashCode);
     	
     	}

    /**
     * getBlurCurrent
     * @param bitmap, String
     * @return Bitmap and save current hashcode in local.
     */	
     private void saveBlurCurrent(Bitmap bitmap,int hashCode) {
//        Log.e(TAG,"getBlurCurrent hashCode = "+hashCode);
        if(untils == null)untils =  new BitmapUntils();
     	  bitmap = untils.rightBig(bitmap,0.05f);//to scale 0.2
     	  bitmap = untils.stackBlur(bitmap,15);//get blur
     	  bitmap = untils.rightBig(bitmap,15.0f);//to scale 4 to save 20% memery.
        saveWallerpaperBlurInLocal(bitmap,WALLPAPER_BLUR_DIR_PATH);
        setWallerpaperHashCode(WALLPAPER_KEY,hashCode);
        
     	} 	
	 	/**
	 	 * to save hashcode inlocal.
	 	 **/	 
	  private void setWallerpaperHashCode(String key,int hashCode) {
	  	
	 	   getSharedPreferences(WALLPAPER_SHARE).edit().putInt(key,hashCode).commit();
	 	   
	 	}
	 	 /**
	 	 * to save current wallpaper with blur in local.
	 	 **/		
	 private void saveWallerpaperBlurInLocal(Bitmap wallpaper,String path) {
//	 	    Log.e(TAG,"saveWallerpaperBlurInLocal");
		    File file = new File(WALLPAPER_BLUR_DIR_PATH);
        if (!file.exists())
            file.mkdir();
 
        file = new File(WALLPAPER_BLUR_PATH);

        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            wallpaper.compress(CompressFormat.JPEG, 50, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /*recycle the bitmap which is produceed in get local bitmap. */
        if(mWallerpaperBlurBitmap != null) {
         mWallerpaperBlurBitmap.recycle();
         mWallerpaperBlurBitmap = null;
         }	 	    
	 	}
	 	/**
	 	 * to get SharedPreferences to save current wallpaper hashcode.
	 	 **/
	 	private SharedPreferences getSharedPreferences(String key) {
	 		
	 		 SharedPreferences wallerpaperSharedPreferences = mContext.getSharedPreferences(key, 0);
	 		 
	 		 return wallerpaperSharedPreferences;
	 		}	 	
}
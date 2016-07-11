package com.android.incallui.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
public class BitmapUntils {
	
	public BitmapUntils() {
		// TODO Auto-generated constructor stub
	}
	/**Bitmap scale to 1.5f*/ 
	private static Bitmap big(Bitmap bitmap) { 

	     Matrix matrix = new Matrix();
		matrix.postScale(1.5f,1.5f); 
	  Bitmap scaleBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),
		        bitmap.getHeight(),matrix,true); 
		bitmap = scaleBitmap.copy(scaleBitmap.getConfig(), true);
		scaleBitmap.recycle();
		scaleBitmap = null;
		
		return bitmap; 
	
	} 
		/**Bitmap scale */ 
	public  Bitmap rightBig(Bitmap bitmap) { 
		
	  int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float scale = 1.0f;
		float w = 270.00f/width;
		float h = 270.00f/height;
		if(w > h) scale = w;
		  else scale = h;
		Matrix matrix = new Matrix(); 
		matrix.postScale(scale,scale); 
	  bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),
		        bitmap.getHeight(),matrix,true); 
		        
	return bitmap; 
	
	} 
	/*
	 * @liupengfei
	 * to get round corner bitmap
	 */
	public  Bitmap makeRoundCorner(Bitmap bitmap)
	{
		bitmap = rightBig(bitmap);
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Log.e("liupengfei","width = "+width+",height = "+height);
		int left = 0, top = 0, right = width, bottom = height;
		float roundPx = height/2;
		if (width > height) {
			left = (width - height)/2;
			top = 0;
			right = left + height;
			bottom = height;
		} else if (height > width) {
			left = 0;
			top = (height - width)/2;
			right = width;
			bottom = top + width;
			roundPx = width/2;
		}

		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		int color = 0xff424242;
		Paint paint = new Paint();
		Rect rect = new Rect(left, top, right, bottom);
		RectF rectF = new RectF(rect);

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}
	/** 
	* Drawable to Bitmap 
	*/  
	public  Bitmap drawableToBitmap(Drawable drawable) {  
	   int width = drawable.getIntrinsicWidth();  
	   int height = drawable.getIntrinsicHeight();  
	   Bitmap bitmap = Bitmap.createBitmap(width, height, drawable.getOpacity() != PixelFormat.OPAQUE ? Config.ARGB_8888 : Config.RGB_565);
	   Canvas canvas = new Canvas(bitmap);  
	   drawable.setBounds(0, 0, width, height);  
	   drawable.draw(canvas);  
	   return bitmap;  
	  
	}  
	/**
	 * Bitmap to Drawable
	 * @param bitmap
	 * @param mcontext
	 * @return
	 */
	public Drawable bitmapToDrawble(Bitmap bitmap,Context mcontext){
		Drawable drawable = new BitmapDrawable(mcontext.getResources(), bitmap);
		return drawable;
	}
	  /**
     * Blur int google api
     * @param bmp
     * @return
     */				
	public Bitmap blurBitmap(Bitmap bitmap,Context mContext){  
        
        //Let's create an empty bitmap with the same size of the bitmap we want to blur  
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);  
          
        //Instantiate a new Renderscript  
        RenderScript rs = RenderScript.create(mContext);  
          
        //Create an Intrinsic Blur Script using the Renderscript  
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));  
          
        //Create the Allocations (in/out) with the Renderscript and the in/out bitmaps  
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);  
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);  
          
        //Set the radius of the blur  
        blurScript.setRadius(25f);  
          
        //Perform the Renderscript  
        blurScript.setInput(allIn);  
        blurScript.forEach(allOut);  
          
        //Copy the final bitmap created by the out Allocation to the outBitmap  
        allOut.copyTo(outBitmap);  
          
        //recycle the original bitmap  
        //bitmap.recycle();  
        //bitmap = null;  
        //After finishing everything, we destroy the Renderscript.  
        rs.destroy();  
        blurScript.destroy();
        allIn.destroy();
        allOut.destroy();
        return outBitmap;
         
    }
    /**
     * Blur in net
     * @param bmp
     * @return
     */	
    public Bitmap stackBlur(Bitmap inBmp, int radius) {
        if (radius < 1) {
            return (null);
        }

        Bitmap bitmap = inBmp.copy(inBmp.getConfig(), true);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }
		/**Bitmap scale */ 
	public  Bitmap rightBig(Bitmap bitmap,float scale) { 
		
    Matrix matrix = new Matrix();
		matrix.postScale(scale,scale);
		Log.e("liupengfei","bitmap = "+bitmap);
	  Bitmap scaleBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),
		        bitmap.getHeight(),matrix,true); 
		bitmap = scaleBitmap.copy(scaleBitmap.getConfig(), true);
		scaleBitmap.recycle();
		scaleBitmap = null;
	return bitmap; 
	
	} 
	public Bitmap mergeBitmap(Bitmap firstBitmap, Bitmap secondBitmap) {
  Bitmap bitmap = Bitmap.createBitmap(firstBitmap.getWidth(), firstBitmap.getHeight(),firstBitmap.getConfig());
  Canvas canvas = new Canvas(bitmap);
  Paint vPaint = new Paint();  
  vPaint.setStyle(Paint.Style.STROKE);
  vPaint.setColor(Color.BLACK);
  vPaint.setAlpha(65);
  canvas.drawBitmap(firstBitmap, new Matrix(), null);
  canvas.drawBitmap(secondBitmap, 0, 0, vPaint);
  return bitmap;
}
 public  Bitmap createNewBitmap(int w,int h){
 	 Bitmap newBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
   return newBitmap;
 	}
}

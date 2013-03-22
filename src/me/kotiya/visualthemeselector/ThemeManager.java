package me.kotiya.visualthemeselector;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.InputStream;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import java.util.Arrays;
import java.io.FileWriter;
import java.io.BufferedWriter;
import android.graphics.Rect;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.NinePatch;


public class ThemeManager {

    private static final String sThemeDirs[] = {
            "bootanime",
            "frame",
            "launcher",
            "lockscreen",
            "navikey",
            "notification",
            "screenshot",
            "statusbar",
            "navibar",
            "simeji",
            "atok",
            "sounds/effect",
            "sounds/bootsound",
            "sounds/camera",
            "wallpaper",
            "font",
            "flickwnn",
    };

    private Context mContext;
    private ContentResolver mContentResolver;
    private WallpaperManager mWallpaperManager;
    private WindowManager mWindowManager;
    private Handler mHandler;
    private static final String TAG = "ThemeManager";

    public ThemeManager(Activity activity) {
        mContext = activity;
        mContentResolver = mContext.getContentResolver();
        mWallpaperManager = WallpaperManager.getInstance(mContext);
        mWindowManager = activity.getWindowManager();
        mHandler = new Handler();
    }

    public void restartLauncher(final Runnable afterProc) {
        restartSystemUI(new Runnable() {
            public void run() {
                String forceHobby = SystemProperties.get("persist.sys.force.hobby");
                if (forceHobby.equals("true")) {
                    applyTheme();
                }
                if (afterProc != null) {
                    afterProc.run();
                }
            }
        });
    }

    private void themeZipInstall(String themeFile) {
        File f = null;
        InputStream is = null;
        ZipInputStream in = null;
        ZipEntry zipEntry = null;
        
        f = new File(themeFile);
        
		try {
			is = new FileInputStream(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	        
       	in = new ZipInputStream(is);
       	String parentPath = Environment.getDataDirectory().toString() + "/theme/";
       	final File parent = new File(parentPath);
       	if (!parent.exists()) {
           	parent.mkdirs();
       	}

        try {
            while ((zipEntry = in.getNextEntry()) != null) {

                final File file = new File(parentPath, zipEntry.getName());

                if (zipEntry.isDirectory()) {
                	if (!file.exists()) {
                		file.mkdirs();
                	}
                } else {
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    int length;
                    int writeSize = 0;
                    byte []buffer = new byte[4096];
                    while ((length = in.read(buffer)) != -1) {
                        out.write(buffer,0,length);
                        writeSize += length;
                    }
                    out.flush();
                    out.close();
                    file.setReadable(true, false);
                    Log.d("File","name:" + file.toString() + " size:" + file.length() + " writeSize:" + writeSize );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTheme(final String themeName, final Runnable afterProc, final boolean performReset) {
        new Thread(new Runnable() {
            public void run() {
                themeAllClear();
                setDefaultCameraSounds();

                File file = new File(Environment.getExternalStorageDirectory().toString() + "/mytheme/" + themeName + ".zip");
                if (file.exists()) {
                	themeZipInstall(Environment.getExternalStorageDirectory().toString() + "/mytheme/" + themeName + ".zip");
                }else {
                    themeAllInstall();
                }
                setDefaultSounds();
                setMySounds();
                setFlickWnnTheme();

                if (performReset == true) {
                    applyTheme();
                    PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
                    pm.reboot(null);
                } else {
                    restartSystemUI(new Runnable() {
                        public void run() {
                            applyTheme();
                            if (afterProc != null) {
                                afterProc.run();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public void clearTheme(final Runnable afterProc) {
        setDefaultSounds();
        themeAllClear();

        restartSystemUI(new Runnable() {
            public void run() {
                try {
                    mWallpaperManager.clear();
                } catch (IOException e) {
                }
                if (afterProc != null) {
                    afterProc.run();
                }
            }
        });
    }

    private void setDefaultSounds() {
        Settings.Global.putString(mContentResolver, Settings.Global.LOW_BATTERY_SOUND,
                "/system/media/audio/ui/LowBattery.ogg");
        Settings.Global.putString(mContentResolver, Settings.Global.DESK_DOCK_SOUND,
                "/system/media/audio/ui/Dock.ogg");
        Settings.Global.putString(mContentResolver, Settings.Global.DESK_UNDOCK_SOUND,
                "/system/media/audio/ui/Undock.ogg");
        Settings.Global.putString(mContentResolver, Settings.Global.CAR_DOCK_SOUND,
                "/system/media/audio/ui/Dock.ogg");
        Settings.Global.putString(mContentResolver, Settings.Global.CAR_UNDOCK_SOUND,
                "/system/media/audio/ui/Undock.ogg");
        Settings.Global.putString(mContentResolver, Settings.Global.LOCK_SOUND,
                "/system/media/audio/ui/Lock.ogg");
        Settings.Global.putString(mContentResolver, Settings.Global.UNLOCK_SOUND,
                "/system/media/audio/ui/Unlock.ogg");
    }

    private void setDefaultCameraSounds() {
        // Set default camera sounds
        StringBuilder ibuilder = new StringBuilder();
        StringBuilder obuilder = new StringBuilder();
        ibuilder.append("/system/media/audio/ui/");
        obuilder.append(Environment.getDataDirectory().toString() + "/theme/sounds/camera/");
        String iDirPath = ibuilder.toString();
        String oDirPath = obuilder.toString();
        File iDir = new File(iDirPath);
        File oDir = new File(oDirPath);
        themeCopy(iDir, oDir);
    }

    private void setDataBase(String key, String name) {
        StringBuilder builder = new StringBuilder();
        builder.append(Environment.getDataDirectory().toString() + "/theme/sounds/effect/");
        builder.append(File.separator);
        builder.append(name);
        String filePath = builder.toString();
        File file = new File(filePath);
        if (file.exists()) {
            Settings.Global.putString(mContentResolver, key, filePath);
        }
    }

    private void setMySounds() {
        String forceHobby = SystemProperties.get("persist.sys.force.hobby");
        if (forceHobby.equals("true")) {
            setDataBase(Settings.Global.LOW_BATTERY_SOUND, "LowBattery.ogg");
            setDataBase(Settings.Global.DESK_DOCK_SOUND, "Dock.ogg");
            setDataBase(Settings.Global.DESK_UNDOCK_SOUND, "UnDock.ogg");
            setDataBase(Settings.Global.CAR_DOCK_SOUND, "CarDock.ogg");
            setDataBase(Settings.Global.CAR_UNDOCK_SOUND, "UnCarDock.ogg");
            setDataBase(Settings.Global.LOCK_SOUND, "Lock.ogg");
            setDataBase(Settings.Global.UNLOCK_SOUND, "unLock.ogg");
        }
    }

    public void themeCopy(File iDir, File oDir) {
        if (iDir.isDirectory()) {
            String[] children = iDir.list();
            for (int i = 0; i < children.length; i++) {
                File iFile = new File(iDir, children[i]);
                File oFile = new File(oDir, children[i]);

                try {
                    FileChannel iChannel = new FileInputStream(iFile).getChannel();
                    FileChannel oChannel = new FileOutputStream(oFile).getChannel();
                    iChannel.transferTo(0, iChannel.size(), oChannel);
                    iChannel.close();
                    oChannel.close();
                    oFile.setReadable(true, false);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void themeInstall(String parts) {
        StringBuilder ibuilder = new StringBuilder();
        StringBuilder obuilder = new StringBuilder();
        ibuilder.append(Environment.getExternalStorageDirectory().toString() + "/mytheme/" + SystemProperties.get("persist.sys.theme") + "/" + parts + "/");
        obuilder.append(Environment.getDataDirectory().toString() + "/theme/" + parts + "/");
        String iDirPath = ibuilder.toString();
        String oDirPath = obuilder.toString();
        File iDir = new File(iDirPath);
        File oDir = new File(oDirPath);
        themeCopy(iDir, oDir);
    }

    public void themeAllInstall() {
        for (String dir : sThemeDirs) {
            themeInstall(dir);
        }
    }

    public void themeDelete(File iDir) {
        if (iDir.isDirectory()) {
            String[] children = iDir.list();
            for (int i = 0; i < children.length; i++) {
                File iFile = new File(iDir, children[i]);
                iFile.delete();
            }
        }
    }

    public void themeClear(String parts) {
        StringBuilder ibuilder = new StringBuilder();
        ibuilder.append(Environment.getDataDirectory().toString() + "/theme/" + parts + "/");
        String iDirPath = ibuilder.toString();
        File iDir = new File(iDirPath);
        themeDelete(iDir);
        deleteFlickWnnTheme();
    }

    public void themeAllClear() {
        for (String dir : sThemeDirs) {
            themeClear(dir);
        }
    }

    private void restartSystemUI(final Runnable postproc) {
        mHandler.post(new Runnable() {
            public void run() {
                try {
                    ActivityManager am = (ActivityManager) mContext
                            .getSystemService(Context.ACTIVITY_SERVICE);
                    am.forceStopPackage("com.android.launcher");
                    Intent jcservice = (new Intent())
                            .setClassName("com.android.systemui",
                                    "com.android.systemui.JcromService");
                    mContext.startActivity(jcservice);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mHandler.postDelayed(postproc, 7500/* ms */);
            }
        });
    }

    private void applyTheme() {
        Bitmap bitmapWallpaper;
        String MY_FRAME_FILE = "home_wallpaper.png";
        StringBuilder builder = new StringBuilder();
        builder.append(Environment.getDataDirectory().toString() + "/theme/wallpaper/");
        builder.append(File.separator);
        builder.append(MY_FRAME_FILE);
        String filePath = builder.toString();
        bitmapWallpaper = BitmapFactory.decodeFile(filePath);
        if (null != bitmapWallpaper) {
            try {
                int srcWidth = bitmapWallpaper.getWidth();
                int srcHeight = bitmapWallpaper.getHeight();

                int screenSize = mContext.getResources().getConfiguration().screenLayout
                        & Configuration.SCREENLAYOUT_SIZE_MASK;
                boolean isScreenLarge = screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE
                        || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
                DisplayMetrics displayMetrics = new DisplayMetrics();
                mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
                int maxDim = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
                int minDim = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
                float WALLPAPER_SCREENS_SPAN = 2f;
                int w, h;
                if (isScreenLarge) {
                    w = (int) (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
                    h = maxDim;
                } else {
                    w = Math.max((int) (minDim * WALLPAPER_SCREENS_SPAN), maxDim);
                    h = maxDim;
                }

                if (w < srcWidth && h < srcHeight) {
                    Matrix matrix = new Matrix();
                    float widthScale = w / (float) srcWidth;
                    float heightScale = h / (float) srcHeight;
                    matrix.postScale(widthScale, heightScale);
                    Bitmap resizedWallpaper = Bitmap.createBitmap(bitmapWallpaper, 0, 0, srcWidth,
                            srcHeight, matrix, true);
                    mWallpaperManager.setBitmap(resizedWallpaper);
                } else {
                    mWallpaperManager.setBitmap(bitmapWallpaper);
                }
            } catch (IOException e) {
            }
        }
    }

    // borrowed from "com/android/launcher2/Workspace.java"
    private float wallpaperTravelToScreenWidthRatio(int width, int height) {

        float aspectRatio = width / (float) height;

        final float ASPECT_RATIO_LANDSCAPE = 16 / 10f;
        final float ASPECT_RATIO_PORTRAIT = 10 / 16f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE = 1.5f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT = 1.2f;

        final float x =
                (WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE - WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT)
                        / (ASPECT_RATIO_LANDSCAPE - ASPECT_RATIO_PORTRAIT);
        final float y = WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT - x * ASPECT_RATIO_PORTRAIT;
        return x * aspectRatio + y;
    }
    
    private void setFlickWnnTheme(){
        
        try{
            ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo("com.pm9.flickwnn", 0);
        } catch (NameNotFoundException ex){
            // Do nothing.
            return;
        }
    
        String mFlickWnnTheme = Environment.getExternalStorageDirectory().toString() + "/FlickWnn/keyboard/";
        String mFlickWnnData = Environment.getDataDirectory().toString() + "/theme/flickwnn/";

        flickWnnThemeCopy(mFlickWnnData, mFlickWnnTheme);     
    }
    
    private void flickWnnThemeCopy(String iPath, String oPath) {
        
        File iDir = new File(iPath);
        File oDir = new File(oPath);

        if (!oDir.exists()){
            oDir.mkdirs();
        }

        if (iDir.isDirectory()) {
            String[] children = iDir.list();

            if(!Arrays.asList(children).contains("text.info")){
                createFlickWnnTextInfo(oPath);
            }
            for (int i = 0; i < children.length; i++) {
                File iFile = new File(iDir, children[i]);
                File oFile = new File(oDir, children[i]);

                try {
                    // Skip file copy in case of inappropriate image.(=failed to get 9patch chunk from image)
                    boolean fcStat = false; 
                    fcStat = createFlickWnnPngInfo(iFile.getName(), iPath, oPath);
                    if(fcStat){
                        FileChannel iChannel = new FileInputStream(iFile).getChannel();
                        FileChannel oChannel = new FileOutputStream(oFile).getChannel();
                        iChannel.transferTo(0, iChannel.size(), oChannel);
                        iChannel.close();
                        oChannel.close();
                        oFile.setReadable(true, false);
                    }
                    
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createFlickWnnTextInfo(String targetPath){
        
        // Default text color.
        // If you want to use custom textinfo, please create custom "text.info" on "/yourtheme/flickwnn/text.info". 
        int textColor = 0xFFFFFFFF;
        int textSize = 22;
        int shadowColor = 0xBB000000;
        float shadowRadius = 2.75F;

        try{
            FileWriter fw = new FileWriter(targetPath + "text.info");
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(String.format("textColor:%8X", textColor));
            bw.newLine();
            bw.write(String.format("textSize:%d", textSize));
            bw.newLine();
            bw.write(String.format("shadowColor:%8X", shadowColor));
            bw.newLine();
            bw.write(String.format("shadowRadius:%#.2f", shadowRadius));
            bw.newLine();
            bw.close();
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private boolean createFlickWnnPngInfo(String ftFile, String themePath, String targetPath){
        
        if (!(ftFile.equals("text.info") || ftFile.equals("keybg_custom_bg.png"))){
            StringBuilder sb = new StringBuilder();
            sb.append(targetPath);
            sb.append(ftFile);
            sb.append(".info");
            String infoFileName = new String(sb);

            StringBuilder sbt = new StringBuilder();
            sbt.append(themePath);
            sbt.append(ftFile);
            String itFile = new String(sbt);

            try{
                FileInputStream fis = new FileInputStream(itFile);
                BufferedInputStream bufs = new BufferedInputStream(fis);

                final Rect padding = new Rect();
                Bitmap bmp = null;
                byte chunk[] = null;

                bmp = BitmapFactory.decodeStream(bufs, padding, null);
                bufs.close();
                chunk = bmp.getNinePatchChunk();
                if (null == chunk){
                    // Inappropriate file if chunk is null.
                    return false;
                }

                OutputStream oInfoFile = null;
            
                oInfoFile = new FileOutputStream(infoFileName);
                oInfoFile.write(padding.left);
                oInfoFile.write(padding.top);
                oInfoFile.write(padding.right);
                oInfoFile.write(padding.bottom);
                oInfoFile.write(chunk.length / 128);
                oInfoFile.write(chunk.length % 128);
                oInfoFile.write(chunk);
                oInfoFile.close();
            } catch(IOException e){
                e.printStackTrace();
                return false;
            }
        }
        return true;

    }

    private void deleteFlickWnnTheme(){
        
        String mFlickWnnTheme = Environment.getExternalStorageDirectory().toString() + "/FlickWnn/keyboard/";
        File mFWTDir = new File(mFlickWnnTheme);
        
        if(mFWTDir.isDirectory()){
            String[] children = mFWTDir.list();
            for(int i = 0; i < children.length; i++){
                File iFile = new File (mFWTDir, children[i]);
                iFile.delete();
            }
        }
    }
}

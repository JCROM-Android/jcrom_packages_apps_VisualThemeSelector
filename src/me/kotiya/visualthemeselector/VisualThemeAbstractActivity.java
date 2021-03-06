package me.kotiya.visualthemeselector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

public abstract class VisualThemeAbstractActivity extends Activity
    implements View.OnClickListener, ActionBar.OnNavigationListener{
		
    protected static final String MY_THEME_PROPERTY = "persist.sys.theme";
    private static final String THEME_LOCK = "persist.sys.theme.lock";
    
	protected int mCurrentPage = 0;		// ViewPagerのデフォルト位置
	private int mCachePageNum = 3;		// ViewPagerでキャッシュに持つページ数
	private ViewPager mViewPager;
	private ViewPagerAdapter mPagerAdapter;
	protected String[] mThemeList;
	protected String mThemePath;
	protected ProgressDialog mProgressDialog;
	private Activity mActivity = this;
	private SpinnerAdapter mSpinnerAdapter = null;
	private ActionBar mActionBar = null;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mActionBar = getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		
		String mExternalStoragePath = Environment.getExternalStorageDirectory().toString();
		StringBuilder builder = new StringBuilder();
		builder.append(mExternalStoragePath);
		builder.append("/mytheme/");
		mThemePath = builder.toString();

		File td = new File(mThemePath);
		
		if(!td.exists()){
			if(!td.mkdir()){
				exitVTS();
				return;
			}
		}
		
		String[] mThemeListTmp = td.list();
		
		List<String> tmpList = new ArrayList<String>(Arrays.asList(mThemeListTmp));
		tmpList.remove(".nomedia");
		mThemeList = (String[])tmpList.toArray(new String[tmpList.size()]);
		Arrays.sort(mThemeList);
		setTitle("");
		
		for(int i = 0; i < mThemeList.length; i++){
			adapter.add(removeFileExtension(mThemeList[i]));
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerAdapter = adapter;
		mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
		
		mPagerAdapter = new ViewPagerAdapter(this);
		
		mPagerAdapter.setList(mThemeList);
		mPagerAdapter.setPath(mThemePath);
		getDisplaySize();
		
		mViewPager = (ViewPager) findViewById(R.id.themepager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(mCachePageNum);
        
        mViewPager.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int arg0) {
				mCurrentPage = arg0;
				mActionBar.setSelectedNavigationItem(mCurrentPage);

			}
        	
        });
	
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		int pagerWidth = findViewById(R.id.themepager).getWidth();
		int pagerHeight = findViewById(R.id.themepager).getHeight();
		
		mPagerAdapter.setSizeofView(pagerWidth, pagerHeight);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
	}
	
    protected void showProgress(int resid) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(resid));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    protected final Runnable closeProgress = new Runnable() {
        @Override
        public void run() {
            SystemProperties.set(THEME_LOCK, "false");
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        }
    };
    
    protected String removeFileExtension(String filename) {
        int lastDotPos = filename.lastIndexOf('.');

        if (lastDotPos == -1) {
            return filename;
        } else if (lastDotPos == 0) {
            return filename;
        } else {
            return filename.substring(0, lastDotPos);
        }
    }
    
    abstract public void selectOnClick(View v);
    
    public void descriptionOnClick(View v){

		StringBuilder builder = new StringBuilder();
		builder.append("/data/data/");
		builder.append(this.getPackageName());
		builder.append("/themethumbs/");
		builder.append(mThemeList[mCurrentPage]);
		String themeZipDir = removeFileExtension(builder.toString());
				
		File themeZipExt = new File(themeZipDir);
		
		String intotext;
		
		String themeName = removeFileExtension(mThemeList[mCurrentPage]);
		String filePath = Environment.getExternalStorageDirectory().toString() + "/.mytheme/" + themeName + "/info.txt";
		File infoFile = new File(filePath);

		if(infoFile.exists()) {
			intotext = infoReader(infoFile);
		}else if(themeZipExt.isDirectory()){
			// Directoryが存在する＝Zipテーマなので該当directoryからinfo.txtを読む
			String themeZipInfoStr = themeZipDir.concat("/info.txt");
			File themeZipInfo = new File(themeZipInfoStr);
			intotext = infoReader(themeZipInfo);
			
		}else{
			// Directoryが存在しない＝ZipテーマではないのでSDからinfo.txtを読む
			StringBuilder builder2 = new StringBuilder();
			builder2.append(mThemePath);
			builder2.append(mThemeList[mCurrentPage]);
			builder2.append("/info.txt");
			String themeInfoStr = builder2.toString();
			
			File themeInfo = new File(themeInfoStr);
			
			intotext = infoReader(themeInfo);
		}
		
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle(R.string.description_dialog_title);
		alertDialog.setMessage(intotext);
		
		alertDialog.setPositiveButton(R.string.description_dialog_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		alertDialog.show();
    }

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mViewPager.setCurrentItem(itemPosition, true);
		return true;
	}
	
	/**
	 * info.txtを読み込んでStringを返す
	 * @param readFile
	 * @return
	 */
	
	private String infoReader(File readFile){
		
		String oString = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(readFile));
			
			StringBuilder oBuilder = new StringBuilder();
			String tmpStr;

			while((tmpStr = br.readLine()) != null){
					oBuilder.append(tmpStr);
					oBuilder.append("\n");
			}
			oString = new String(oBuilder);
			
		} catch (FileNotFoundException e) {
			oString = getString(R.string.info_txt_not_found);
		} catch (IOException e) {
			oString = getString(R.string.info_txt_not_found);
		}
		
		return oString;
		
	}
	
	private void getDisplaySize(){
		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		Point dispSize = new Point();
		disp.getSize(dispSize);
		
		mPagerAdapter.setSizeofView(dispSize.x, dispSize.y);
	}

	abstract public void exitVTS();
}

package me.kotiya.visualthemeselector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.os.SystemProperties;

public class VTSettingsActivity extends VisualThemeAbstractActivity {
    
    private final Runnable closeProgress = new Runnable() {
        @Override
        public void run() {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;

	        Intent intent = new Intent();
	        intent.putExtra("jcrom.new.theme", removeFileExtension(mThemeList[mCurrentPage]));
		setResult(RESULT_OK, intent);
		finish();
            }
        }
    };

	public void selectOnClick(View v) {
		// TODO 自動生成されたメソッド・スタブ
    	
    	DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO 自動生成されたメソッド・スタブ

				boolean performReset = false;
				if(which == DialogInterface.BUTTON_POSITIVE){
					performReset = true;
				}
				
				showProgress(R.string.progress_set_theme);

				SystemProperties.set(MY_THEME_PROPERTY, removeFileExtension(mThemeList[mCurrentPage]));
				new ThemeManager(mActivity).setTheme(removeFileExtension(mThemeList[mCurrentPage]), closeProgress, performReset);
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.set_theme_confirm_reboot);
		builder.setPositiveButton(R.string.set_theme_confirm_yes, listener);
		builder.setNegativeButton(R.string.set_theme_confirm_no, listener);
		builder.show();
        
	}
}

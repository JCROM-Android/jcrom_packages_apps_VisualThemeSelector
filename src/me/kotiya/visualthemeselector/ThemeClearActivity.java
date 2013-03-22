package me.kotiya.visualthemeselector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ThemeClearActivity extends Activity {
	
	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO 自動生成されたメソッド・スタブ
		super.onCreate(savedInstanceState);
		setContentView(R.layout.theme_clear_activity);

		// intentに対する応答
		setResult(RESULT_OK);
		
		// テーマをクリアする
		showProgress(R.string.progress_clear_theme);
		new ThemeManager(this).clearTheme(closeProgress);
		
	}
	
    private void showProgress(int resid) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(resid));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private final Runnable closeProgress = new Runnable() {
        @Override
        public void run() {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;

                finish();
            }
        }
    };

}

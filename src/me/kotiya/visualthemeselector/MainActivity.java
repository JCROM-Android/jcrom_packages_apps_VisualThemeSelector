package me.kotiya.visualthemeselector;

import android.net.Uri;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends VisualThemeAbstractActivity {
	
	static private final int INTENT_SET_THEME = 1;
    
	@Override
	public void selectOnClick(View v) {
		// TODO 自動生成されたメソッド・スタブ
		
    	StringBuilder builder = new StringBuilder();
		builder.append(mThemeList[mCurrentPage]);
		String theme = builder.toString();
    	
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("jcrom:///set_theme"));
		intent.putExtra("jcrom.new.theme", removeFileExtension(theme));
		startActivityForResult(intent, INTENT_SET_THEME);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
	
		if(requestCode == INTENT_SET_THEME){
			switch(resultCode){
			case RESULT_OK:
				break;

			case RESULT_CANCELED:
				break;
			}
		}
	}

	@Override
	public void exitVTS() {
		// TODO 自動生成されたメソッド・スタブ
		Toast.makeText(this, R.string.mytheme_not_found_message, Toast.LENGTH_LONG).show();
		setResult(RESULT_CANCELED);
		finish();
	}
}

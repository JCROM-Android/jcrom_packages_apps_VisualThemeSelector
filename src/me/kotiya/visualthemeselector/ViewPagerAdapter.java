package me.kotiya.visualthemeselector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ViewPagerAdapter extends PagerAdapter {

	private Context mContext;
	private String mThemePath;
	private int THEME_NUMS;
	private String[] mThemeList;
	private String[] decodeEntry = {"thumbnail/thumbnail.png", "wallpaper/home_wallpaper.png", "info.txt"};			// 抽出対象ZipEntry定義
	private String[] extractedFiles = {"thumbnail.png", "home_wallpaper.png", "info.txt"};								// 抽出後のファイル名称定義
	private int mPagerWidth;
	private int mPagerHeight;

	/**
	 * コンストラクタ
	 * @param context
	 */
	public ViewPagerAdapter(final Context context){
		mContext = context;
	}


	@Override
	public int getCount() {
		// TODO 自動生成されたメソッド・スタブ
		return THEME_NUMS;

	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		// TODO 自動生成されたメソッド・スタブ
		return arg0 == (ImageView)arg1;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		// TODO 自動生成されたメソッド・スタブ
		container.removeView((View)object);

	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		// TODO 自動生成されたメソッド・スタブ

		Bitmap tb = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPreferredConfig = Bitmap.Config.RGB_565;

		int scale;

		// mThemeList[position]のテーマからサムネイルを取り出して表示する。
		if(mThemeList[position].endsWith(".zip")){
			// zipテーマの場合
			extractFiles(mThemeList[position]);

			StringBuilder builder = new StringBuilder();
			builder.append("/data/data/");
			builder.append(mContext.getPackageName());
			builder.append("/themethumbs/");
			builder.append(mThemeList[position]);
			int delPos = builder.indexOf(".zip");
			builder.delete(delPos, delPos+4);
			builder.append("/");
			String thumbnailStr = builder.toString();

			File thumbDirectory = new File(thumbnailStr);
			String[] thumbList = thumbDirectory.list();

			for(int i = 0; i < thumbList.length; i++){
				if(thumbList[i].endsWith(".png")){
					builder.append(thumbList[i]);
					String thumbnail = builder.toString();
					BitmapFactory.decodeFile(thumbnail, options);

					scale = Math.max(options.outWidth / mPagerWidth, options.outHeight / mPagerHeight);
					options.inSampleSize = scale;
					options.inJustDecodeBounds = false;

					tb = BitmapFactory.decodeFile(thumbnail, options);

					break;
				}
			}

		}else{
			// テーマフォルダの場合
			StringBuilder builder = new StringBuilder();
			builder.append(mThemePath);
			builder.append(mThemeList[position]);
			builder.append("/thumbnail/thumbnail.png");
			String tbf = builder.toString();

			BitmapFactory.decodeFile(tbf, options);
			scale = Math.max(options.outWidth / mPagerWidth, options.outHeight / mPagerHeight);
			options.inSampleSize = scale;
			options.inJustDecodeBounds = false;

			tb = BitmapFactory.decodeFile(tbf, options);

			if(null == tb){
				StringBuilder builder2 = new StringBuilder();
				builder2.append(mThemePath);
				builder2.append(mThemeList[position]);
				builder2.append("/wallpaper/home_wallpaper.png");
				String tbw = builder2.toString();

				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(tbw, options);
				scale = Math.max(options.outWidth / mPagerWidth, options.outHeight / mPagerHeight);
				options.inSampleSize = scale;
				options.inJustDecodeBounds = false;
				tb = BitmapFactory.decodeFile(tbw, options);
			}
		}

		ImageView iv = new ImageView(mContext);
		iv.setImageBitmap(tb);
		container.addView(iv);
		return iv;
	}

	/**
	 * zipファイルからthumbnail.png, home_wallpaper.png, info.txtを抽出する
	 * @param String zipFileName
	 */
	private void extractFiles(String zipFileName){

		// ターゲットのZipファイル
		StringBuilder zFbuilder = new StringBuilder();
		zFbuilder.append(mThemePath);
		zFbuilder.append(zipFileName);
		File zipfile = new File(zFbuilder.toString());

		// output対象フォルダ
		StringBuilder builder = new StringBuilder();
		builder.append("/data/data/");
		builder.append(mContext.getPackageName());
		builder.append("/themethumbs/");
		builder.append(zipfile.getName());
		int delPos = builder.indexOf(".zip");
		builder.delete(delPos, delPos+4);
		builder.append("/");
		File outDir = new File(builder.toString());

		// thumbnailのファイル名
		long lMtime = zipfile.lastModified();
		StringBuilder oFbuilder = new StringBuilder();
		oFbuilder.append(Long.toString(lMtime));
		oFbuilder.append(".png");
		String thumbName = oFbuilder.toString();

		// outputするフォルダがない場合はフォルダを作成する
		// フォルダが存在する場合は中身のファイル名を確認して一致する場合はファイルを抽出処理しない
		if(!outDir.exists()){
			// フォルダが存在しない
			outDir.mkdirs();
		}else{
			// フォルダが存在する
			// フォルダのファイル一覧を取得して比較する
			String[] children = outDir.list();
			for(int i = 0; i < children.length; i++){
				if(children[i].equals(thumbName)){
					// 一致するものが存在する場合は更新不要なので終了する
					return;
				}
			}
			// 一致するものが存在しない場合はフォルダの中身を全て削除する
			for(int i = 0; i < children.length; i++){
				File iFile = new File(outDir, children[i]);
				iFile.delete();
			}

		}

		// ファイルを抽出する
		for(int i = 0; i < 3; i++){

			boolean thumbstat = checkFile(outDir, thumbName);
			if(thumbstat) continue;

			decodeZip(zipfile, outDir, i);
		}
	}

	/**
	 * 要求されたファイルをZipfileから抽出する
	 * @param zipfile
	 * @param outputDir
	 * @param position
	 */
	private void decodeZip(File zipfile, File outputDir, int position){

		File outFile = new File(outputDir, extractedFiles[position]);
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;

		try {
			ZipFile zf = new ZipFile(zipfile);
			ZipEntry ze = zf.getEntry(decodeEntry[position]);

			if(null == ze) return;

			InputStream is = zf.getInputStream(ze);
			bis = new BufferedInputStream(is);

			bos = new BufferedOutputStream(new FileOutputStream(outFile));

			int ava;
			while((ava = bis.available()) > 0){
				byte[] bs = new byte[ava];

				bis.read(bs);
				bos.write(bs);
			}
			bis.close();
			bos.close();

		} catch (ZipException e) {
			// TODO 自動生成された catch ブロック
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
		}
	}

	/**
	 * 画像ファイルが抽出済かチェックして存在したらrenameする。
	 * @param outDir
	 * @param thumbName
	 * @return
	 */
	private boolean checkFile(File outDir, String thumbName){

		File thumbnail = new File(outDir.getPath().concat("/").concat(thumbName));
		File tn = new File(outDir.getPath().concat("/").concat(extractedFiles[0]));
		if(tn.exists()){
			tn.renameTo(thumbnail);
			return true;
		}

		File wp = new File(outDir.getPath().concat("/").concat(extractedFiles[1]));
		if(wp.exists()){
			wp.renameTo(thumbnail);
			return false;
		}

		return false;

	}

	public void setList(String[] themeList){
		mThemeList = themeList;
		THEME_NUMS = mThemeList.length;
	}

	public void setPath(String themePath){
		mThemePath = themePath;
	}

	public void setSizeofView(int width, int height){
		mPagerWidth = width;
		mPagerHeight = height;		
	}
	
}

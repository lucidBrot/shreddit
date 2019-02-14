package com.lucidbrot.shreddit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HandleTextIntentActivity extends Activity {

	private View rootView;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_handletextintent);
		rootView = findViewById(R.id.rootviewlul);

		Log.d("onCreate", "got to here");

		// Get intent, action and MIME type
		final Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null) {

			if ("text/plain".equals(type)) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						handleSendText(intent); // Handle text being sent
					}
				};
				Thread t = new Thread(runnable);
				t.start();
			}
		}
	}

	private void handleSendText(Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		Log.d("handleSendText", "got to here with url " + sharedText);
		if (sharedText != null) {
			// Update UI to reflect text being shared
			if (URLUtil.isValidUrl(sharedText)) {
				if (isImageUrl(sharedText)) {
					Log.d("handleSendText", "got to here  A");
					// TODO: good case. direct image share
					getImage(sharedText);
				}
				Log.d("handleSendText", "got to anotherwhere C");
				// image is somewhere within the page
				loadImagePageHTML(sharedText);
			}
			else {
				Log.d("handleSendText", "got to there B");
				notImageUrl(sharedText);
			}
		}
	}

	private void loadImagePageHTML(final String url) {
		final WebView webView = rootView.findViewById(R.id.webview);
		webView.post(new Runnable() {
			@Override
			public void run() {

				final Context myApp = HandleTextIntentActivity.this;

				/* An instance of this class will be registered as a JavaScript interface */
				class MyJavaScriptInterface {
					@JavascriptInterface
					@SuppressWarnings("unused")
					public void processHTML(String html) {
						// process the html as needed by the app
						Log.d("js tag","le html: "+html);
						actualllyProcessHTML(html);
					}
				}

				final WebView browser = webView;
				/* JavaScript must be enabled if you want it to work, obviously */
				browser.getSettings().setJavaScriptEnabled(true);

				/* Register a new JavaScript interface called HTMLOUT */
				browser.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

				/* WebViewClient must be set BEFORE calling loadUrl! */
				browser.setWebViewClient(new WebViewClient() {
					@Override
					public void onPageFinished(WebView view, String url) {
						/* This call inject JavaScript into the page which just finished loading. */
						browser.loadUrl(
								"javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
					}
				});

				/* load a web page */
				webView.loadUrl(url);

			}
		});
	}

	private void actualllyProcessHTML(String html) {
		// https://external-preview.redd.it/kmeBt8R6jt4X-Zhio7K8BbzvHhvowyxYy2IrjaVpBMU.jpg?auto=webp&amp;s=17c5a4da7d2d691e376cfee07c6cb17e6716f6ea"/>
		//Pattern pattern = Pattern.compile("https://external-preview.redd.it/(.*?)/");
		Pattern pattern = Pattern.compile("https://external\\-preview\\.redd\\.it(.*?)\"");
		Matcher matcher = pattern.matcher(html);
		if(matcher.find()){
			String url = matcher.group(1);
			Log.d("actualllyProcessHTML", "Url: "+url);
			String actualUrl = "https://external-preview.redd.it"+url.substring(0, url.length()-1).replace("&amp;", "&");
			if (actualUrl.endsWith("&quot;)")){
				actualUrl = actualUrl.substring(0, actualUrl.length()- "&quot;)".length());
			}
			getImage(actualUrl);
		} else {
			Log.d("actualllyProcessHTML","Failerinod to parse");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(rootView.getContext(), "Failed to parse", Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	private void getImage(String sharedText) {
		Log.d("getImage", "got to here, using url: "+sharedText);
		Bitmap image = getBitmapFromURL(sharedText);
		if (image != null) {
			showImage(image);
			shareImage(image);
		}
	}

	private void shareImage(final Bitmap image) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				Uri uri = saveImage(image);
				shareImageUri(uri);
			}
		};
		new Thread(r).start();
	}

	/**
	 * Saves the image as PNG to the app's cache directory.
	 * @param image Bitmap to save.
	 * @return Uri of the saved file or null
	 */
	private Uri saveImage(Bitmap image) {
		//TODO - Should be processed in another thread
		File imagesFolder = new File(getCacheDir(), "images");
		Uri uri = null;
		try {
			imagesFolder.mkdirs();
			File file = new File(imagesFolder, "shared_image.png");

			FileOutputStream stream = new FileOutputStream(file);
			image.compress(Bitmap.CompressFormat.PNG, 90, stream);
			stream.flush();
			stream.close();
			uri = FileProvider.getUriForFile(this, "com.mydomain.fileprovider", file);

		} catch (IOException e) {
			Log.d("saveImage", "IOException while trying to write file for sharing: " + e.getMessage());
		}
		return uri;
	}

	/**
	 * Shares the PNG image from Uri.
	 * @param uri Uri of image to share.
	 */
	private void shareImageUri(Uri uri){
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_STREAM, uri);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.setType("image/png");
		startActivity(intent);
	}

	private void showImage(final Bitmap image) {
		runOnUiThread((new Runnable() {
			@Override
			public void run() {
				((ImageView) findViewById(R.id.imageview)).setImageBitmap(image);
			}
		}));
	}

	public Bitmap getBitmapFromURL(String src) {
		try {
			java.net.URL url = new java.net.URL(src);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			Log.d("getBitmapFromURL", "got image");
			return myBitmap;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void notImageUrl(final String sharedText) {
		final Activity activity = this;
		Log.d("NotImageUrl", "got to here");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(activity, "is not image url: " + sharedText, Toast.LENGTH_SHORT).show();
			}
		});
	}

	private boolean isImageUrl(String url) {
		URLConnection connection = null;
		try {
			connection = new URL(url).openConnection();
		}
		catch (IOException e) {
			e.printStackTrace();
			Log.e("isImageUrl", "Failed to fetch " + url);
		}
		String contentType = connection.getHeaderField("Content-Type");
		boolean isImage = contentType.startsWith("image/");
		return isImage;
	}
}



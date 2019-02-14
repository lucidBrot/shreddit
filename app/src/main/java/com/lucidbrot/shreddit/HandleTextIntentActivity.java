package com.lucidbrot.shreddit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

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
			String actualUrl = "https://external-preview.redd.it"+url;
			getImage(actualUrl);
		} else {
			Log.d("actualllyProcessHTML","Failerinod to parse");
		}
	}

	private void getImage(String sharedText) {
		Log.d("getImage", "got to here, using url: "+sharedText);
		Bitmap image = getBitmapFromURL(sharedText);
		if (image != null) {
			showImage(image);
		}
	}

	private void showImage(Bitmap image) {
		((ImageView) findViewById(R.id.imageview)).setImageBitmap(image);
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



package com.lucidbrot.shreddit;

import android.graphics.Bitmap;

import java.util.Optional;

public class UglyCachingSingleton {
    private static UglyCachingSingleton instance;
    private UglyCachingSingleton(){}

    public static UglyCachingSingleton getInstance(){
        if (instance==null){
            instance = new UglyCachingSingleton();
        }
        return instance;
    }

    private Bitmap latestImage;
    private String latestImageUrl;

    private String latestInitialUrl;
    private String matchingImageUrl;

    public Optional<Bitmap> getCachedBitmapForImageUrl(String url){
        if (latestImageUrl !=null && latestImageUrl.equals(url)){
            return Optional.of(latestImage);
        } else {
            return Optional.empty();
        }
    }

    public Optional<Bitmap> getCachedBitmapForInitialUrl(String initialUrl){
        String imageUrl = getCachedImageUrlForInitialUrl(initialUrl);
        if (imageUrl==null){
            return Optional.empty();
        } else {
            return getCachedBitmapForImageUrl(imageUrl);
        }
    }

    public String getCachedImageUrlForInitialUrl(String initialUrl){
        if (latestInitialUrl != null && latestInitialUrl.equals(initialUrl)){
            return matchingImageUrl;
        } else {
            return null;
        }
    }

    public void setCachedBitmapForImageUrl(String sharedText, Bitmap image) {
        if (sharedText!= null && image!=null) {
            latestImage = image;
            latestImageUrl = sharedText;
        }
    }

    public void setCachedImageUrlForInitialUrl(String initialUrl, String actualUrl) {
        if (initialUrl!=null && actualUrl!=null){
            matchingImageUrl = actualUrl;
            latestInitialUrl = initialUrl;
        }
    }
}

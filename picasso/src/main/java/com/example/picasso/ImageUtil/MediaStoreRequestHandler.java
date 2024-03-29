package com.example.picasso.ImageUtil;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import java.io.IOException;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.provider.MediaStore.Images.Thumbnails.MICRO_KIND;
import static android.provider.MediaStore.Images.Thumbnails.MINI_KIND;
import static com.example.picasso.ImageUtil.MediaStoreRequestHandler.PicassoKind.MICRO;
import static com.example.picasso.ImageUtil.MediaStoreRequestHandler.PicassoKind.MINI;

public class MediaStoreRequestHandler extends RequestHandler {
    private Context context;

    MediaStoreRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        final Uri uri = data.uri;
        return (SCHEME_CONTENT.equals(uri.getScheme())
                && MediaStore.AUTHORITY.equals(uri.getAuthority()));
    }

    @Override
    public Bitmap load(Request request) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        
        //直接加载大图
        if(!request.resize){
            return MediaStore.Images.Media.getBitmap(contentResolver,request.uri);
        }
        
        //加载缩略图
        
        if (!beforeAndroidTen()) {
            return contentResolver.loadThumbnail(request.uri, new Size(request.targetWidth,
                    request.targetHeight), null);
        }

        PicassoKind picassoKind = getPicassoKind(request.targetWidth, request.targetHeight);
        long id = ContentUris.parseId(request.uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        calculateInSampleSize(request.targetWidth, request.targetHeight, picassoKind.width,
                picassoKind.height, options);

        return MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id, picassoKind.androidKind,
                options);
    }

    static PicassoKind getPicassoKind(int targetWidth, int targetHeight) {
        if (targetWidth <= MICRO.width && targetHeight <= MICRO.height) {
            return MICRO;
        } else {
            return MINI;
        }
    }

    /**
     * 判断版本是否在Android 10 之前
     */
    private static boolean beforeAndroidTen() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

    enum PicassoKind {
        MICRO(MICRO_KIND, 96, 96),
        MINI(MINI_KIND, 512, 384);

        final int androidKind;
        final int width;
        final int height;

        PicassoKind(int androidKind, int width, int height) {
            this.androidKind = androidKind;
            this.width = width;
            this.height = height;
        }
    }
}

package com.kronos.sutd;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class photoCaptured {
    private static final photoCaptured ourInstance = new photoCaptured();
    private String imgPath;
    private Context context;

    static photoCaptured getInstance() {
        return ourInstance;
    }

    private photoCaptured() {}

    public String getImgPath(){
        return imgPath;
    }

    public void setImgPath(String path){
        imgPath = path;
    }

    public void setContext(Context c){context=c;}

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "KRONOS_" + timeStamp;
        File rootPath = context.getExternalFilesDir("Pictures");
        File image = new File(rootPath.getAbsolutePath()+"/"+imageFileName+".jpg");
        // Save a file: path for use with ACTION_VIEW intents
        imgPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public Bitmap processThumbnail(Bitmap myBitmap, String path) throws IOException {
        ExifInterface ei = new ExifInterface(path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap;
        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = this.rotateImage(myBitmap, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = this.rotateImage(myBitmap, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = this.rotateImage(myBitmap, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                rotatedBitmap = myBitmap;
        }
        return rotatedBitmap;
    }
}


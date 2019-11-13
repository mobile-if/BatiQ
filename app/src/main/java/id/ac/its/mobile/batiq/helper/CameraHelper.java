package id.ac.its.mobile.batiq.helper;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraHelper {

    private static CameraHelper INSTANCE;

    WeakReference<Context> context;
    Listener listener;

    public static CameraHelper with(Activity activity){
        if (INSTANCE == null)
            INSTANCE = new CameraHelper();
        INSTANCE.context = new WeakReference<>((Context) activity);
        return INSTANCE;
    }

    private CameraHelper() {}

    public CameraHelper setListener(Listener listener) {
        this.listener = listener;
        return INSTANCE;
    }

    public void takePicture(){
        if (context.get() == null)
            return;

        Intent intent = new Intent(context.get(), ActivityHelper.class);
        if (context.get() instanceof Application)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.get().startActivity(intent);
    }

    private static void onPictureTaken(String filepath, Bitmap bmp){
        if (INSTANCE == null)
            return;

        if (INSTANCE.listener != null)
            INSTANCE.listener.onImageCaptured(filepath, bmp);
    }

    private static void onPermissionGranted(){
        if (INSTANCE == null)
            return;

        if (INSTANCE.listener != null)
            INSTANCE.listener.onPermissionGranted();
    }

    private static void onPermissionDenied(){
        if (INSTANCE == null)
            return;

        if (INSTANCE.listener != null)
            INSTANCE.listener.onPermissionDenied();
    }

    public static class ActivityHelper extends AppCompatActivity {
        private static final int REQ_CODE = 143;

        File fileFoto = null;
        ImageHelper.CompressResize taskCompressResize;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setTitle("");
            takePicture();
        }

        private void takePicture(){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_CODE);
                }
                else
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQ_CODE);
            }
            else {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    try {
                        fileFoto = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }

                    if (fileFoto != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "id.ac.its.mobile.batiq.fileprovider",
                                fileFoto);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                        for (ResolveInfo resolveInfo : resInfoList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        startActivityForResult(takePictureIntent, REQ_CODE);
                    }
                }
            }
        }

        private boolean deleteImage(){
            if (fileFoto != null && fileFoto.getAbsolutePath()!=null) {
                try {
                    boolean status = fileFoto.delete();
                    fileFoto = null;
                    return status;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onDestroy() {
            if (taskCompressResize != null)
                taskCompressResize.cancel(true);
            super.onDestroy();
        }

        private File createImageFile() throws IOException {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
            String imageFileName = timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        }
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                //Bundle extras = data.getExtras();
                //Bitmap imageBitmap = (Bitmap) extras.get("data");
                //previewImgView.setImageBitmap(imageBitmap);
                if (fileFoto!=null && fileFoto.getAbsolutePath() != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(fileFoto.getAbsolutePath(), options);
                    int imageHeight = options.outHeight;
                    int imageWidth = options.outWidth;
                    if (taskCompressResize != null)
                        taskCompressResize.cancel(true);
                    taskCompressResize = new ImageHelper.CompressResize(this);
                    taskCompressResize.execute(new ImageHelper.CompressResize.Parameter(fileFoto.getAbsolutePath(),
                            20, imageWidth, imageHeight, new ImageHelper.CompressResize.CompressListener() {
                        @Override
                        public void afterCompressed(Bitmap img) {
                            CameraHelper.onPictureTaken(fileFoto.getAbsolutePath(), img);
                            finish();
                        }
                    }));
                }
            }
            else {
                deleteImage();
                finish();
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            switch (requestCode){
                case REQ_CODE:
                    for (int i=0;i<grantResults.length;i++){
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            CameraHelper.onPermissionDenied();
                            finish();
                            return;
                        }
                    }
                    CameraHelper.onPermissionGranted();
                    takePicture();
                    break;
            }
        }
    }

    public interface Listener {
        void onPermissionGranted();
        void onPermissionDenied();
        void onImageCaptured(String filepath, Bitmap bmp);
    }
}

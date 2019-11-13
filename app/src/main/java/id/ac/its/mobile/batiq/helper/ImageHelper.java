package id.ac.its.mobile.batiq.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ImageHelper {
    public static final String BACKUP_IMAGE_DIRECTORY = "BatiQ";

    public static Bitmap filepathToBitmap(String filepath){
        if (filepath == null)
            return null;
        try {
            File imgFile = new File(filepath);
            if (imgFile.exists()) {
                return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            }
        } catch (Exception e){

        }
        return null;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
    public static boolean bitmapToFile(Bitmap source, File file, Bitmap.CompressFormat format, int quality){
        OutputStream os;
        boolean success = false;
        try {
            os = new FileOutputStream(file);
            success = source.compress(format, quality, os);
            os.flush();
            os.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return success;
    }
    public static boolean compress(String filePath, int quality){
        OutputStream os;
        try {
            Bitmap img = BitmapFactory.decodeFile(filePath);
            os = new FileOutputStream(filePath);
            img.compress(Bitmap.CompressFormat.JPEG, quality, os);
            os.flush();
            os.close();
            img.recycle();
            return true;
        }catch (Exception e){
            Log.e("Image Helper", "Error compress: " + e);
            return false;
        }
    }

    public static Bitmap resize(String filePath, int width, int height){
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, bfo);

        int photoWidth = bfo.outWidth;
        int photoHeight = bfo.outHeight;

        Log.d("Image Helper", "resize: width="+photoWidth+" height="+photoHeight+" mimeType="+bfo.outMimeType);


        int scaleFactor = 1;
        if ((width > 0) || (height > 0)) {
            scaleFactor = Math.min(photoWidth / width, photoHeight / height);
        }

        bfo.inJustDecodeBounds = false;
        bfo.inSampleSize = scaleFactor;
        bfo.inPurgeable = true;

        Log.d("Image Helper", "resize: scalefactor"+scaleFactor);

        return BitmapFactory.decodeFile(filePath, bfo);
    }

    public static class CompressResize extends AsyncTask<CompressResize.Parameter, Void, CompressResize.Response>{
        private WeakReference<Context> context;
        private SweetAlertDialog dialog;

        public CompressResize(Context context) {
            this.context = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (context.get() != null) {
                dialog = new SweetAlertDialog(context.get(), SweetAlertDialog.PROGRESS_TYPE);
                dialog.setCancelable(false);
                dialog.setTitleText("Sedang mengkompres foto");
                dialog.show();
            }
        }

        @Override
        protected Response doInBackground(Parameter... parameters) {
            Parameter param = parameters[0];
            if (param == null || param.getPathFoto() == null || param.getQuality() == null || param.getWidth() == null || param.getHeight() == null )
                return null;

            try {
                ExifInterface ei = new ExifInterface(param.getPathFoto());
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                Bitmap bitmap = BitmapFactory.decodeFile(param.getPathFoto());
                OutputStream os = new FileOutputStream(param.getPathFoto());

                Bitmap rotatedBitmap = null;
                switch(orientation) {

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotatedBitmap = rotateImage(bitmap, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotatedBitmap = rotateImage(bitmap, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotatedBitmap = rotateImage(bitmap, 270);
                        break;
                    case ExifInterface.ORIENTATION_NORMAL:
                    default:
                        rotatedBitmap = bitmap;
                }
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.flush();
                os.close();
                bitmap.recycle();
                rotatedBitmap.recycle();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }

            ImageHelper.compress(param.getPathFoto(), param.getQuality());
            Bitmap img = ImageHelper.resize(param.getPathFoto(), param.getWidth(), param.getHeight());

            //Backup image to sdcard
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                try {
                    File original = new File(param.getPathFoto());
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), BACKUP_IMAGE_DIRECTORY+File.separator);
                    if(!(directory.exists() && directory.isDirectory()) && !directory.mkdirs())
                        //Delete when there is a file with the same name
                        if(!(directory.delete() && directory.mkdirs()))
                            throw new Exception("Folder tidak terbuat!");
                    File imgFile = new File(directory, original.getName());

                    InputStream in = new FileInputStream(original);
                    OutputStream out = new FileOutputStream(imgFile);
                    try {
                        // Transfer bytes from in to out
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    } finally {
                        out.close();
                        in.close();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            return new Response(img, param.listener);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (context.get() == null && dialog!=null)
                dialog.dismiss();
        }

        @Override
        protected void onPostExecute(Response response) {
            if (dialog!=null && dialog.isShowing())
                dialog.dismiss();

            if (response == null)
                return;

            if (response.listener != null && response.resImg != null)
                response.listener.afterCompressed(response.resImg);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (dialog!=null && dialog.isShowing())
                dialog.dismiss();
        }

        public static class Parameter{
            private String pathFoto;
            private Integer quality;
            private Integer width;
            private Integer height;
            private CompressListener listener;

            public Parameter() {
            }

            public Parameter(String pathFoto, Integer quality, Integer width, Integer height, CompressListener listener) {
                this.pathFoto = pathFoto;
                this.quality = quality;
                this.width = width;
                this.height = height;
                this.listener = listener;
            }

            public Integer getWidth() {
                return width;
            }

            public void setWidth(Integer width) {
                this.width = width;
            }

            public Integer getHeight() {
                return height;
            }

            public void setHeight(Integer height) {
                this.height = height;
            }

            public String getPathFoto() {
                return pathFoto;
            }

            public void setPathFoto(String pathFoto) {
                this.pathFoto = pathFoto;
            }

            public Integer getQuality() {
                return quality;
            }

            public void setQuality(Integer quality) {
                this.quality = quality;
            }

            public CompressListener getListener() {
                return listener;
            }

            public void setListener(CompressListener listener) {
                this.listener = listener;
            }

        }
        private class Response{
            private Bitmap resImg;
            private CompressListener listener;

            public Response(Bitmap resImg, CompressListener listener) {
                this.resImg = resImg;
                this.listener = listener;
            }
        }
        public interface CompressListener{
            void afterCompressed(Bitmap img);
        }
    }
}

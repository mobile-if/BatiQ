package id.ac.its.mobile.batiq;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;

import id.ac.its.mobile.batiq.helper.BaseActivity;
import id.ac.its.mobile.batiq.helper.CameraHelper;
import id.ac.its.mobile.batiq.model.Dataset;
import id.ac.its.mobile.batiq.model.ErrorResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    ImageView ivPreview;
    Button btnTakePhoto;
    API api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = getRetrofit().create(API.class);

        ivPreview = findViewById(R.id.ivPreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        btnTakePhoto.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnTakePhoto:
                CameraHelper.with(this).setListener(new CameraHelper.Listener() {
                    @Override
                    public void onPermissionGranted() {

                    }

                    @Override
                    public void onPermissionDenied() {
                        showToast("Permission Denied");
                    }

                    @Override
                    public void onImageCaptured(String filepath, Bitmap bmp) {
                        ivPreview.setImageBitmap(bmp);

                        String img_b64 = "data:image/jpeg;base64," + encodeToBase64(bmp, Bitmap.CompressFormat.JPEG, 100);

                        showProgress("Sedang mengirim...", null);
                        api.store("kelompok_muhajir", "batik1", img_b64).enqueue(new Callback<Dataset>() {
                            @Override
                            public void onResponse(Call<Dataset> call, Response<Dataset> response) {
                                hideProgress();
                                if (response.isSuccessful()){
                                    showInfo("Terkirim!", null);
                                } else {
                                    try {
                                        showError("Terjadi masalah!", new Gson().fromJson(response.errorBody().string(),
                                                ErrorResponse.class).getMessage());
                                    } catch (Exception e) {
                                        showError("Terjadi masalah!", null);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<Dataset> call, Throwable t) {
                                hideProgress();
                                showError("Gagal terhubung ke server!", null);
                            }
                        });

                    }
                }).takePicture();
                break;
        }
    }

    private static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }
}

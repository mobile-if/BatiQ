package id.ac.its.mobile.batiq;

import java.util.ArrayList;

import id.ac.its.mobile.batiq.model.Dataset;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface API {

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("api/dataset/{kelompok}/{label}")
    Call<Dataset> store(@Path("kelompok") String kelompok,
                        @Path("label") String label,
                        @Field("image") String image);
}

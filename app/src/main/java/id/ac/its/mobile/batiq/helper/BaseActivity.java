package id.ac.its.mobile.batiq.helper;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import cn.pedant.SweetAlert.SweetAlertDialog;
import id.ac.its.mobile.batiq.Server;
import retrofit2.Retrofit;

public class BaseActivity extends AppCompatActivity {
    private SweetAlertDialog dialogProgress, dialogWarning, dialogError, dialogInfo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Retrofit retrofit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        retrofit = Server.getInstance();
    }

    @Override
    protected void onDestroy() {
        hideError();
        hideWarning();
        hideProgress();
        hideInfo();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    public void setTitle(String title){
        if (getSupportActionBar()!=null)
            getSupportActionBar().setTitle(title);
    }
    public void enableBackButton(){
        if (getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void setSwipeRefresher(int id, final SwipeRefreshLayout.OnRefreshListener onRefreshListener){
        swipeRefreshLayout = findViewById(id);
        if (swipeRefreshLayout!=null)
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (onRefreshListener!=null)
                        onRefreshListener.onRefresh();
                }
            });
    }

    public void setSwipeRefresher(final SwipeRefreshLayout.OnRefreshListener onRefreshListener){
        ViewGroup root = findViewById(android.R.id.content);
        ViewGroup child = (ViewGroup) root.getChildAt(0);

        ((ViewGroup) child.getParent()).removeView(child);
        swipeRefreshLayout = new SwipeRefreshLayout(this);
        swipeRefreshLayout.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        swipeRefreshLayout.addView(child);
        root.removeAllViews();
        root.addView(swipeRefreshLayout);

        if (swipeRefreshLayout!=null)
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (onRefreshListener!=null)
                        onRefreshListener.onRefresh();
                }
            });
    }

    public void disableSwipeRefresher(int id){
        swipeRefreshLayout = findViewById(id);
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setEnabled(false);
    }

    public void setRefreshing(boolean refreshing){
        if (swipeRefreshLayout!=null)
            swipeRefreshLayout.setRefreshing(refreshing);
    }

    public void showProgress(String titleText, @Nullable String contentText){
        hideProgress();
        dialogProgress = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialogProgress.setTitleText(titleText);
        dialogProgress.setCancelable(false);
        if (contentText!=null)
            dialogProgress.setContentText(contentText);
        dialogProgress.show();
    }

    public void setProgressIndicator(@Nullable String titleText, @Nullable String contentText){
        if (dialogProgress!=null && dialogProgress.isShowing()){
            if (titleText != null)
                dialogProgress.setTitle(titleText);
            if (contentText != null)
                dialogProgress.setContentText(contentText);
        }
    }

    public void hideProgress(){
        if (dialogProgress!=null && dialogProgress.isShowing())
            dialogProgress.dismiss();
    }

    public void showWarning(String titleText, @Nullable String contentText){
        hideWarning();
        dialogWarning = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        dialogWarning.setTitleText(titleText);
        if (contentText!=null)
            dialogWarning.setContentText(contentText);
        dialogWarning.show();
    }

    public void hideWarning(){
        if (dialogWarning!=null && dialogWarning.isShowing())
            dialogWarning.dismiss();
    }

    public void showInfo(String titleText, @Nullable String contentText){
        hideInfo();
        dialogInfo = new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE);
        dialogInfo.setTitleText(titleText);
        if (contentText!=null)
            dialogInfo.setContentText(contentText);
        dialogInfo.show();
    }

    public void hideInfo(){
        if (dialogInfo!=null && dialogInfo.isShowing())
            dialogInfo.dismiss();
    }

    public void showError(String titleText, @Nullable String contentText){
        hideError();
        dialogError = new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE);
        dialogError.setTitleText(titleText);
        if (contentText!=null)
            dialogError.setContentText(contentText);
        dialogError.show();
    }

    public void hideError(){
        if (dialogError!=null && dialogError.isShowing())
            dialogError.dismiss();
    }

    public void showToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}

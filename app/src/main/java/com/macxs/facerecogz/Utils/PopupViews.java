package com.macxs.facerecogz.Utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.macxs.facerecogz.databinding.BottomSheetBaseLoaderBinding;
import com.macxs.facerecogz.databinding.BottomSheetErrorBinding;
import com.macxs.facerecogz.databinding.LoadingFacePopupBinding;
import com.sdsmdg.tastytoast.TastyToast;

public class PopupViews {
    BottomSheetBaseLoaderBinding loaderBinding;
    LoadingFacePopupBinding loadingFacePopupBinding;
    BottomSheetErrorBinding errorBinding;
    BottomSheetDialog bottomSheetDialog;
    Activity ctx;

    public PopupViews(Activity ctx) {
        this.ctx = ctx;
        bottomSheetDialog = new BottomSheetDialog(ctx);
        loaderBinding = BottomSheetBaseLoaderBinding.inflate(ctx.getLayoutInflater());
        errorBinding = BottomSheetErrorBinding.inflate(ctx.getLayoutInflater());
        loadingFacePopupBinding = LoadingFacePopupBinding.inflate(ctx.getLayoutInflater());
    }
    public void toastDefault(String msg){
        TastyToast.makeText(ctx, msg, TastyToast.LENGTH_LONG, TastyToast.DEFAULT);
    }
    public void toastWarn(String msg){
        TastyToast.makeText(ctx, msg, TastyToast.LENGTH_LONG, TastyToast.WARNING);
    }
    public void toastSuccess(String msg){
        TastyToast.makeText(ctx, msg, TastyToast.LENGTH_LONG, TastyToast.SUCCESS);
    }
    public void toastError(String msg){
        TastyToast.makeText(ctx, msg, TastyToast.LENGTH_LONG, TastyToast.ERROR);
    }
    public void toastConfused(String msg){
        TastyToast.makeText(ctx, msg, TastyToast.LENGTH_LONG, TastyToast.CONFUSING);
    }

    public void showLoading(String msg) {
        bottomSheetDialog.setContentView(loaderBinding.getRoot());
//        setupFullHeightBottomSheet(bottomSheetDialog, loaderBinding.getRoot());
        loaderBinding.desc.setText(msg);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.show();
    }
    public void showFaceLoading(String msg) {
        bottomSheetDialog.setContentView(loadingFacePopupBinding.getRoot());
        setupFullHeightBottomSheet(bottomSheetDialog, loadingFacePopupBinding.getRoot());
        loadingFacePopupBinding.desc.setText(msg);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.show();
    }
    public void hideFaceLoading() {
        bottomSheetDialog.dismiss();
    }
    public void showError(String msg) {
        bottomSheetDialog.setContentView(errorBinding.getRoot());
//        setupFullHeightBottomSheet(bottomSheetDialog, loaderBinding.getRoot());
        errorBinding.msg.setText(msg);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.show();
    }
    public void hideLoading(){
        bottomSheetDialog.dismiss();
    }public void hideError(){
        bottomSheetDialog.dismiss();
    }

    private void setupFullHeightBottomSheet(BottomSheetDialog bottomSheetDialog, View contentView) {
        FrameLayout bottomSheet = (FrameLayout) contentView.getParent();

        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        if (layoutParams != null) {
            layoutParams.height = height;
        }
        bottomSheet.setLayoutParams(layoutParams);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.setOnShowListener(dialog -> {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        });
    }
}

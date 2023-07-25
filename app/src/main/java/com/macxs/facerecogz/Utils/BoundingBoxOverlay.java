package com.macxs.facerecogz.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;

import androidx.camera.core.CameraSelector;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;


public class BoundingBoxOverlay extends SurfaceView {
    Paint box = new Paint();
    Paint circle = new Paint();
    Context context;
    AttributeSet attributeSet;
    int frameheight;
    int framewidth;
    float xfactor;
    float yfactor;
    Matrix output2OverlayTransform = new Matrix();
    boolean addPostScaleTransform = false;
    RectF boundingBox = null;
    private int cameraFacing = CameraSelector.LENS_FACING_FRONT;
    private boolean areDimsInit = false;
    Face face;

    public BoundingBoxOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attributeSet = attrs;
        box.setColor(Color.CYAN);
        box.setStyle(Paint.Style.STROKE);
        circle.setColor(Color.RED);
        circle.setStyle(Paint.Style.FILL);
    }

    public void setheight(int frameheight) {
        this.frameheight = frameheight;
    }

    public void setFace(Face face) {
        this.face = face;
    }

    public void setWidth(int framewidth) {
        this.framewidth = framewidth;
    }

    public void setCameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
        areDimsInit = false;
        boundingBox = null;
        output2OverlayTransform.reset();

    }

    public boolean dimsinit() {
        return areDimsInit;
    }

    public void setFaceBBox(RectF bbox) {
        this.boundingBox = bbox;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (boundingBox != null) {
            float basePosX = 1f;
            float basePosY = 1f;
            if (!areDimsInit) {
                float viewWidth = (float) getWidth();
                float viewHeight = (float) getHeight();
                xfactor = viewWidth / (float) framewidth;
                yfactor = viewHeight / (float) frameheight;
                basePosX = xfactor;
                basePosY = yfactor;
                output2OverlayTransform.preScale(xfactor, yfactor);
                if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
                    output2OverlayTransform.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f);
                }
                areDimsInit = true;
            } else {
                RectF rect = new RectF(boundingBox);
                output2OverlayTransform.mapRect(rect);
                canvas.drawRoundRect(rect, 16f, 16f, box);
                Log.e("boundbox", " box" + String.valueOf(rect));
            }



        }

    }

    private void drawCircle() {

    }

}

package com.github.florent37.materialviewpager.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.github.florent37.materialviewpager.R;

/**
 * Created by Christophe on 12/07/2014
 */

public class MaskableFrameLayout extends FrameLayout {


    private int mMaskLength;
    private int mMaskPadding = 0;

    ShapeDrawable.ShaderFactory leftShader = new ShapeDrawable.ShaderFactory() {
        @Override
        public Shader resize(int width, int height) {
            return new LinearGradient(0, 0, mMaskLength, 0,
                    new int[]{Color.TRANSPARENT, Color.BLACK},
                    new float[]{0, 1},
                    Shader.TileMode.CLAMP);
        }
    };

    ShapeDrawable.ShaderFactory rightShader = new ShapeDrawable.ShaderFactory() {
        @Override
        public Shader resize(int width, int height) {
            return new LinearGradient(0, 0, mMaskLength, 0,
                    new int[]{Color.BLACK, Color.TRANSPARENT},
                    new float[]{0, 1},
                    Shader.TileMode.CLAMP);
        }
    };

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    //Constants
    private static final String TAG = "MaskableFrameLayout";

    private static final int MODE_ADD = 0;
    private static final int MODE_CLEAR = 1;
    private static final int MODE_DARKEN = 2;
    private static final int MODE_DST = 3;
    private static final int MODE_DST_ATOP = 4;
    private static final int MODE_DST_IN = 5;
    private static final int MODE_DST_OUT = 6;
    private static final int MODE_DST_OVER = 7;
    private static final int MODE_LIGHTEN = 8;
    private static final int MODE_MULTIPLY = 9;
    private static final int MODE_OVERLAY = 10;
    private static final int MODE_SCREEN = 11;
    private static final int MODE_SRC = 12;
    private static final int MODE_SRC_ATOP = 13;
    private static final int MODE_SRC_IN = 14;
    private static final int MODE_SRC_OUT = 15;
    private static final int MODE_SRC_OVER = 16;
    private static final int MODE_XOR = 17;

    private Handler mHandler;

    //Mask props
    @Nullable
    private Drawable mLeftDrawableMask = null;
    @Nullable
    private Drawable mRightDrawableMask = null;
    @Nullable
    private Drawable mPaddingDrawablwMask = null;

    @Nullable
    private Bitmap mFinalPaddingMask = null;
    @Nullable
    private Bitmap mFinalLeftMask = null;
    @Nullable
    private Bitmap mFinalRightMask = null;

    //Drawing props
    private Paint mPaint = null;
    private PorterDuffXfermode mPorterDuffXferMode = null;

    public MaskableFrameLayout(Context context) {
        super(context);
    }

    public MaskableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        construct(context, attrs);
    }

    public MaskableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        construct(context, attrs);
    }

    private void construct(Context context, AttributeSet attrs) {
        mHandler = new Handler();
        setDrawingCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null); //Only works for software layers
        }
        mPaint = createPaint();
        Resources.Theme theme = context.getTheme();
        if (theme != null) {
            TypedArray a = theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.MaskableLayout,
                    0, 0);
            try {
                //Load the mask if specified in xml
                mMaskLength = a.getDimensionPixelOffset(R.styleable.MaskableLayout_maskLength, 0);

                initMasks();
                //Load the mode if specified in xml
                mPorterDuffXferMode = getModeFromInteger(
                        a.getInteger(R.styleable.MaskableLayout_porterduffxfermode, 0));

            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        } else {
            log("Couldn't load theme, mask in xml won't be loaded.");
        }
        registerMeasure();
    }

    @NonNull
    private Paint createPaint() {
        Paint output = new Paint(Paint.ANTI_ALIAS_FLAG);
        output.setXfermode(mPorterDuffXferMode);
        return output;
    }

    //Mask functions
    @Nullable
    private Drawable loadMask(ShapeDrawable.ShaderFactory shader) {
        PaintDrawable p = new PaintDrawable();
        p.setShape(new RectShape());
        p.setShaderFactory(shader);
        return p;
    }

    //Mask functions
    @Nullable
    private Drawable loadMask() {
        ShapeDrawable p = new ShapeDrawable(new RectShape());
        p.getPaint().setColor(0x00000000);
        return p;
    }

    public void setMaskPadding(int maskPadding) {
        mMaskPadding = maskPadding;
        invalidate();
    }

    private void initMasks() {
        Drawable leftDrawable = loadMask(leftShader);
        Drawable rightDrawable = loadMask(rightShader);
        Drawable paddingDrawable = loadMask();

        if (leftDrawable != null && rightDrawable != null && paddingDrawable != null) {
            mLeftDrawableMask = leftDrawable;
            mRightDrawableMask = rightDrawable;
            mPaddingDrawablwMask = paddingDrawable;
        } else {
            log("Are you sure you don't want to provide a mask ?");
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        int max = getWidth() - mMaskPadding;
        return (x < mMaskPadding || x > max || super.onInterceptTouchEvent(ev));
    }

    @Nullable
    private Bitmap makeBitmapMask(@Nullable Drawable drawable) {
        if (drawable != null) {
            if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                Bitmap mask = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mask);
                drawable.setCallback(null);
                drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                drawable.draw(canvas);
                return mask;
            } else {
                log("Can't create a mask with height 0 or width 0. Or the layout has no children and is wrap content");
                return null;
            }
        } else {
            log("No bitmap mask loaded, view will NOT be masked !");
        }
        return null;
    }

    //Once the size has changed we need to remake the mask.
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setSize(w, h);
    }

    private void setSize(int width, int height) {
        if (width > 0 && height > 0) {
            if (mLeftDrawableMask != null) {
                //Remake the 9patch
                swapBitmapMask();
            }
        } else {
            log("Width and height must be higher than 0");
        }
    }

    //Drawing
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mFinalLeftMask != null && mPaint != null) {
            mPaint.setXfermode(mPorterDuffXferMode);
            Rect r = new Rect(0, 0, mMaskPadding + 1, canvas.getHeight());
            canvas.drawBitmap(mFinalPaddingMask, null, r, mPaint);
            canvas.drawBitmap(mFinalLeftMask, mMaskPadding, 0.0f, mPaint);
            canvas.drawBitmap(mFinalRightMask, canvas.getWidth() - mMaskPadding - mMaskLength, 0.0f, mPaint);
            mPaint.setXfermode(null);
        } else {
            log("Mask or paint is null ...");
        }
    }

    //Once inflated we have no height or width for the mask. Wait for the layout.
    private void registerMeasure() {
        final ViewTreeObserver treeObserver = MaskableFrameLayout.this.getViewTreeObserver();
        if (treeObserver != null && treeObserver.isAlive()) {
            treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ViewTreeObserver aliveObserver = treeObserver;
                    if (!aliveObserver.isAlive()) {
                        aliveObserver = MaskableFrameLayout.this.getViewTreeObserver();
                    }
                    if (aliveObserver != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            aliveObserver.removeOnGlobalLayoutListener(this);
                        } else {
                            aliveObserver.removeGlobalOnLayoutListener(this);
                        }
                    } else {
                        log("GlobalLayoutListener not removed as ViewTreeObserver is not valid");
                    }
                    swapBitmapMask();
                }
            });
        }
    }

    //Logging
    private void log(@NonNull String message) {
//        Log.d(TAG, message);
    }

    //Animation
    @Override
    public void invalidateDrawable(Drawable dr) {
        if (dr != null) {
//            initMasks();
//            swapBitmapMask();
            invalidate();
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (who != null && what != null) {
            mHandler.postAtTime(what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (who != null && what != null) {
            mHandler.removeCallbacks(what);
        }
    }

    private void swapBitmapMask() {
        Bitmap newLeftMask = makeBitmapMask(mLeftDrawableMask);
        Bitmap newRightMask = makeBitmapMask(mRightDrawableMask);
        Bitmap newPaddingMask = makeBitmapMask(mPaddingDrawablwMask);


        if (newLeftMask != null) {
            if (mFinalLeftMask != null && !mFinalLeftMask.isRecycled()) {
                mFinalLeftMask.recycle();
            }
            mFinalLeftMask = newLeftMask;
        }

        if (newRightMask != null) {
            if (mFinalRightMask != null && !mFinalRightMask.isRecycled()) {
                mFinalRightMask.recycle();
            }
            mFinalRightMask = newRightMask;
        }
        if (newPaddingMask != null) {
            if (mFinalPaddingMask != null && !mFinalPaddingMask.isRecycled()) {
                mFinalPaddingMask.recycle();
            }
            mFinalPaddingMask = newPaddingMask;
        }


    }

    //Utils
    private PorterDuffXfermode getModeFromInteger(int index) {
        PorterDuff.Mode mode = null;
        switch (index) {
            case MODE_ADD:
                if (Build.VERSION.SDK_INT >= 11) {
                    mode = PorterDuff.Mode.ADD;
                } else {
                    log("MODE_ADD is not supported on api lvl " + Build.VERSION.SDK_INT);
                }
            case MODE_CLEAR:
                mode = PorterDuff.Mode.CLEAR;
                break;
            case MODE_DARKEN:
                mode = PorterDuff.Mode.DARKEN;
                break;
            case MODE_DST:
                mode = PorterDuff.Mode.DST;
                break;
            case MODE_DST_ATOP:
                mode = PorterDuff.Mode.DST_ATOP;
                break;
            case MODE_DST_IN:
                mode = PorterDuff.Mode.DST_IN;
                break;
            case MODE_DST_OUT:
                mode = PorterDuff.Mode.DST_OUT;
                break;
            case MODE_DST_OVER:
                mode = PorterDuff.Mode.DST_OVER;
                break;
            case MODE_LIGHTEN:
                mode = PorterDuff.Mode.LIGHTEN;
                break;
            case MODE_MULTIPLY:
                mode = PorterDuff.Mode.MULTIPLY;
                break;
            case MODE_OVERLAY:
                if (Build.VERSION.SDK_INT >= 11) {
                    mode = PorterDuff.Mode.OVERLAY;
                } else {
                    log("MODE_OVERLAY is not supported on api lvl " + Build.VERSION.SDK_INT);
                }
            case MODE_SCREEN:
                mode = PorterDuff.Mode.SCREEN;
                break;
            case MODE_SRC:
                mode = PorterDuff.Mode.SRC;
                break;
            case MODE_SRC_ATOP:
                mode = PorterDuff.Mode.SRC_ATOP;
                break;
            case MODE_SRC_IN:
                mode = PorterDuff.Mode.SRC_IN;
                break;
            case MODE_SRC_OUT:
                mode = PorterDuff.Mode.SRC_OUT;
                break;
            case MODE_SRC_OVER:
                mode = PorterDuff.Mode.SRC_OVER;
                break;
            case MODE_XOR:
                mode = PorterDuff.Mode.XOR;
                break;
            default:
                mode = PorterDuff.Mode.DST_IN;
        }
        log("Mode is " + mode.toString());
        return new PorterDuffXfermode(mode);
    }

}
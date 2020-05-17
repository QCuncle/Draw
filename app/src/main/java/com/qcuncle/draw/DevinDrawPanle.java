
package com.qcuncle.draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.RequiresApi;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 自定义绘图面板
 * Created by QC uncle on 2020/4/1.
 */

public class DevinDrawPanle extends View {
    public Canvas mCanvas;//画布
    private Paint mPaint;//画笔
    private Paint mBitmapPaint;//专门绘制图片的画笔;
    private Path mPath;//手绘路径
    private Bitmap mBitmap ;//图片
    private Bitmap mTempBitmap;//临时图片
    private String str;//将图片base64转化为字符串（用于暂存）
    private int shapeStyle = 0;//形状
    private int mWidth, mHeight;//宽高
    private float drawX, drawY;//绘制点
    //绘制幅度，当一个方向上触摸移动等于这个距离就绘制一条线,quadTo是绘制贝塞尔曲线
    private final static int TOUCH_TOLERANCE = 4;

    public DevinDrawPanle(Context context) {
        super(context);
        initView();
    }

    public DevinDrawPanle(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DevinDrawPanle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {
        initPaints();//初始化画笔
        mPath = new Path();//初始化绘图路径
    }

    /**
     * 初始化画笔
     */

    private void initPaints() {
        //绘图画笔
        mPaint = new Paint();//用数组管理画笔
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.parseColor("#8800CC"));
        mPaint.setStrokeWidth(1);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        //绘制图片的画笔
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }
    /**
     * 清除画布
     *
     */

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        //绘制背景
        mCanvas.drawColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);//使用这一句，每次调用都把画好的图片在此绘制一遍，可保留绘画痕迹
        canvas.drawPath(mPath, mPaint);//不断被调用，不断绘制
    }

    /**
     * 触摸事件
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN://按下
                touchStart(x, y);
                break;
            case MotionEvent.ACTION_MOVE://移动
                touchMove(x, y);
                break;
            case MotionEvent.ACTION_UP://提起
                touchEnd(x, y);
                break;
            default:
                break;
        }
        invalidate();//刷新视图
        return true;
    }

    /**
     * 绘制结束
     *
     * @param x
     * @param y
     */
    private void touchEnd(float x, float y) {
        switch (shapeStyle) {
            case 0:
                drawBezierEnd(x, y);//绘制贝塞尔曲线结束
                break;
            case 1:
                drawLine(x, y);
                break;
            case 2:
                drawRect(x, y);
                break;
            case 3:
                drawCircle(x, y);
                break;
            case 4:
                drawOval(x, y);
                break;
            case 5:
                drawRoundRect(x, y);
                break;
            default:
                break;
        }
    }

    /**
     * 绘制贝塞尔曲线结束
     *
     * @param x
     * @param y
     */
    private void drawBezierEnd(float x, float y) {
        mPath.lineTo(x, y);
        mCanvas.drawPath(mPath, mPaint);//这句必须要，否则绘制的东西立即消失
        mPath.reset();
    }

    /**
     * 绘制path
     *
     * @param x
     * @param y
     */
    private void touchMove(float x, float y) {
        //装载临时图片,不抬起就不会真正画到画布上，画贝塞尔除外
        mCanvas.drawBitmap(mTempBitmap, 0, 0, mBitmapPaint);
        switch (shapeStyle) {
            case 0:
                drawBezier(x, y);//绘制贝塞尔曲线
                break;
            case 1:
                drawLine(x, y);//绘制直线
                break;
            case 2:
                drawRect(x, y);//绘制矩形
                break;
            case 3:
                drawCircle(x, y);//绘制圆
                break;
            case 4:
                drawOval(x, y);//绘制椭圆
                break;
            case 5:
                drawRoundRect(x, y);//绘制圆角矩形,圆角12
                break;
            default:
                break;
        }
    }

    /**
     * 绘制直线
     *
     * @param x
     * @param y
     */
    private void drawLine(float x, float y) {
        mCanvas.drawLine(drawX, drawY, x, y, mPaint);
    }


    /**
     * 绘制矩形
     *
     * @param x
     * @param y
     */
    private void drawRect(float x, float y) {
        mCanvas.drawRect(drawX, drawY, x, y, mPaint);
    }


    /**
     * 绘制圆
     *
     * @param x
     * @param y
     */
    private void drawCircle(float x, float y) {
        //求得圆心位置
        float cx = (drawX + x) / 2;
        float cy = (drawY + y) / 2;
        //求得半径
        float radius = (float) Math.sqrt(Math.pow(x - drawX, 2) + Math.pow(y - drawY, 2)) / 2;//半径等于对角线的一半
        mCanvas.drawCircle(cx, cy, radius, mPaint);
    }

    /**
     * 绘制椭圆
     *
     * @param x
     * @param y
     */
    private void drawOval(float x, float y) {
        RectF rectF = new RectF(drawX, drawY, x, y);
        mCanvas.drawOval(rectF, mPaint);
    }

    /**
     * 绘制圆角矩形
     *
     * @param x
     * @param y
     */
    private void drawRoundRect(float x, float y) {
        RectF rectF = new RectF(drawX, drawY, x, y);
        mCanvas.drawRoundRect(rectF, 12, 12, mPaint);

    }

    /**
     * 绘制贝塞尔曲线
     *
     * @param x
     * @param y
     */
    private void drawBezier(float x, float y) {
        float dx = Math.abs(x - drawX);
        float dy = Math.abs(y - drawY);//计算绘制距离
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            //绘制一阶贝赛尔曲线，前两个参数为控制点，后两个为结束点
            mPath.quadTo((x + drawX) / 2, (y + drawY) / 2, x, y);//这里以中间位置为控制
            drawX = x;
            drawY = y;//重新记录绘画点
        }
    }

    /**
     * 绘制开始
     *
     * @param x
     * @param y
     */
    private void touchStart(float x, float y) {
        mPath.reset();//路径工具复位
        mPath.moveTo(x, y);//绘制点移动
        drawX = x;
        drawY = y;
        //记录下来，作为正式绘制的起始点
        mTempBitmap = Bitmap.createBitmap(mBitmap);//保存临时图片
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public void bitmapToString() {
        //将Bitmap转换成字符串
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream);//压缩图像到输出流
        byte[] bitmapBytes = bStream.toByteArray();
        str = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
    }
    /**
     * 保存图片
     *
     */
    public void saveBitmap(File file) {
        //如果文件夹不存在则创建
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        //如果文件不存在则创建
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        OutputStream outputStream = null;
        BufferedOutputStream bos = null;
        try {
            outputStream = new FileOutputStream(file);//输出流
            bos = new BufferedOutputStream(outputStream);//缓冲输出流
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);//压缩图像到输出流
            bos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 设置画笔颜色
     *
     * @param color
     */
    public void setPaintColor(@ColorInt int color) {
        mPaint.setColor(color);
    }

    /**
     * 设置画笔颜色
     *
     * @param colorID 从资源文件中获取
     */
    public void setPaintColorFromResources(@ColorRes int colorID) {
        mPaint.setColor(getResources().getColor(colorID));
    }

    /**
     * 设置画笔颜色
     *
     * @param color 从字符串解析
     */
    public void setPaintColor(String color) {
        try {
            //有可能解析失败
            mPaint.setColor(Color.parseColor(color));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置画笔粗细
     *
     * @param width
     */
    public void setPaintStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    /**
     * 画矩形
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void drawRect(float left, float top, float right, float bottom) {
        mCanvas.drawRect(left, top, right, bottom, mPaint);
    }

    /**
     * 获得画布
     *
     * @return
     */
    public Canvas getCanvas() {
        return mCanvas;
    }

    public void saveCanvas(Bitmap mBitmap) {
       mCanvas.drawBitmap(mBitmap,0,0,null);
       mCanvas = new Canvas(mBitmap);
    }
    /*
    public void setmCanvas(Bitmap bitmap) {
        try {
            mCanvas.drawBitmap(bitmap, 0, 0, mBitmapPaint);
            Log.e("test","读存储");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


 */


    public String getStr() {
        return str;
    }

    /**
     * 获得画笔
     *
     * @return
     */
    public Paint getPaint() {
        return mPaint;
    }

    /**
     * 设置绘制形状
     *
     * @param shapeStyle
     */
    public void setShapeStyle(int shapeStyle) {
        this.shapeStyle = shapeStyle;
    }
}
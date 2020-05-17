package com.qcuncle.draw;


import android.Manifest;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Date;

/**
 *简易绘画程序
 */
public class MainActivity extends AppCompatActivity {
    public Bitmap bitmap;
    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;
    private DevinDrawPanle drawPanle;
    private String strBase64;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }
        drawPanle = new DevinDrawPanle(this);
        //设置画笔粗细
        drawPanle.setPaintStrokeWidth(5);
        //supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //setContentView(R.layout.activity_main);
        getDrawInfo();
        setContentView(drawPanle);
        Toast.makeText(MainActivity.this,"点击右上角开始绘图吧！",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }


    /* 利用反射机制调用MenuBuilder的setOptionalIconsVisible方法设置mOptionalIconsVisible为true，给菜单设置图标时才可见

     * 让菜单同时显示图标和文字

     */

    @Override

    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_option, menu);
        //添加子菜单
        SubMenu mnLineColor = menu.addSubMenu("颜色").setIcon(R.drawable.colortools);
        SubMenu mnShapeStyle = menu.addSubMenu("形状").setIcon(R.drawable.shape);
        //实例化子菜单
        getMenuInflater().inflate(R.menu.menu_paint_colors, mnLineColor);
        getMenuInflater().inflate(R.menu.menu_shape_style, mnShapeStyle);
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /**主菜单
             *
             */
            //保存绘图
            case R.id.menu_item_save:
                saveFile();
                break;
            //清除绘图
            case R.id.menu_item_wipe:
                drawPanle.mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                //这里需要重新绘制背景
                drawPanle.mCanvas.drawColor(Color.WHITE);
                saveDrawInfo();//清除时自动暂存
                Toast.makeText(MainActivity.this,"清除成功",Toast.LENGTH_LONG).show();
                break;
            /*暂存绘图
            case R.id.menu_item_info:
                saveDrawInfo();
                Toast.makeText(MainActivity.this,"暂存成功，读取暂存还未完善",Toast.LENGTH_LONG).show();
                //drawPanle.mCanvas.drawBitmap();
                break;

             */
            //清除绘图
            case R.id.menu_item_test:
                drawPanle.mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                saveDrawInfo();//清除时自动暂存
                break;
            /**颜色选择
             *
             */
            case R.id.color_black:
                drawPanle.setPaintColor(Color.BLACK);
                break;
            case R.id.color_red:
                drawPanle.setPaintColor(Color.RED);
                break;
            case R.id.color_orange:
                drawPanle.setPaintColor("#FF7F00");//有重载形式
                break;
            case R.id.color_yellow:
                drawPanle.setPaintColor(Color.YELLOW);
                break;
            case R.id.color_green:
                drawPanle.setPaintColor(Color.GREEN);
                break;
            case R.id.color_cyan:
                drawPanle.setPaintColor("#00FFFF");
                break;
            case R.id.color_blue:
                drawPanle.setPaintColor(Color.BLUE);
                break;
            case R.id.color_purple:
                drawPanle.setPaintColor("#8800CC");
                break;
            /**设置形状
             *
             */
            case R.id.drawbezier:
                drawPanle.setShapeStyle(0);
                break;
            case R.id.drawline:
                drawPanle.setShapeStyle(1);
                break;
            case R.id.drawrect:
                drawPanle.setShapeStyle(2);
                break;
            case R.id.drawcircle:
                drawPanle.setShapeStyle(3);
                break;
            case R.id.drawoval:
                drawPanle.setShapeStyle(4);
                break;
            case R.id.drawroundrect:
                drawPanle.setShapeStyle(5);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 保存文件
     *
     */
    public void reload() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void saveFile() {
        Date date = new Date();
        //获取当前的日期
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        //设置日期格式
        String str = df.format(date);
        //用系统时间命名文件
        File saveFile = new File(Environment.getExternalStorageDirectory(), "Draw" + str + ".png");
        drawPanle.saveBitmap(saveFile);
        Toast.makeText(this, "图片已保存：" + saveFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }
    /**
     * 暂存绘图
     * shared_prefs
     *
     */
    public void saveDrawInfo(){
        drawPanle.bitmapToString();
        String strBase64 = drawPanle.getStr();
        SharedPreferences sp = getSharedPreferences("DrawInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("Draw", strBase64);
        editor.commit();
    }
    public void getDrawInfo(){
        SharedPreferences sp = null;
        sp = this.getSharedPreferences("DrawInfo", Context.MODE_PRIVATE);
        String str = sp.getString("Draw", strBase64);
        //Bitmap bitmap = null;
        try {
            byte[] bitmapByte = Base64.decode(str, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
            drawPanle.saveCanvas(bitmap);
            Log.e("test","test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

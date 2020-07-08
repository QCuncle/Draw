package com.qcuncle.draw;


import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 简易绘画程序
 */
public class MainActivity extends AppCompatActivity {
    public Bitmap bitmap;
    public String fPath = null;//分享路径
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
        Toast.makeText(MainActivity.this, "点击右上角开始绘图吧！", Toast.LENGTH_LONG).show();
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
                Toast.makeText(MainActivity.this, "图片保存成功\n路径为：" + fPath , Toast.LENGTH_SHORT).show();
                break;
            //分享图片
            case R.id.menu_item_share:
                sharePic();
                break;
            //清除绘图
            case R.id.menu_item_wipe:
                drawPanle.mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                //这里需要重新绘制背景
                drawPanle.mCanvas.drawColor(Color.WHITE);
                saveDrawInfo();//清除时自动暂存
                Toast.makeText(MainActivity.this, "清除成功", Toast.LENGTH_LONG).show();
                break;
            /*暂存绘图（有bug）
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
     * 分享图片
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sharePic() {
        saveFile();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/png");
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos.isEmpty()) {
            return;
        }
        List<Intent> targetIntents = new ArrayList<>();
        for (ResolveInfo info : resolveInfos) {
            ActivityInfo ainfo = info.activityInfo;
            switch (ainfo.packageName) {
                case "com.tencent.mm":
                    addShareIntent(targetIntents, ainfo);
                    break;
                case "com.tencent.mobileqq":
                    addShareIntent(targetIntents, ainfo);
                    break;
                case "com.sina.weibo":
                    addShareIntent(targetIntents, ainfo);
                    break;
            }
        }
        if (targetIntents == null || targetIntents.size() == 0) {
            return;
        }
        Intent chooserIntent = Intent.createChooser(targetIntents.remove(0), "请选择分享平台");
        if (chooserIntent == null) {
            return;
        }
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(new Parcelable[]{}));
        try {
            startActivity(chooserIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "找不到该分享应用组件", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addShareIntent(List<Intent> list, ActivityInfo ainfo) {
        Intent target = new Intent(Intent.ACTION_SEND);
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//临时授权该Uri所代表的文件
        target.setType("image/png");
        File file = new File(fPath);
       // Uri u = FileProvider.getUriForFile(MainActivity.this,"com.qcuncle.draw.fileprovider",file);
        Uri uri = FileProvider.getUriForFile(this,
                "com.qcuncle.draw.fileprovider",
                file);
        target.putExtra(Intent.EXTRA_STREAM,uri);
        target.setPackage(ainfo.packageName);
        target.setClassName(ainfo.packageName, ainfo.name);
        list.add(target);
    }


    /**
     * 保存文件
     */

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void saveFile() {
        String filePath = Environment.getExternalStorageDirectory().toString() + "/Draw";
        File f = new File(filePath);
        //如果文件夹不存在则创建
        if (!f.exists()) {
            f.mkdirs();
        }
        Log.d("msg", "文件夹"+filePath);
        Date date = new Date();
        //获取当前的日期
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        //设置日期格式
        String str = df.format(date);
        String path =  "/" + str + ".png";//文件名
        //用系统时间命名文件
        fPath = filePath + path;
        File saveFile = new File(fPath);
        drawPanle.saveBitmap(saveFile);
        Log.d("msg", "文件路径"+saveFile.getAbsolutePath());
    }

    /**
     * 暂存绘图(还未完善)
     * shared_prefs
     */
    public void saveDrawInfo() {
        drawPanle.bitmapToString();
        String strBase64 = drawPanle.getStr();
        SharedPreferences sp = getSharedPreferences("DrawInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("Draw", strBase64);
        editor.commit();
    }

    public void getDrawInfo() {
        SharedPreferences sp = null;
        sp = this.getSharedPreferences("DrawInfo", Context.MODE_PRIVATE);
        String str = sp.getString("Draw", strBase64);
        //Bitmap bitmap = null;
        try {
            byte[] bitmapByte = Base64.decode(str, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
            drawPanle.saveCanvas(bitmap);
            Log.e("test", "test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

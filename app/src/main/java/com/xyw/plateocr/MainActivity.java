package com.xyw.plateocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //拍照→裁剪→识别
                startActivityForResult(new Intent(MainActivity.this, PlateOcr.class), 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                String[] value = data.getStringArrayExtra("plate");
                String path = data.getStringExtra("path");
                int state = data.getIntExtra("state", -1024);
                if (state == 0) {
                    Toast.makeText(MainActivity.this, "识别成功", Toast.LENGTH_LONG).show();
                    TextView plateData = findViewById(R.id.plate_data);
                    ImageView imageView = findViewById(R.id.plate_image);
                    plateData.setText("车牌号码：" + value[0] + "\n" + "车牌颜色：" + value[1] + "\n");
                    int left = Integer.valueOf(value[7]);
                    int top = Integer.valueOf(value[8]);
                    int w = Integer.valueOf(value[9])
                            - Integer.valueOf(value[7]);
                    int h = Integer.valueOf(value[10])
                            - Integer.valueOf(value[8]);
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    //在使用图片路径识别模式跳入本界面时   请将下面这行代码注释
                    Bitmap b = Bitmap.createBitmap(bitmap, left, top, w, h);
                    if (b != null) {
                        imageView.setImageBitmap(b);
                    }
                }

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

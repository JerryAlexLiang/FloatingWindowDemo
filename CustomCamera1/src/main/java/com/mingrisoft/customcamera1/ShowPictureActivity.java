package com.mingrisoft.customcamera1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

public class ShowPictureActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);
        Intent intent = getIntent();
        imageView = (ImageView) findViewById(R.id.show_image);
        textView = (TextView) findViewById(R.id.txt);
        if (intent != null) {
            String path = intent.getStringExtra("image_path");
            imageView.setImageURI(Uri.parse(path));
            textView.setText("保存路径：" + path);
        }
    }
}

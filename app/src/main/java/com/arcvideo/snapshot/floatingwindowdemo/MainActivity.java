package com.arcvideo.snapshot.floatingwindowdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_one)
    Button btnOne;
    @BindView(R.id.btn_two)
    Button btnTwo;
    @BindView(R.id.btn_three)
    Button btnThree;
    @BindView(R.id.btn_four)
    Button btnFour;
    @BindView(R.id.btn_five)
    Button btnFive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_one, R.id.btn_two, R.id.btn_three, R.id.btn_four, R.id.btn_five})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_one:
                Intent intent = new Intent(MainActivity.this,CameraActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_two:
                break;
            case R.id.btn_three:
                break;
            case R.id.btn_four:
                break;
            case R.id.btn_five:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

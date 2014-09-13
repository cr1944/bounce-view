
package com.example.bounce;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.bounce.BounceView.BounceCallback;

public class MainActivity extends Activity implements BounceCallback {
    View layout;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BounceView bv = (BounceView) findViewById(R.id.bv);
        bv.setBounceCallback(this);
        layout = findViewById(android.R.id.content);
        tv = (TextView) findViewById(android.R.id.text1);
        ListView lv = (ListView) findViewById(android.R.id.list);
        String[] data = new String[100];
        for (int i = 0;i<data.length;i++) {
            data[i] = "Item" + i;
        }
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data));
    }

    @Override
    public void onScrollChanged(float factor) {
        //Log.d("MainActivity", "onScrollChanged:" + factor);
        interpolate(tv, factor);
    }

    private void interpolate(View view1, float factor) {

        float scaleX = 1f + (0.5f - 1f) * factor;
        float scaleY = 1f + (0.5f - 1f) * factor;
        float translationX = -factor * (layout.getWidth() - tv.getWidth()) / 2;
        float translationY = 0;

        view1.setTranslationX(translationX);
        view1.setTranslationY(translationY);
        view1.setScaleX(scaleX);
        view1.setScaleY(scaleY);
    }

}

package com.waterchen.pickdialogtest;

import java.util.ArrayList;
import java.util.List;

import com.jingchen.timerpicker.R;
import com.waterchen.pickdialogtest.PickerDialog.OnPickListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	protected static final String TAG = "MainActivity";
	private PickerDialog mPickerDialog;
	private Button dialogBtn;
	private List<String> mItemDataList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mItemDataList = new ArrayList<String>();
		for(int i = 1; i <= 60; i++){
			mItemDataList.add(""+i);
		}

		dialogBtn = (Button) findViewById(R.id.dialog);
		dialogBtn.setOnClickListener(this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (mPickerDialog == null) {
			mPickerDialog = new PickerDialog(this, mItemDataList);
			mPickerDialog.setOnPickListener(new OnPickListener() {
				@Override
				public void onPick(int itemId, String itemData) {
					Log.d(TAG, "id :" + itemId + " item :" + itemData);
				}
			});
		}

		mPickerDialog.show();

	}

}

package com.waterchen.pickdialogtest;

import java.util.List;

import com.jingchen.timerpicker.R;
import com.waterchen.pickdialogtest.PickerView.onSelectListener;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.View.OnClickListener;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;

public class PickerDialog extends Dialog implements OnClickListener {

	/**
	 * 动画时间
	 */
	public static final int DEFAULT_DURING = 300;
	/**
	 * 弹出Dialog的动画
	 */
	private TranslateAnimation mOpenAnimation;

	/**
	 * 关闭Dialog的动画
	 */
	private TranslateAnimation mCloseAnimation;

	private View mActionSheetView;

	private PickerView mPickerView;

	private TextView mCancelView;
	private TextView mSureView;

	private OnPickListener mPickListener;

	private int mSelectedItemId;

	private String mSelectedItemData;
	private List<String> mItemsList;
	/**
	 * 标记是否已经关闭,以防多次点击阴影层造成的问题
	 */
	private boolean mIsClosed;

	public PickerDialog(Context context, List<String> items) {
		super(context);
		this.mItemsList = items;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pickerdialog_layout);

		// 初始化Window属性
		initWindow();
		// 初始化动画
		initAnimation();
		// 初始化控件
		initViews();

		mIsClosed = false;
	}

	/**
	 * 初始化Window属性
	 */
	private void initWindow() {
		Window win = getWindow();
		win.getDecorView().setPadding(0, 0, 0, 0);
		WindowManager.LayoutParams lp = win.getAttributes();
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		win.setAttributes(lp);
		// Dialog背景色,使Dialog背景色为透明
		Drawable drawable = new ColorDrawable();
		drawable.setAlpha(0);
		win.setBackgroundDrawable(drawable);
	}

	private void initAnimation() {
		int type = TranslateAnimation.RELATIVE_TO_SELF;
		if (mOpenAnimation == null) {
			mOpenAnimation = new TranslateAnimation(type, 0, type, 0, type, 1, type, 0);
			mOpenAnimation.setDuration(DEFAULT_DURING);
			mOpenAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
				}
			});
		}
		if (mCloseAnimation == null) {
			mCloseAnimation = new TranslateAnimation(type, 0, type, 0, type, 0, type, 1);
			mCloseAnimation.setDuration(DEFAULT_DURING);
			mCloseAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// 动画结束后撤销Dialog
					closePickerDialog();
				}
			});
		}

	}

	/**
	 * 初始化控件
	 */
	private void initViews() {
		mCancelView = (TextView) findViewById(R.id.pickerdialog_cancel);
		mSureView = (TextView) findViewById(R.id.pickerdialog_sure);
		mPickerView = (PickerView) findViewById(R.id.pickerdialog_pv);
		mActionSheetView = findViewById(R.id.pickerdialog_content_rl);

		mPickerView.setData(mItemsList);
		mPickerView.setSelected(0);
		mPickerView.setTextSize(20);
		mPickerView.setItemCount(7);
		mPickerView.setOnSelectListener(new onSelectListener() {
			@Override
			public void onSelect(int index, String data) {
				mSelectedItemData = data;
				mSelectedItemId = index;
			}
		});

		mActionSheetView.setOnClickListener(this);
		mCancelView.setOnClickListener(this);
		mSureView.setOnClickListener(this);

		mSelectedItemId = 0;
		mSelectedItemData = mItemsList.get(mSelectedItemId);
	}

	private void closePickerDialog() {
		mIsClosed = false;
		dismiss();
	}

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.pickerdialog_content_rl:
			if (!mIsClosed) {
				mActionSheetView.startAnimation(mCloseAnimation);
				mIsClosed = true;
			}
			break;

		case R.id.pickerdialog_cancel:
			if (!mIsClosed) {
				mActionSheetView.startAnimation(mCloseAnimation);
				mIsClosed = true;
			}
			break;
		case R.id.pickerdialog_sure:
			if (mPickListener != null) {
				mPickListener.onPick(mSelectedItemId, mSelectedItemData);
			}
			break;
		default:
			break;
		}
	}

	public interface OnPickListener {
		public void onPick(int itemId, String itemData);
	}

	public void setOnPickListener(OnPickListener pickListener) {
		this.mPickListener = pickListener;
	}

	@Override
	protected void onStart() {
		super.onStart();
		//显示从边沿进入的
		mActionSheetView.startAnimation(mOpenAnimation);
	}


}

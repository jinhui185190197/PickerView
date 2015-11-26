package com.waterchen.pickdialogtest;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

public class PickerView extends View {

	public static final String TAG = "PickerView";

	public static final int DEFAULT_TEXT_SIZE = 20;
	public static final int DEFAULT_SHOW_COUNT = 5;

	public static final int MESSAGE_SCROLL = 1000;
	public static final int MESSAGE_SELECTED_ITEM = 3000;

	public static final int DEFAULT_SELECTED_TEXT_COLOR = 0xff313131;
	public static final int DEFAULT_ITEM_TEXT_COLOR = 0xffafafaf;
	public static final int DEFAULT_LINE_COLOR = 0xffc5c5c5;
	/**
	 * 最大惯性滑动速度
	 */
	public static final int MAX_FLING_VELOCITY = 6000;
	public static final int MAX_FLING_DISTANCE = 0x7FFFFFFF;

	private List<String> mDataList;


	/**
	 * 实现fling后的匀减速滑动
	 */
	private Scroller mScroller;
	/**
	 * 手势控件
	 */
	private GestureDetector mGestureDetector;
	/**
	 * 标志是否正在匀减速滑动
	 */
	private boolean isFlingPerformed;
	/**
	 * 标识是否强制停止
	 */
	private boolean isForceStoped;

	private onSelectListener mSelectListener;
	private Timer timer;
	private UpdateTimerTask mTask;

	private Context mContext;
	private boolean isAlignCenter = true;

	// ------------------------------------------
	private int mTextSize;
	private int mMaxTextWidth;
	private int mMaxTextHeight;
	private int mItemColor;
	private int mSelectColor;
	private int mLineColor;
	/**
	 * 字体之间的间隙大小(以最大字体大小为基准)
	 */
	private float mLineSpacingMultiplier;
	private boolean isLoop;
	/**
	 * 选中框上下线的位置
	 */
	private int mTopLineY;
	private int mBottomLineY;

	private int mPreCurrentIndex;
	private int mInitPosition;
	/**
	 * 显示item数
	 */
	private int mItemCount;
	/**
	 * 直径
	 */
	private int mMeasuredHeight;
	/**
	 * 半圆的长度
	 */
	private int mHalfCircumference;
	/**
	 * 半径
	 */
	private int mRadius;
	private int mMeasuredWidth;
	/**
	 * 记录移动了多少个小块
	 */
	private int mChange;
	private float mLastPositionY;
	/**
	 * 其他item的画笔
	 */
	private Paint mItemTextPaint;
	/**
	 * 被选中文字的画笔
	 */
	private Paint mSelectedTextPaint;
	/**
	 * 选中框的画笔
	 */
	private Paint mLinePaint;

	private int mSelectedItem;
	private int mTotalScrollY;

	Handler updateHandler = new Handler() {

		@Override
		public void handleMessage(Message paramMessage) {
			if (paramMessage.what == MESSAGE_SCROLL) {
				// 刷新
				invalidate();
			} else if (paramMessage.what == MESSAGE_SELECTED_ITEM) {
				// 结束,回调选择
				performSelect();
			}
		}

	};

	public PickerView(Context context) {
		this(context, null);
	}

	public PickerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mContext = context;
		mScroller = new Scroller(context, new DecelerateInterpolator());
		mGestureDetector = new GestureDetector(context, mGestureListener);
		init();
	}

	public void setOnSelectListener(onSelectListener listener) {
		mSelectListener = listener;
	}

	/**
	 * 滚动结束,回调被选中的项
	 */
	private void performSelect() {
		if (mSelectListener != null)
			mSelectListener.onSelect(mSelectedItem, mDataList.get(mSelectedItem));
	}

	public void setData(List<String> datas) {
		mDataList = datas;
		if (mDataList != null) {
			if (mInitPosition >= mDataList.size()) {
				mInitPosition = 0;
			}
		}
		initData();
		invalidate();
	}

	public void setTextAlignCenter(boolean isCenter) {
		this.isAlignCenter = isCenter;
		initData();
		invalidate();
	}

	/**
	 * 选择选中的item的index
	 * 
	 * @param selected
	 */
	public void setSelected(int selected) {
		mInitPosition = (selected < 0) ? 0 : selected;
	}

	public void setItemCount(int itemCount) {
		this.mItemCount = itemCount;
		initData();
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		initData();
		mMeasuredWidth = getMeasuredWidth();
	}

	private void init() {
		mTextSize = (int) (mContext.getResources().getDisplayMetrics().density * DEFAULT_TEXT_SIZE);
		mItemColor = DEFAULT_ITEM_TEXT_COLOR;
		mSelectColor = DEFAULT_SELECTED_TEXT_COLOR;
		mLineColor = DEFAULT_LINE_COLOR;
		mLineSpacingMultiplier = 2.0F;
		isLoop = true;
		mInitPosition = -1;
		mItemCount = DEFAULT_SHOW_COUNT;
		mLastPositionY = 0.0F;
		mTotalScrollY = 0;
		setTextSize(16F);

		mItemTextPaint = new Paint();
		mSelectedTextPaint = new Paint();
		mLinePaint = new Paint();

		timer = new Timer();
	}

	private void initData() {
		if (mDataList == null) {
			return;
		}
		// 初始化item字体画笔
		mItemTextPaint.setColor(mItemColor);
		mItemTextPaint.setAntiAlias(true);
		mItemTextPaint.setTypeface(Typeface.MONOSPACE);
		mItemTextPaint.setTextSize(mTextSize);
			
		// 初始化被选中字体画笔
		mSelectedTextPaint.setColor(mSelectColor);
		mSelectedTextPaint.setAntiAlias(true);
		mSelectedTextPaint.setTextScaleX(1.05F);
		mSelectedTextPaint.setTypeface(Typeface.MONOSPACE);
		mSelectedTextPaint.setTextSize(mTextSize);
			mSelectedTextPaint.setTextAlign(Align.CENTER);
			
		if(isAlignCenter){
			mItemTextPaint.setTextAlign(Align.CENTER);
			mSelectedTextPaint.setTextAlign(Align.CENTER);
		}else{
			mItemTextPaint.setTextAlign(Align.LEFT);
			mSelectedTextPaint.setTextAlign(Align.LEFT);
		}
		// 初始化选中框画笔
		mLinePaint.setColor(mLineColor);
		mLinePaint.setAntiAlias(true);
		mLinePaint.setTypeface(Typeface.MONOSPACE);
		mLinePaint.setTextSize(mTextSize);
		// 计算数据中字体的最大宽高值
		measureTextWidthHeight();

		mHalfCircumference = (int) (mMaxTextHeight * mLineSpacingMultiplier * (mItemCount - 1));
		mMeasuredHeight = (int) ((mHalfCircumference * 2) / Math.PI);
		mRadius = (int) (mHalfCircumference / Math.PI);
		mTopLineY = (int) (getHeight() / 2.0f - mLineSpacingMultiplier * mMaxTextHeight / 2.0f);
		mBottomLineY = (int) (mTopLineY + mLineSpacingMultiplier * mMaxTextHeight);
		if (mInitPosition == -1) {
			if (isLoop) {
				mInitPosition = (mDataList.size() + 1) / 2;
			} else {
				mInitPosition = 0;
			}
		}
		mPreCurrentIndex = mInitPosition;
	}

	/**
	 * 计算被选中字体的宽高最大值
	 */
	private void measureTextWidthHeight() {
		Rect rect = new Rect();
		for (int i = 0; i < mDataList.size(); i++) {
			String curStr = (String) mDataList.get(i);
			mSelectedTextPaint.getTextBounds(curStr, 0, curStr.length(), rect);
			int textWidth = rect.width();
			if (textWidth > mMaxTextWidth) {
				mMaxTextWidth = textWidth;
			}
			mSelectedTextPaint.getTextBounds(curStr, 0, curStr.length(), rect); // 星期
			int textHeight = rect.height();
			if (textHeight > mMaxTextHeight) {
				mMaxTextHeight = textHeight;
			}
		}

	}

	/**
	 * 设置字体大小
	 * 
	 * @param size
	 */
	public final void setTextSize(float size) {
		if (size > 0.0F) {
			mTextSize = (int) (mContext.getResources().getDisplayMetrics().density * size);
		}
		initData();
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {

		String as[];
		if (mDataList == null) {
			super.onDraw(canvas);
			return;
		}
		as = new String[mItemCount];

		// 计算移动了多少个单位位置
		mChange = (int) (mTotalScrollY / (mLineSpacingMultiplier * mMaxTextHeight));
		// 计算将要被选中的文字的下标
		mPreCurrentIndex = mInitPosition + mChange % mDataList.size();
		if (!isLoop) {
			if (mPreCurrentIndex < 0) {
				mPreCurrentIndex = 0;
			}
			if (mPreCurrentIndex > mDataList.size() - 1) {
				mPreCurrentIndex = mDataList.size() - 1;
			}
		} else {
			if (mPreCurrentIndex < 0) {
				mPreCurrentIndex = mDataList.size() + mPreCurrentIndex;
			}
			if (mPreCurrentIndex > mDataList.size() - 1) {
				mPreCurrentIndex = mPreCurrentIndex - mDataList.size();
			}
		}

		// 计算偏移量(不足一个单位位置)
		int j2 = (int) (mTotalScrollY % (mLineSpacingMultiplier * mMaxTextHeight));
		int k1 = 0;
		// 确定当前需要显示的item数据
		while (k1 < mItemCount) {
			int l1 = mPreCurrentIndex - (mItemCount / 2 - k1);
			if (isLoop) {
				if (l1 < 0) {
					l1 = l1 + mDataList.size();
				}
				if (l1 > mDataList.size() - 1) {
					l1 = l1 - mDataList.size();
				}
				as[k1] = (String) mDataList.get(l1);
			} else if (l1 < 0) {
				as[k1] = "";
			} else if (l1 > mDataList.size() - 1) {
				as[k1] = "";
			} else {
				as[k1] = (String) mDataList.get(l1);

			}
			k1++;
		}

		int left = (isAlignCenter) ? mMeasuredWidth / 2 : (mMeasuredWidth - mMaxTextWidth) / 2;
		// 绘制选中框的线
		canvas.drawLine(0.0F, mTopLineY, mMeasuredWidth, mTopLineY, mLinePaint);
		canvas.drawLine(0.0F, mBottomLineY, mMeasuredWidth, mBottomLineY, mLinePaint);

		// 绘制需要显示的item
		int j1 = 0;
		while (j1 < mItemCount) {
			canvas.save();
			float itemHeight = mMaxTextHeight * mLineSpacingMultiplier;
			double radian = ((itemHeight * j1 - j2) * Math.PI) / mHalfCircumference;
			float angle = (float) (90D - (radian / Math.PI) * 180D);
			if (angle >= 90F || angle <= -90F) {
				canvas.restore();
			} else {
				// 计算canvas y轴方向上移动的距离
				int translateY = (int) ((mRadius - Math.cos(radian) * mRadius
						- (Math.sin(radian) * mMaxTextHeight) / 2D) + (getHeight() - mMeasuredHeight) / 2.0f);
				canvas.translate(0.0F, translateY);
				// 设置垂直方向上的压缩比(越靠近上下端,字的高度越小)
				canvas.scale(1.0F, (float) Math.sin(radian));
				if (translateY <= mTopLineY && mMaxTextHeight + translateY >= mTopLineY) {
					// 在选中框的上方分界线之间
					canvas.save();
					canvas.clipRect(0, 0, mMeasuredWidth, mTopLineY - translateY);
					canvas.drawText(as[j1], left, mMaxTextHeight, mItemTextPaint);
					canvas.restore();
					canvas.save();
					canvas.clipRect(0, mTopLineY - translateY, mMeasuredWidth, (int) (itemHeight));
					canvas.drawText(as[j1], left, mMaxTextHeight, mSelectedTextPaint);
					canvas.restore();
				} else if (translateY <= mBottomLineY && mMaxTextHeight + translateY >= mBottomLineY) {
					// 在选中框的下方分界线之间
					canvas.save();
					canvas.clipRect(0, 0, mMeasuredWidth, mBottomLineY - translateY);
					canvas.drawText(as[j1], left, mMaxTextHeight, mSelectedTextPaint);
					canvas.restore();
					canvas.save();
					canvas.clipRect(0, mBottomLineY - translateY, mMeasuredWidth, (int) (itemHeight));
					canvas.drawText(as[j1], left, mMaxTextHeight, mItemTextPaint);
					canvas.restore();
				} else if (translateY >= mTopLineY && mMaxTextHeight + translateY <= mBottomLineY) {
					// 此时在选中框的区域内
					canvas.clipRect(0, 0, mMeasuredWidth, (int) (itemHeight));
					canvas.drawText(as[j1], left, mMaxTextHeight, mSelectedTextPaint);
					mSelectedItem = mDataList.indexOf(as[j1]);
				} else {
					canvas.clipRect(0, 0, mMeasuredWidth, (int) (itemHeight));
					canvas.drawText(as[j1], left, mMaxTextHeight, mItemTextPaint);
				}
				canvas.restore();
			}
			j1++;
		}
		super.onDraw(canvas);

	}

	/**
	 * 手势监听器,根据不同手势采取不同操作
	 */
	private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// 惯性滑动手势
			// 需要限制速度大小,过快会出现BUG
			int velocity = (int) velocityY;
			if (Math.abs(velocity) > MAX_FLING_VELOCITY) {
				velocity = (velocityY > 0) ? MAX_FLING_VELOCITY : -MAX_FLING_VELOCITY;
			}
			// 使用Scroller计算惯性滑动过程
			isFlingPerformed = true;
			mScroller.fling(0, (int) mLastPositionY, 0, velocity / 2, 0, 0, -MAX_FLING_DISTANCE, MAX_FLING_DISTANCE);
			invalidate();
			return true;
		};

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			// 滑动手势
			doScroll(e2.getY());
			return true;
		};

		public boolean onDown(MotionEvent event) {
			// 如果正在fling,手指下按时强制停止滑动
			if (isFlingPerformed) {
				isForceStoped = true;
				mScroller.forceFinished(true);
				invalidate();
			}
			doDown(event);
			return false;
		};

	};

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (!mGestureDetector.onTouchEvent(event) && event.getAction() == MotionEvent.ACTION_UP) {
			// 自动归位
			doUp();
		}
		return true;
	}

	private void doDown(MotionEvent event) {
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
		mLastPositionY = event.getY();
	}

	private void doUp() {
		// 抬起手后mCurrentSelected的位置由当前位置move到中间选中位置
		if (Math.abs(mTotalScrollY) < 0.0001) {
			mTotalScrollY = 0;
			return;
		}
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
		int offset = (int) (mTotalScrollY % (mLineSpacingMultiplier * mMaxTextHeight));
		mTask = new UpdateTimerTask(updateHandler, offset);
		timer.schedule(mTask, 0, 10);
	}

	/**
	 * 进行滑动操作
	 * 
	 * @param dY
	 */
	private void doScroll(float dy) {
		float moveLength = mLastPositionY - dy;
		mLastPositionY = dy;
		mTotalScrollY = (int) ((float) mTotalScrollY + moveLength);
		if (!isLoop) {
			int initPositionCircleLength = (int) (mInitPosition * (mLineSpacingMultiplier * mMaxTextHeight));
			int initPositionStartY = -1 * initPositionCircleLength;
			if (mTotalScrollY < initPositionStartY) {
				mTotalScrollY = initPositionStartY;
			}
		}
		invalidate();
	}

	public interface onSelectListener {
		void onSelect(int index, String data);
	}

	@Override
	public void computeScroll() {
		super.computeScroll();

		if (isFlingPerformed) {
			if (mScroller.computeScrollOffset()) {
				// 继续修改相关值,实现自动滚动
				doScroll(mScroller.getCurrY());
			} else {
				isFlingPerformed = false;
				// 判断是否在fling的过程中强制停止(这时候不需要自动回滚到正确位置)
				if (isForceStoped) {
					isForceStoped = false;
				} else {
					// 正常停止,回滚到正确位置
					doUp();
				}
			}
		}
	}

	/**
	 * 定时任务,用于滑动结束后自动回滚
	 * 
	 * @author waterchen
	 *
	 */
	class UpdateTimerTask extends TimerTask {
		Handler handler;
		int realTotalOffset;
		int realOffset;
		int offset;

		public UpdateTimerTask(Handler scrollHandler, int scrollOffset) {
			handler = scrollHandler;
			offset = scrollOffset;
			realTotalOffset = Integer.MAX_VALUE;
			realOffset = 0;
		}

		@Override
		public void run() {

			if (realTotalOffset == Integer.MAX_VALUE) {
				float itemHeight = mLineSpacingMultiplier * mMaxTextHeight;
				offset = (int) ((offset + itemHeight) % itemHeight);
				if ((float) offset > itemHeight / 2.0F) {
					realTotalOffset = (int) (itemHeight - (float) offset);
				} else {
					realTotalOffset = -offset;
				}
			}
			realOffset = (int) ((float) realTotalOffset * 0.1F);

			if (realOffset == 0) {
				if (realTotalOffset < 0) {
					realOffset = -1;
				} else {
					realOffset = 1;
				}
			}
			if (Math.abs(realTotalOffset) <= 0) {
				mTask.cancel();
				handler.sendEmptyMessage(3000);
				return;
			} else {
				mTotalScrollY = mTotalScrollY + realOffset;
				handler.sendEmptyMessage(1000);
				realTotalOffset = realTotalOffset - realOffset;
				return;
			}
		}

	}

}


package com.example.bounce;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class BounceView extends ViewGroup {
    private static final String TAG = "BounceView";
    private static final boolean DEBUG = true;
    private View mTopView;
    private View mBottomView;
    private View mStableView;
    private int mTopViewId;
    private int mBottomViewId;
    private int mStableViewId;
    private Scroller mScroller;
    private boolean mIsBeingDragged;
    private boolean mIsTouchFromHeader;
    private int mActivePointerId = INVALID_POINTER;
    private static final int INVALID_POINTER = -1;
    private float mLastMotionX;
    private float mLastMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private int mMinTopHeight;
    private int mMaxTopHeight;
    private int mTopScrollDistance;
    private static final int DEFAULT_MIN_TOP_HEIGHT = 120;
    private static final int DEFAULT_MAX_TOP_HEIGHT = 300;
    private static final int DEFAULT_TOP_SCROLL = 80;
    private BounceCallback mCallback;
    public interface BounceCallback {
        /**
         * 
         * @param factor indicate the scroll distance, [0,1]
         */
        public void onScrollChanged(float factor);
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
    private static final Interpolator sScrollInterpolator = new Interpolator() {
        
        @Override
        public float getInterpolation(float input) {
            return (float)(1.0f - Math.pow((1.0f - input / 2), 2));
        }
    };

    public BounceView(Context context) {
        this(context, null);
    }

    public BounceView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.BounceView);
    }

    public BounceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScroller = new Scroller(context, sInterpolator);
        float density = getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BounceView, defStyle, 0);
        mMinTopHeight = a.getDimensionPixelOffset(R.styleable.BounceView_minTopHeight,
                (int) (DEFAULT_MIN_TOP_HEIGHT * density));
        mMaxTopHeight = a.getDimensionPixelOffset(R.styleable.BounceView_maxTopHeight,
                (int) (DEFAULT_MAX_TOP_HEIGHT * density));
        mTopScrollDistance = a.getDimensionPixelSize(R.styleable.BounceView_topScrollDistance,
                (int) (DEFAULT_TOP_SCROLL * density));
        mTopViewId = a.getResourceId(R.styleable.BounceView_topViewId, -1);
        mBottomViewId = a.getResourceId(R.styleable.BounceView_bottomViewId, -1);
        mStableViewId = a.getResourceId(R.styleable.BounceView_stableViewId, -1);
        a.recycle();
    }

    @Override
    protected void onFinishInflate() {
        if (mTopViewId == -1 || mBottomViewId == -1) {
            throw new IllegalStateException("invalid top view or bottom view id!");
        }
        if (mStableViewId != -1) {
            mStableView = findViewById(mStableViewId);
        }
        mTopView = findViewById(mTopViewId);
        mTopView.setClickable(true);
        mBottomView = findViewById(mBottomViewId);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mTopView.layout(l, t - mTopScrollDistance, r, t + mTopView.getMeasuredHeight() - mTopScrollDistance);
        mStableView.layout(l, t, r, t + mStableView.getMeasuredHeight());
        mBottomView.layout(l, b - mBottomView.getMeasuredHeight(), r, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec));
        final int measuredWidth = getMeasuredWidth();
        final int measuredHeight = getMeasuredHeight();
        int childWidthSize = measuredWidth - getPaddingLeft() - getPaddingRight();
        int childHeightSize = measuredHeight - getPaddingTop() - getPaddingBottom();

        ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) mTopView.getLayoutParams();
        final int widthSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
        int heightSize = mMaxTopHeight;
        int heightMode = MeasureSpec.EXACTLY;
        if (lp.height > 0) {
            heightSize = lp.height;
        }
        final int topHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
        mTopView.measure(widthSpec, topHeightSpec);
        ViewGroup.LayoutParams lp2 = (ViewGroup.LayoutParams) mStableView.getLayoutParams();
        heightSize = mMaxTopHeight;
        if (lp2.height > 0) {
            heightSize = lp2.height;
        }
        final int stableHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
        mStableView.measure(widthSpec, stableHeightSpec);
        heightSize = childHeightSize - mMinTopHeight;
        final int bottomHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
        mBottomView.measure(widthSpec, bottomHeightSpec);

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

        switch (action) {
            /**
             * We have to return false of ACTION_DOWN here for listview touch event.
             * As we return false here, ACTION_DOWN is send to child.
             * If child NOT handled, ACTION_DOWN will send back to onTouchEvent here.
             * Then if we return true in onTouchEvent, following events will be
             * sent to onTouchEvent directly, not here any more.
             * On the other hand, if we return false in onTouchEvent, no more event will
             * be sent to here or onTouchEvent.
             * So we should make sure child can handle touch event, by setClickable,
             * to enforce follow events send here first.
             */
            case MotionEvent.ACTION_DOWN: {
                if (DEBUG) Log.v(TAG, "MotionEvent.ACTION_DOWN");
                //mScroller.abortAnimation();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mLastMotionY = mInitialMotionY = ev.getY();
                mLastMotionX = mInitialMotionX = ev.getX();
                mIsTouchFromHeader = isTouchInHeaderView(mInitialMotionX, mInitialMotionY);
                Log.d(TAG, "mInitialMotionX:" + mInitialMotionX + ", mInitialMotionY:" + mInitialMotionY
                        +", mIsTouchFromHeader:" + mIsTouchFromHeader);
                return false;
            }
            case MotionEvent.ACTION_MOVE: {
                if (DEBUG) Log.v(TAG, "MotionEvent.ACTION_MOVE");
                if (mIsBeingDragged) {
                    return true;
                } else if (mIsTouchFromHeader) {
                    if (ev.getY() < mLastMotionY) {
                        final int pointerIndex = MotionEventCompat
                                .findPointerIndex(ev, mActivePointerId);
                        mLastMotionY = MotionEventCompat.getY(ev, pointerIndex);
                        mLastMotionX = MotionEventCompat.getX(ev, pointerIndex);
                        mIsBeingDragged = false;
                    } else {
                        mIsBeingDragged = true;
                    }
                } else {
                    mIsBeingDragged = false;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (DEBUG) Log.v(TAG, "MotionEvent.ACTION_CANCEL");
                mIsBeingDragged = false;
                mIsTouchFromHeader = false;
                mActivePointerId = INVALID_POINTER;
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (DEBUG) Log.v(TAG, "MotionEvent.ACTION_UP");
                final int index = MotionEventCompat.getActionIndex(ev);
                int pointerId = MotionEventCompat.getPointerId(ev, index);
                if (pointerId == mActivePointerId) {
                    scrollBack();
                    mIsBeingDragged = false;
                    mIsTouchFromHeader = false;
                    mActivePointerId = INVALID_POINTER;
                }
                break;
            }
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (DEBUG) Log.d(TAG, "MotionEvent.ACTION_DOWN");
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (DEBUG) Log.d(TAG, "MotionEvent.ACTION_MOVE");
                if (mActivePointerId == INVALID_POINTER) {
                    return true;
                }
                final int pointerIndex = MotionEventCompat
                        .findPointerIndex(event, mActivePointerId);
                final float y = MotionEventCompat.getY(event, pointerIndex);
                final float x = MotionEventCompat.getX(event, pointerIndex);
                //final float deltaX = mLastMotionX - x;
                final float deltaY = mLastMotionY - y;
                mLastMotionX = x;
                mLastMotionY = y;
                if (mIsBeingDragged) {
                    if (deltaY < 0) {
                        //scroll to bottom, decelerate
                        float distance = getHeight() - mInitialMotionY;
                        int originalScroll = (int) (getScrollY() + deltaY);
                        int realScroll = (int) (sScrollInterpolator.getInterpolation(y / distance)
                                * (mMinTopHeight - mMaxTopHeight));
                        scrollTo(getScrollX(), Math.max(realScroll, originalScroll));
                    } else {
                        //scroll up, linear
                        if (getScrollY() + deltaY == 0) {
                            return true;
                        } else if (getScrollY() + deltaY > 0) {
                            scrollBy(0, 0 - getScrollY());
                        } else {
                            scrollBy(0, (int) deltaY);
                        }
                    }
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (DEBUG) Log.d(TAG, "MotionEvent.ACTION_UP");
                final int index = MotionEventCompat.getActionIndex(event);
                int pointerId = MotionEventCompat.getPointerId(event, index);
                if (pointerId == mActivePointerId) {
                    scrollBack();
                    mActivePointerId = INVALID_POINTER;
                    mIsBeingDragged = false;
                    mIsTouchFromHeader = false;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (DEBUG) Log.d(TAG, "MotionEvent.ACTION_CANCEL");
                if (mIsBeingDragged) {
                    mActivePointerId = INVALID_POINTER;
                }
                mIsBeingDragged = false;
                mIsTouchFromHeader = false;
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                if (DEBUG) Log.d(TAG, "MotionEvent.ACTION_POINTER_DOWN");
                // final int index = MotionEventCompat.getActionIndex(event);
                // final float y = MotionEventCompat.getY(event, index);
                // mLastMotionY = y;
                // mActivePointerId = MotionEventCompat.getPointerId(event,
                // index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP: {
                if (DEBUG) Log.d(TAG, "MotionEvent.ACTION_POINTER_UP");
                final int index = MotionEventCompat.getActionIndex(event);
                int pointerId = MotionEventCompat.getPointerId(event, index);
                if (pointerId == mActivePointerId) {
                    scrollBack();
                    mActivePointerId = INVALID_POINTER;
                    mIsBeingDragged = false;
                    mIsTouchFromHeader = false;
                }
                // onSecondaryPointerUp(event);
                // mLastMotionY = MotionEventCompat.getY(event,
                // MotionEventCompat.findPointerIndex(event, mActivePointerId));
                break;
            }
        }
        return true;
    }

    private boolean isTouchInHeaderView(float x, float y) {
        return x >= 0 && x <= getWidth() && y >= 0 && y <= mMinTopHeight;
    }

    private void scrollBack() {
        mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY(), 300);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int y = mScroller.getCurrY();

            scrollTo(getScrollX(), y);

            // Keep on drawing until the animation has finished.
            ViewCompat.postInvalidateOnAnimation(this);
            return;
        }

    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == mTopView) {
            int saveCount = canvas.save();
            //scroll down, scrollY < 0
            int scrollY = getScrollY();
            float distance = 0;
            if (scrollY != 0) {
                distance = scrollY / (float) (mMinTopHeight - mMaxTopHeight);
            }
            if (mCallback != null) {
                mCallback.onScrollChanged(distance);
            }
            float topScrollY = mTopScrollDistance * distance;
            //dy > 0, translate down
            canvas.translate(0, scrollY + topScrollY);
//            canvas.clipRect(getScrollX() + getPaddingLeft(), getScrollY() / 10 + getPaddingTop(),
//                    getScrollX() + getRight() - getLeft() - getPaddingRight(),
//                    getScrollY() / 10 + getBottom() - getTop() - getPaddingBottom());
            boolean result = super.drawChild(canvas, child, drawingTime);
            canvas.restoreToCount(saveCount);
            return result;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    public void setBounceCallback(BounceCallback callback) {
        mCallback = callback;
    }
}

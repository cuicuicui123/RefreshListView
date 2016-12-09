package com.example.refreshlistview;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Cui on 2016/12/5.
 */


public class RefreshListView extends ListView implements AbsListView.OnScrollListener {
    private Context mContext;
    private View mHeaderView;
    private TextView mHeaderTv;
    private ImageView mHeaderProgressIv;
    private AnimationDrawable mHeaderDrawable;
    private ImageView mArrowIv;
    private Animation mPullAnimation;
    private Animation mReturnAnimation;

    private View mFooterView;
    private TextView mFooterTv;
    private ImageView mFooterProgressIv;
    private AnimationDrawable mFooterDrawable;

    private int mHeaderHeight;
    private int mFooterHeight;
    private int mState;
    private static final int PULL_TO_REFRESH = 0;
    private static final int RELEASE_TO_REFRESH = 1;
    private static final int DONE = 2;
    private static final int REFRESHING = 3;

    private boolean mCanRefresh;
    private boolean mCanLoad;

    private int mScale = 4;//距离缩放倍数，手指移动距离大于4倍headerView高度代表可以进行刷新
    private float mStartY;
    private OnRefreshListener mOnRefreshListener;
    private OnLoadListener mOnLoadListener;


    public RefreshListView(Context context) {
        super(context);
        init(context);
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        mContext = context;

        mHeaderView = LayoutInflater.from(mContext).inflate(R.layout.header_view, null);
        mHeaderTv = (TextView) mHeaderView.findViewById(R.id.tv_header);
        mHeaderProgressIv = (ImageView) mHeaderView.findViewById(R.id.iv_header_progress);
        mHeaderDrawable = (AnimationDrawable) mHeaderProgressIv.getDrawable();
        mHeaderDrawable.setOneShot(false);
        mHeaderProgressIv.setVisibility(GONE);
        mArrowIv = (ImageView) mHeaderView.findViewById(R.id.iv_arrow);
        mPullAnimation = AnimationUtils.loadAnimation(mContext, R.anim.arrow_rotate);
        mPullAnimation.setFillAfter(true);
        mReturnAnimation = AnimationUtils.loadAnimation(mContext, R.anim.arrow_rotate_return);
        mReturnAnimation.setFillAfter(true);

        measureView(mHeaderView);
        mHeaderHeight = mHeaderView.getMeasuredHeight();
        mHeaderView.setPadding(0, -1 * mHeaderHeight, 0, 0);//通过设置padding达到将头部隐藏的效果
        mHeaderView.invalidate();
        addHeaderView(mHeaderView, null, false);

        mFooterView = LayoutInflater.from(mContext).inflate(R.layout.footer_view, null);
        mFooterTv = (TextView) mFooterView.findViewById(R.id.tv_footer);
        mFooterProgressIv = (ImageView) mFooterView.findViewById(R.id.iv_footer_progress);
        mFooterDrawable = (AnimationDrawable) mFooterProgressIv.getDrawable();
        mFooterProgressIv.setVisibility(GONE);

        measureView(mFooterView);
        mFooterHeight = mFooterView.getMeasuredHeight();
        mFooterView.setPadding(0, 0, 0, -1 * mFooterHeight);
        addFooterView(mFooterView, null, false);


        mCanRefresh = true;
        mCanLoad = false;
        setOnScrollListener(this);
    }


    //测量View宽高
    private void measureView(View child){
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if(params == null){
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, params.width);
        int lpHeight = params.height;
        int childHeightSpec;
        if(lpHeight > 0){
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        }else{
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mState != REFRESHING) {
            if (mCanRefresh) {
                handleHeaderTouchEvent(ev);
            } else if (mCanLoad) {
                handleFooterViewTouchEvent(ev);
            }
        }
        return super.onTouchEvent(ev);
    }

    private void handleHeaderTouchEvent(MotionEvent ev){
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartY = ev.getY();
                mState = PULL_TO_REFRESH;
                break;
            case MotionEvent.ACTION_MOVE:

                float distance = (ev.getY() - mStartY) / mScale;
                if (distance > 0 || mState != DONE) {
                    setSelection(0);//设置当前位置为0，防止列表跟着一起滚动
                    if (distance < mHeaderHeight) {
                        mHeaderTv.setText("下拉刷新");
                        if (mState == RELEASE_TO_REFRESH) {
                            mArrowIv.startAnimation(mReturnAnimation);
                        }
                        mState = PULL_TO_REFRESH;
                        mHeaderView.setPadding(0, (int) (distance - mHeaderHeight), 0, 0);//下拉过程中慢慢将头部展示出来
                    } else {
                        if (mState == PULL_TO_REFRESH) {
                            mArrowIv.startAnimation(mPullAnimation);
                        }
                        mState = RELEASE_TO_REFRESH;
                        mHeaderTv.setText("松开刷新");
                        //随着下拉增加头部高度,这里可以选择设置padding和改变高度两种方式
                        //设置padding：
                        mHeaderView.setPadding(0, (int) ((distance - mHeaderHeight) / 2), 0, (int) ((distance - mHeaderHeight) / 2));
                    }
                    mHeaderView.invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mState == RELEASE_TO_REFRESH) {
                    mState = REFRESHING;
                } else {
                    if (mState == PULL_TO_REFRESH) {
                        mState = DONE;
                    }
                }
                handleHeaderViewByState();
                break;
        }
    }

    private void handleHeaderViewByState(){
        switch (mState) {
            case REFRESHING:
                mHeaderTv.setText("正在刷新");
                mHeaderView.setPadding(0, 0, 0, 0);
                mHeaderProgressIv.setVisibility(VISIBLE);
                mHeaderDrawable.start();
                mArrowIv.clearAnimation();
                mArrowIv.setVisibility(GONE);
                mHeaderView.invalidate();
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefresh();
                }
                break;
            case DONE:
                mHeaderView.setPadding(0, -1 * mHeaderHeight, 0, 0);
                mHeaderView.invalidate();
                break;
        }
    }


    private void handleFooterViewTouchEvent(MotionEvent ev){
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartY = ev.getY();
                mState = PULL_TO_REFRESH;
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = (mStartY - ev.getY()) / mScale;

                if (distance > 0 || mState != DONE) {
                    if (distance < mFooterHeight) {
                        mFooterTv.setText("上拉加载更多");
                        mState = PULL_TO_REFRESH;
                        mFooterView.setPadding(0, 0, 0, (int) (distance - mFooterHeight));
                    } else {
                        mFooterTv.setText("松开加载");
                        mState = RELEASE_TO_REFRESH;
                        mFooterView.setPadding(0, (int) ((distance - mFooterHeight) / 2), 0, (int) ((distance - mFooterHeight) / 2));
                    }
                    mFooterView.invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mState == RELEASE_TO_REFRESH) {
                    mState = REFRESHING;
                } else {
                    if (mState == PULL_TO_REFRESH) {
                        mState = DONE;
                    }
                }
                handleFooterViewByState();
                break;
        }
    }


    private void handleFooterViewByState(){
        switch (mState) {
            case REFRESHING:
                mFooterTv.setText("正在加载");
                mFooterView.setPadding(0, 0, 0, 0);
                mFooterProgressIv.setVisibility(VISIBLE);
                mFooterDrawable.start();
                mFooterView.invalidate();
                if (mOnLoadListener != null) {
                    mOnLoadListener.onLoad();
                }
                break;
            case DONE:
                mFooterView.setPadding(0, 0, 0, -1 * mFooterHeight);
                mFooterView.invalidate();
                break;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mCanRefresh = firstVisibleItem == 0 ? true : false;
        mCanLoad = (getLastVisiblePosition() == totalItemCount - 1 && visibleItemCount < totalItemCount) ? true : false;
        Log.i("flag", mCanRefresh + "  " + mCanLoad);
    }

    public void refreshComplete(){
        mHeaderTv.setText("刷新成功");
        mHeaderProgressIv.setVisibility(GONE);
        mHeaderDrawable.stop();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHeaderView.setPadding(0, -1 * mHeaderHeight, 0, 0);
                mHeaderView.invalidate();
                mState = DONE;
                mArrowIv.setVisibility(VISIBLE);
            }
        }, 1000);

    }

    public void loadComplete(){
        mFooterTv.setText("加载成功");
        mFooterProgressIv.setVisibility(GONE);
        mFooterDrawable.stop();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFooterView.setPadding(0, 0, 0, -1 * mFooterHeight);
                mFooterView.invalidate();
                mState = DONE;
            }
        }, 1000);
    }

    public interface OnRefreshListener{
        void onRefresh();
    }

    /**
     * 设置刷新操作回调方法
     * @param onRefreshListener
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener){
        mOnRefreshListener = onRefreshListener;
    }

    public interface OnLoadListener{
        void onLoad();
    }

    public void setOnLoadListener(OnLoadListener onLoadListener){
        mOnLoadListener = onLoadListener;
    }

}

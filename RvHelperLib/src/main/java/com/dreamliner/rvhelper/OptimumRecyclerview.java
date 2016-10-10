package com.dreamliner.rvhelper;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;

import com.dreamliner.loadmore.LoadMoreHandler;
import com.dreamliner.loadmore.LoadMoreRecycleViewContainer;
import com.dreamliner.loadmore.LoadMoreUIHandler;
import com.dreamliner.ptrlib.PtrClassicFrameLayout;
import com.dreamliner.ptrlib.PtrDefaultHandler;
import com.dreamliner.ptrlib.PtrFrameLayout;
import com.dreamliner.ptrlib.PtrHandler;
import com.dreamliner.ptrlib.PtrUIHandler;
import com.dreamliner.rvhelper.adapter.BaseDataAdapter;
import com.dreamliner.rvhelper.empty.EmptyLayout;
import com.dreamliner.rvhelper.interfaces.OnRefreshListener;
import com.dreamliner.rvhelper.loading.LoadingLayout;
import com.dreamliner.rvhelper.util.FloatUtil;

import static com.dreamliner.rvhelper.util.LayoutManagerUtil.getFirstVisibleItemPosition;
import static com.dreamliner.rvhelper.util.LayoutManagerUtil.getLastVisibleItemPosition;

public class OptimumRecyclerview extends FrameLayout {

    private static final String TAG = "OptimumRecyclerview";

    //主界面
    private PtrClassicFrameLayout mPtrLayout;
    private LoadMoreRecycleViewContainer mLoadmoreContainer;
    private RecyclerView mRecyclerView;

    //下拉刷新回调
    private OnRefreshListener mOnRefreshListener;
    private LoadMoreHandler mLoadMoreHandler;

    //加载中页面
    private boolean isLoading = false;
    private ViewStub mLoadingViewStub;
    private LoadingLayout mLoadingLayout;

    //空白页面
    private ViewStub mEmptyViewStub;
    private EmptyLayout mEmptyLayout;

    private int mEmptyId;
    private int mLoadingId;
    private boolean mClipToPadding;
    private int mPadding;
    private int mPaddingTop;
    private int mPaddingBottom;
    private int mPaddingLeft;
    private int mPaddingRight;
    private int mScrollbarStyle;

    //下拉刷新的头部相关信息
    private int mPtrBgColor;
    private int mDurationToClose;
    private int mDurationToCloseHeader;
    private boolean mKeepHeaderWhenREfresh;
    private boolean mPullToFresh;
    private float mRatioOfHedaerHeightToRefresh;
    private float mResistance;


    private boolean isRecyclerMove = false;
    private int mRecyclerviewIndex = 0;

    public PtrClassicFrameLayout getPtrLayout() {
        return mPtrLayout;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public OptimumRecyclerview(Context context) {
        super(context);
        initView();
    }

    public OptimumRecyclerview(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initView();
    }

    public OptimumRecyclerview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
        initView();
    }

    protected void initAttrs(AttributeSet attrs) {
        TypedArray optimumRvArr = getContext().obtainStyledAttributes(attrs, R.styleable.OptimumRecyclerview);
        TypedArray ptrArr = getContext().obtainStyledAttributes(attrs, R.styleable.PtrFrameLayout);

        try {
            //初始化rv相关
            mEmptyId = optimumRvArr.getResourceId(R.styleable.OptimumRecyclerview_rvhelp_layout_empty, R.layout.layout_default_empty);
            mLoadingId = optimumRvArr.getResourceId(R.styleable.OptimumRecyclerview_rvhelp_layout_loading,
                    R.layout.layout_default_loading);

            mClipToPadding = optimumRvArr.getBoolean(R.styleable.OptimumRecyclerview_rvhelp_recyclerClipToPadding, false);
            mPadding = (int) optimumRvArr.getDimension(R.styleable.OptimumRecyclerview_rvhelp_recyclerPadding, -1.0f);
            mPaddingTop = (int) optimumRvArr.getDimension(R.styleable.OptimumRecyclerview_rvhelp_recyclerPaddingTop, 0.0f);
            mPaddingBottom = (int) optimumRvArr.getDimension(R.styleable.OptimumRecyclerview_rvhelp_recyclerPaddingBottom, 0.0f);
            mPaddingLeft = (int) optimumRvArr.getDimension(R.styleable.OptimumRecyclerview_rvhelp_recyclerPaddingLeft, 0.0f);
            mPaddingRight = (int) optimumRvArr.getDimension(R.styleable.OptimumRecyclerview_rvhelp_recyclerPaddingRight, 0.0f);
            mScrollbarStyle = optimumRvArr.getInt(R.styleable.OptimumRecyclerview_rvhelp_scrollbarStyle, -1);

            //初始化uptr相关
            mPtrBgColor = ptrArr.getInt(R.styleable.PtrFrameLayout_ptr_bg_color, 0xf1f1f1);
            mDurationToClose = ptrArr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_close, 200);
            mDurationToCloseHeader = ptrArr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_close_header, 1000);
            mKeepHeaderWhenREfresh = ptrArr.getBoolean(R.styleable.PtrFrameLayout_ptr_duration_to_close, true);
            mPullToFresh = ptrArr.getBoolean(R.styleable.PtrFrameLayout_ptr_duration_to_close, false);
            mRatioOfHedaerHeightToRefresh = ptrArr.getFloat(R.styleable.PtrFrameLayout_ptr_ratio_of_header_height_to_refresh, 1.2f);
            mResistance = ptrArr.getFloat(R.styleable.PtrFrameLayout_ptr_resistance, 1.7f);
        } finally {
            optimumRvArr.recycle();
            ptrArr.recycle();
        }
    }

    private void initView() {
        if (isInEditMode()) {
            return;
        }
        View v = LayoutInflater.from(getContext()).inflate(R.layout.layout_rvhelper, this);

        //初始化加载中界面
        mLoadingViewStub = (ViewStub) v.findViewById(R.id.loading_viewstub);
        mLoadingViewStub.setLayoutResource(mLoadingId);
        if (0 != mLoadingId) {
            View loadingView = mLoadingViewStub.inflate();
            if (loadingView instanceof LoadingLayout) {
                mLoadingLayout = (LoadingLayout) loadingView;
            }
        }

        //初始化空白页面界面
        mEmptyViewStub = (ViewStub) v.findViewById(R.id.empty_viewstub);
        mEmptyViewStub.setLayoutResource(mEmptyId);
        if (0 != mEmptyId) {
            View emptyView = mEmptyViewStub.inflate();
            if (emptyView instanceof EmptyLayout) {
                mEmptyLayout = (EmptyLayout) emptyView;
            }
        }

        initPtrView(v);
        initRecyclerView(v);
        initLoadmoreView(v);

        //默认先显示加载中界面
        showLoadingView();
    }

    private void initPtrView(View v) {
        mPtrLayout = (PtrClassicFrameLayout) v.findViewById(R.id.ptr_layout);
        mPtrLayout.setEnabled(false);
        mPtrLayout.setBackgroundColor(mPtrBgColor);
        mPtrLayout.setDurationToClose(mDurationToClose);
        mPtrLayout.setDurationToCloseHeader(mDurationToCloseHeader);
        mPtrLayout.setKeepHeaderWhenRefresh(mKeepHeaderWhenREfresh);
        mPtrLayout.setPullToRefresh(mPullToFresh);
        mPtrLayout.setRatioOfHeaderHeightToRefresh(mRatioOfHedaerHeightToRefresh);
        mPtrLayout.setResistance(mResistance);

        mPtrLayout.setLastUpdateTimeRelateObject(this);
    }

    /**
     * Implement this method to customize the AbsListView
     */
    protected void initRecyclerView(View view) {
        View recyclerView = view.findViewById(android.R.id.list);

        if (recyclerView instanceof RecyclerView)
            mRecyclerView = (RecyclerView) recyclerView;
        else
            throw new IllegalArgumentException("OptimumRecyclerview works with a RecyclerView!");

        mRecyclerView.setClipToPadding(mClipToPadding);

        if (!FloatUtil.compareFloats(mPadding, -1.0f)) {
            mRecyclerView.setPadding(mPadding, mPadding, mPadding, mPadding);
        } else {
            mRecyclerView.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
        }
        if (mScrollbarStyle != -1) {
            mRecyclerView.setScrollBarStyle(mScrollbarStyle);
        }

        mRecyclerView.addOnScrollListener(new CustomOnScrollListener());
    }

    private void initLoadmoreView(View v) {
        mLoadmoreContainer = (LoadMoreRecycleViewContainer) v.findViewById(R.id.loadmore_container);
        mLoadmoreContainer.setEnableLoadmore(false);
    }

    /**
     * Set the layout manager to the recycler
     */
    public void setLayoutManager(RecyclerView.LayoutManager manager) {
        mRecyclerView.setLayoutManager(manager);
    }

    /**
     * Set the adapter to the recyclerview
     */
    public void setAdapter(RecyclerView.Adapter adapter) {

        if (null != adapter) {
            mRecyclerView.setAdapter(adapter);
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    loadEmptyView();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    super.onItemRangeChanged(positionStart, itemCount);
                    loadEmptyView();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    super.onItemRangeChanged(positionStart, itemCount, payload);
                    loadEmptyView();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    super.onItemRangeInserted(positionStart, itemCount);
                    loadEmptyView();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    super.onItemRangeRemoved(positionStart, itemCount);
                    loadEmptyView();
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                    loadEmptyView();
                }

                private void loadEmptyView() {
                    Log.i(TAG, "try loadEmptyView and hide the hideProgress");

                    hideLoadingView();
                    mPtrLayout.refreshComplete();
                    if (null != mEmptyLayout) {
                        if (getAdapter().getItemCount() == 0) {
                            showEmptyView();
                        } else {
                            hideEmptyView();
                        }
                    }
                }
            });
        }
    }

    /**
     * Add the onItemTouchListener for the recycler
     */
    public void addOnItemTouchListener(RecyclerView.OnItemTouchListener listener) {
        mRecyclerView.addOnItemTouchListener(listener);
    }

    /**
     * Remove the onItemTouchListener for the recycler
     */
    public void removeOnItemTouchListener(RecyclerView.OnItemTouchListener listener) {
        mRecyclerView.removeOnItemTouchListener(listener);
    }

    /**
     * @return the recyclerview adapter
     */
    public RecyclerView.Adapter getAdapter() {
        return mRecyclerView.getAdapter();
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return mRecyclerView.getLayoutManager();
    }

    /**
     * Set the onRefresh listener
     */
    public void setRefreshListener(OnRefreshListener onRefreshListener) {
        mPtrLayout.setEnabled(true);
        mOnRefreshListener = onRefreshListener;
        mPtrLayout.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                if (null != mOnRefreshListener) {
                    mOnRefreshListener.onRefresh(frame);
                }
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return !isLoading && PtrDefaultHandler.checkContentCanBePulledDown(frame, mRecyclerView, header);
            }
        });
    }

    public void setRefreshListener(OnRefreshListener onRefreshListener, @NonNull View headerView) {
        setRefreshListener(onRefreshListener);
        setHeaderView(headerView);
    }

    private void setHeaderView(@NonNull View headerView) {
        if (!(headerView instanceof PtrUIHandler)) {
            throw new RuntimeException("headerView must implements PtrUIHandler");
        }
        mPtrLayout.setEnabled(true);
        mPtrLayout.setHeaderView(headerView);
        mPtrLayout.addPtrUIHandler((PtrUIHandler) headerView);
    }

    public OnRefreshListener getOnRefreshListener() {
        return mOnRefreshListener;
    }

    public void refreshComplete() {
        mPtrLayout.refreshComplete();
    }

    public LoadMoreHandler getLoadMoreHandler() {
        return mLoadMoreHandler;
    }

    public void setLoadMoreHandler(LoadMoreHandler loadMoreHandler) {
        mLoadMoreHandler = loadMoreHandler;
        if (null == mLoadmoreContainer.getFooterView()) {
            setDefaultLoadMoreHandler(loadMoreHandler);
        } else {
            mLoadmoreContainer.setLoadMoreHandler(mLoadMoreHandler);
        }
    }

    public void setDefaultLoadMoreHandler(LoadMoreHandler loadMoreHandler) {
        mLoadMoreHandler = loadMoreHandler;

        //配置loadmore
        mLoadmoreContainer.setEnableLoadmore(true);
        mLoadmoreContainer.setRecyclerViewAdapter((BaseDataAdapter<?, ?>) mRecyclerView.getAdapter());
        mLoadmoreContainer.useDefaultFooter();
        mLoadmoreContainer.setAutoLoadMore(true);
        mLoadmoreContainer.setShowLoadingForFirstPage(true);
        mLoadmoreContainer.setLoadMoreHandler(mLoadMoreHandler);
    }

    public void setLoadMoreHandler(LoadMoreHandler loadMoreHandler, View loadmoreView) {
        if (!(loadmoreView instanceof LoadMoreUIHandler)) {
            throw new RuntimeException("loadmoreview must implements LoadMoreUIHandler");
        }

        mLoadMoreHandler = loadMoreHandler;
        //配置loadmore
        mLoadmoreContainer.setEnableLoadmore(true);
        mLoadmoreContainer.setRecyclerViewAdapter((BaseDataAdapter<?, ?>) mRecyclerView.getAdapter());

        mLoadmoreContainer.setLoadMoreView(loadmoreView);
        mLoadmoreContainer.setLoadMoreUIHandler((LoadMoreUIHandler) loadmoreView);

        mLoadmoreContainer.setAutoLoadMore(true);
        mLoadmoreContainer.setShowLoadingForFirstPage(true);
        mLoadmoreContainer.setLoadMoreHandler(mLoadMoreHandler);
    }

    public void setNumberBeforeMoreIsCalled(int max) {
        mLoadmoreContainer.setItemLeftToLoadMore(max);
    }

    public void loadMoreFinish(boolean emptyResult, boolean hasMore) {
        mLoadmoreContainer.loadMoreFinish(emptyResult, hasMore);
    }

    public void setOnTouchListener(OnTouchListener listener) {
        mRecyclerView.setOnTouchListener(listener);
    }

    public void addItemDecoration(RecyclerView.ItemDecoration itemDecoration) {
        mRecyclerView.addItemDecoration(itemDecoration);
    }

    public void addItemDecoration(RecyclerView.ItemDecoration itemDecoration, int index) {
        mRecyclerView.addItemDecoration(itemDecoration, index);
    }

    public void removeItemDecoration(RecyclerView.ItemDecoration itemDecoration) {
        mRecyclerView.removeItemDecoration(itemDecoration);
    }

    public void showLoadingView() {
        isLoading = true;
        mLoadingViewStub.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        mEmptyViewStub.setVisibility(View.GONE);
        doDefaultLoadingView(true);
    }

    private void hideLoadingView() {
        isLoading = false;
        mLoadingViewStub.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mEmptyViewStub.setVisibility(View.VISIBLE);
        doDefaultLoadingView(false);
    }

    private void doDefaultLoadingView(boolean isLoading) {
        if (null == mLoadingLayout) {
            return;
        }
        if (isLoading) {
            mLoadingLayout.onShowLoading();
        } else {
            mLoadingLayout.onHideLoading();
        }
    }

    public void setEmptyType(int type) {
        if (null == mEmptyLayout) {
            return;
        }
        mEmptyLayout.setEmptyType(type);
    }

    private void showEmptyView() {
        mRecyclerView.setVisibility(View.GONE);
        mEmptyViewStub.setVisibility(View.VISIBLE);
    }

    private void hideEmptyView() {
        mRecyclerView.setVisibility(View.VISIBLE);
        mEmptyViewStub.setVisibility(View.GONE);
    }

    /**
     * @return inflated progress view or null
     */
    public LoadingLayout getLoadingLayout() {
        return mLoadingLayout;
    }

    /**
     * @return inflated empty view or null
     */
    public EmptyLayout getEmptyLayout() {
        return mEmptyLayout;
    }

    public void move(int n) {
        move(n, true);
    }

    public void move(int n, boolean smooth) {
        if (n < 0 || n >= getAdapter().getItemCount()) {
            Log.e(TAG, "move: index error");
            return;
        }
        mRecyclerviewIndex = n;
        mRecyclerView.stopScroll();
        if (smooth) {
            smoothMoveToPosition(n);
        } else {
            moveToPosition(n);
        }
    }

    private void moveToPosition(int n) {

        int firstItem = getFirstVisibleItemPosition(getLayoutManager());
        int lastItem = getLastVisibleItemPosition(getLayoutManager());
        if (n <= firstItem) {
            mRecyclerView.scrollToPosition(n);
        } else if (n <= lastItem) {
            int top = mRecyclerView.getChildAt(n - firstItem).getTop();
            mRecyclerView.smoothScrollBy(0, top);
        } else {
            mRecyclerView.scrollToPosition(n);
            isRecyclerMove = true;
        }
    }

    private void smoothMoveToPosition(int n) {

        int firstItem = getFirstVisibleItemPosition(getLayoutManager());
        int lastItem = getLastVisibleItemPosition(getLayoutManager());
        if (n <= firstItem) {
            mRecyclerView.smoothScrollToPosition(n);
        } else if (n <= lastItem) {
            int top = mRecyclerView.getChildAt(n - firstItem).getTop();
            mRecyclerView.smoothScrollBy(0, top);
        } else {
            mRecyclerView.smoothScrollToPosition(n);
            isRecyclerMove = true;
        }
    }

    public void setEmptyOnClick(OnClickListener emptyOnClick) {
        if (null != mEmptyLayout) {
            mEmptyLayout.setOnClickListener(emptyOnClick);
        }
    }

    class CustomOnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (isRecyclerMove && newState == RecyclerView.SCROLL_STATE_IDLE) {
                isRecyclerMove = false;
                int n = mRecyclerviewIndex - getFirstVisibleItemPosition(getLayoutManager());
                if (0 <= n && n < mRecyclerView.getChildCount()) {
                    int top = mRecyclerView.getChildAt(n).getTop();
                    mRecyclerView.smoothScrollBy(0, top);
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (isRecyclerMove) {
                isRecyclerMove = false;
                int n = mRecyclerviewIndex - getFirstVisibleItemPosition(getLayoutManager());
                if (0 <= n && n < mRecyclerView.getChildCount()) {
                    int top = mRecyclerView.getChildAt(n).getTop();
                    mRecyclerView.scrollBy(0, top);
                }
            }
        }
    }
}

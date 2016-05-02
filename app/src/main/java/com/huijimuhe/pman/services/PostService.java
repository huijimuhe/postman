package com.huijimuhe.pman.services;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.huijimuhe.pman.utils.PowerUtil;

import java.util.ArrayList;
import java.util.List;

public class PostService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PostService";

    private static final String MAIN_ACT = "MainActivity";
    private static final String DETAIL_ACT = "NewsPageActivity";
    private static final String BASE_ACT = "BaseActivity";

    private static final int MSG_BACK = 159;
    private static final int MSG_REFRESH_NEW_LIST = 707;
    private static final int MSG_READ_NEWS = 19;
    private static final int MSG_POST_COMMENT = 211;
    private static final int MSG_REFRESH_COMPLETE = 22;
    private static final int MSG_FINISH_COMMENT = 59;

    private String currentActivityName = MAIN_ACT;
    private HandlerEx mHandler = new HandlerEx();

    private boolean mIsMutex = false;
    private int mReadCount = 0;
    private List<String> readedNews = new ArrayList<>();
    private PowerUtil powerUtil;
    private SharedPreferences sharedPreferences;

    /**
     * AccessibilityEvent
     *
     * @param event 事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (sharedPreferences == null) return;

        setCurrentActivityName(event);
        watchMain(event);
        watchBasic(event);
        watchDetail(event);
    }

    private void watchMain(AccessibilityEvent event) {
        //新闻列表
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && currentActivityName.contains(MAIN_ACT)) {
            if (mReadCount > 4) {
                //如果读取完了都没有新的就刷新
                Log.d(TAG, "新闻已读取完,需要刷新列表");
                //需要刷新列表了
                mHandler.sendEmptyMessage(MSG_REFRESH_NEW_LIST);
            } else {
                mHandler.sendEmptyMessage(MSG_READ_NEWS);
            }
        }
    }

    private void watchDetail(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && currentActivityName.contains(DETAIL_ACT)) {
            //添加评论
            mHandler.sendEmptyMessage(MSG_POST_COMMENT);
        }
    }

    private void watchBasic(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && currentActivityName.contains(BASE_ACT)) {
            Log.d(TAG, "进入非新闻页,即将退出");
            mHandler.sendEmptyMessage(MSG_BACK);
            mHandler.sendEmptyMessage(MSG_BACK);
        }
    }

    private void refreshList() {
        List<AccessibilityNodeInfo> nodes = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("android:id/list");
        for (AccessibilityNodeInfo node : nodes) {
            //页面是否加载完成
            if (node == null) return;
            //执行刷新
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        //重新开始读取新闻
        mHandler.sendEmptyMessage(MSG_REFRESH_COMPLETE);
    }

    private void enterDetailAct() {

        //获取列表items
        List<AccessibilityNodeInfo> nodes = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.netease.newsreader.activity:id/perfect_item");

        for (AccessibilityNodeInfo node : nodes) {
            //页面是否加载完成
            if (node == null) return;

            //获取列表item的标题
            List<AccessibilityNodeInfo> titles = node.findAccessibilityNodeInfosByViewId("com.netease.newsreader.activity:id/title");

            for (AccessibilityNodeInfo title : titles) {

                //检查是否已读取
                if (!readedNews.contains(title.getText().toString())) {
                    //点击读取该新闻
                    readedNews.add(title.getText().toString());
                    node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "进入新闻:" + title.getText().toString());
                    mReadCount++;
                    //进入一个就停止
                    return;
                }
            }
        }
    }

    private void postComment() {
        //激活输入框
        List<AccessibilityNodeInfo> nodes = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.netease.newsreader.activity:id/mock_reply_edit");
        for (AccessibilityNodeInfo node : nodes) {

            //页面是否加载完成
            if (node == null) return;

            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        //输入内容
        List<AccessibilityNodeInfo> editNodes = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.netease.newsreader.activity:id/reply_edit");
        for (AccessibilityNodeInfo node : editNodes) {

            //页面是否加载完成
            if (node == null) return;

            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "抽烟的人最讨厌了");
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        }

//        //回复按钮
//        List<AccessibilityNodeInfo> postNodes = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.netease.newsreader.activity:id/reply");
//        for (AccessibilityNodeInfo node : postNodes) {
//           //页面是否加载完成
//           if (node == null) return;
//           node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        }

        //退出
        mHandler.sendEmptyMessage(MSG_FINISH_COMMENT);

        Log.d(TAG, "评论已发表");
    }

    /**
     * 设置当前页面名称
     *
     * @param event
     */
    private void setCurrentActivityName(AccessibilityEvent event) {

        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        try {
            ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());

            getPackageManager().getActivityInfo(componentName, 0);
            currentActivityName = componentName.flattenToShortString();
            Log.d(TAG, "<--pkgName-->" + event.getPackageName().toString());
            Log.d(TAG, "<--className-->" + event.getClassName().toString());
            Log.d(TAG, "<--currentActivityName-->" + currentActivityName);
        } catch (PackageManager.NameNotFoundException e) {
            currentActivityName = MAIN_ACT;
        }
    }

    @Override
    public void onDestroy() {
        this.powerUtil.handleWakeLock(false);
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
    }

    /**
     * 屏幕是否常亮
     */
    private void watchFlagsFromPreference() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.powerUtil = new PowerUtil(this);
        Boolean watchOnLockFlag = sharedPreferences.getBoolean("pref_watch_on_lock", false);
        this.powerUtil.handleWakeLock(watchOnLockFlag);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_watch_on_lock")) {
            Boolean changedValue = sharedPreferences.getBoolean(key, false);
            this.powerUtil.handleWakeLock(changedValue);
        }
    }

    /**
     * 处理机
     */
    private class HandlerEx extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //后退
                case MSG_BACK:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            performGlobalAction(GLOBAL_ACTION_BACK);
                        }
                    }, 1000);
                    break;
                //结束评论
                case MSG_FINISH_COMMENT:
                    for (int i = 0; i < 4; i++) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                performGlobalAction(GLOBAL_ACTION_BACK);
                            }
                        }, 2000 +i*500);
                    }
                    break;
                //刷新列表
                case MSG_REFRESH_NEW_LIST:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshList();
                        }
                    }, 3000);
                    break;
                //结束刷新
                case MSG_REFRESH_COMPLETE:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mReadCount = 0;
                            enterDetailAct();
                        }
                    }, 3000);
                    break;
                //进入新闻页
                case MSG_READ_NEWS:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            enterDetailAct();
                        }
                    }, 3000);
                    break;
                //发送评论
                case MSG_POST_COMMENT:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            postComment();
                        }
                    }, 3000);
                    break;
            }
        }
    }
}

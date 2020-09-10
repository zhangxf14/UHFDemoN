package com.uhf.uhf.activity.setpage;

import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.reader.base.ReaderBase;
import com.reader.helper.ISO180006BOperateTagBuffer;
import com.reader.helper.InventoryBuffer;
import com.reader.helper.OperateTagBuffer;
import com.reader.helper.ReaderHelper;
import com.reader.helper.ReaderSetting;
import com.uhf.uhf.R;
import com.uhf.uhf.UHFApplication;
import com.uhf.uhf.activity.BaseActivity;
import com.uhf.uhf.fragment.FragmentMessageListener;
import com.uhf.uhf.fragment.PageReaderFuDanCmdFragment;
import com.uhf.uhf.fragment.TestTempInventoryFragment;
import com.uhf.uhf.widget.PopupMenu;
import com.uhf.uhf.widget.tagpage.PageInventoryReal;
import com.uhf.uhf.widget.tagpage.PageInventoryReal6B;
import com.util.ExcelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PageReaderFuDanTestTemp extends BaseActivity implements FragmentMessageListener {

    private ReaderHelper mReaderHelper;
    private ReaderBase mReader;

    private ReaderSetting m_curReaderSetting;
    private InventoryBuffer m_curInventoryBuffer;
    private OperateTagBuffer m_curOperateTagBuffer;
    private ISO180006BOperateTagBuffer m_curOperateTagISO18000Buffer;

    private ViewPager viewPager;
    private List<Fragment> fragments = new ArrayList<>();

    private TextView[] title = new TextView[2];
    private int currIndex = 0;

    public String epc = "";
    private boolean isClearMask = false;

    PageReaderFuDanCmdFragment fuDanCmdFragment ;
    TestTempInventoryFragment tempInventoryFragment ;

    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onResume() {
        if (mReader != null) {
            if (!mReader.IsAlive())
                mReader.StartWait();
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReader != null) {
            mReader.clearTagMask((byte)0xFF,(byte)0x00);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_fudan_test_temperature);
        ((UHFApplication) getApplication()).addActivity(this);
        // Storage Permissions
        ExcelUtils.verifyStoragePermissions(this);
        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
            mReader = mReaderHelper.getReader();
        } catch (Exception e) {
            e.printStackTrace();
        }

        m_curReaderSetting = mReaderHelper.getCurReaderSetting();
        m_curInventoryBuffer = mReaderHelper.getCurInventoryBuffer();
        m_curOperateTagBuffer = mReaderHelper.getCurOperateTagBuffer();
        m_curOperateTagISO18000Buffer = mReaderHelper.getCurOperateTagISO18000Buffer();
        initView();
    }

    private void initView() {
        viewPager = findViewById(R.id.vPager);
        viewPager.setCurrentItem(0);

        fuDanCmdFragment = new PageReaderFuDanCmdFragment();
        tempInventoryFragment = new TestTempInventoryFragment();
        tempInventoryFragment.setFragmentMessageListener(this);
        fragments.add(tempInventoryFragment);
        fragments.add(fuDanCmdFragment);
        viewPager.setAdapter(new MyViewPagerAdapter(getSupportFragmentManager(),fragments));
        viewPager.setOnPageChangeListener(new MyViewPagerChang());

        title[0] = findViewById(R.id.tab_index1);
        title[1] = findViewById(R.id.tab_index2);

        title[0].setOnClickListener(new MyOnClickListener(0));
        title[1].setOnClickListener(new MyOnClickListener(1));
    }

    class MyViewPagerAdapter extends FragmentStatePagerAdapter {
        private List<Fragment> fragments;
        private FragmentManager fragmentManager;

        MyViewPagerAdapter(FragmentManager fragmentManager,List<Fragment> fragments) {
            super(fragmentManager);
            this.fragmentManager = fragmentManager;
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

    }

    class MyViewPagerChang implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            //fuDanCmdFragment.setEpc("");
        }

        @Override
        public void onPageSelected(int position) {
            currIndex = position;
            if (0 == currIndex) {
                title[1].setBackgroundResource(R.drawable.btn_select_background_select_left_down);
                title[0].setBackgroundResource(R.drawable.btn_select_background_select_right);
            } else {
                title[1].setBackgroundResource(R.drawable.btn_select_background_select_left);
                title[0].setBackgroundResource(R.drawable.btn_select_background_select_right_down);
            }
            title[1 - currIndex].setTextColor(Color.rgb(0x00, 0xBB, 0xF7));
            title[currIndex].setTextColor(Color.rgb(0xFF, 0xFF, 0xFF));
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    public class MyOnClickListener implements View.OnClickListener {
        private int index = 0;
        public MyOnClickListener(int i) {
            index = i;
        }
        @Override
        public void onClick(View v) {
            if (!isClearMask) {
                isClearMask = true;
                mReader.clearTagMask((byte)0xFF,(byte)0x00);
                fuDanCmdFragment.setEpc("");
            }
            viewPager.setCurrentItem(index);
        }
    }

    @Override
    public void sendMessage(String message) {
        if (isClearMask) {
            isClearMask = false;
            return;
        }
        epc = message;
        Log.d(TAG,"EPC:" + epc);
        viewPager.setCurrentItem(1);
        fuDanCmdFragment.setEpc(epc);
    }

    //环境在Activity中
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean ret = false;
        ret = activityParseOnkey(keyCode);
        if (!ret) {
            ret = tempInventoryFragment.onKeyDown(event);  //这里的mCurFragment是我们前的Fragment
        }
        return ret;
    }

    private boolean activityParseOnkey(int keyCode) {
        boolean ret = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                break;
            case KeyEvent.KEYCODE_BACK:
                break;
        }
        return ret;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean ret = false;
        ret = activityParseOnkey(keyCode);
        if (!ret) {
            ret = tempInventoryFragment.onKeyUp(event);  //这里的mCurFragment是我们前的Fragment
        }
        return ret;
    }

}

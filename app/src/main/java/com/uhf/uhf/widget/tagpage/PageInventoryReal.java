package com.uhf.uhf.widget.tagpage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.reader.base.CMD;
import com.reader.base.ERROR;
import com.reader.base.ReaderBase;
import com.reader.helper.ISO180006BOperateTagBuffer;
import com.reader.helper.InventoryBuffer;
import com.reader.helper.OperateTagBuffer;
import com.reader.helper.ReaderHelper;
import com.reader.helper.ReaderSetting;
import com.uhf.uhf.widget.LogList;
import com.uhf.uhf.R;
import com.uhf.uhf.R.id;
import com.uhf.uhf.R.layout;
import com.uhf.uhf.widget.TagRealList;
import com.uhf.uhf.adapter.AbstractSpinerAdapter.IOnItemSelectListener;
import com.uhf.uhf.dialog.SpinerPopWindow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PageInventoryReal extends LinearLayout {
    private static final String TAG = "PageInventory";
    private static final boolean DEBUG = true;

    public boolean mKeyF4Pressing = false;
    // fixed by lei.li 2016/11/09
    // private LogList mLogList;
    private LogList mLogList;
    // fixed by lei.li 2016/11/09
    private TextView mStartStop;
    // private TextView mRefreshButton;
    //private LinearLayout mLayoutRealSet;
    private TextView mSessionIdTextView, mInventoriedFlagTextView;
    private TableRow mDropDownRow1, mDropDownRow2;
    //private CheckBox mCbRealSet, mCbRealSession;
    private List<String> mSessionIdList;
    private List<String> mInventoriedFlagList;
    private SpinerPopWindow mSpinerPopWindow1, mSpinerPopWindow2;
    private EditText mRealRoundEditText;
    private TagRealList mTagRealList;
    private ReaderHelper mReaderHelper;
    private ReaderBase mReader;
    private int mPos1 = -1, mPos2 = -1;
    private static ReaderSetting m_curReaderSetting;
    private static InventoryBuffer m_curInventoryBuffer;
    private static OperateTagBuffer m_curOperateTagBuffer;
    private static ISO180006BOperateTagBuffer m_curOperateTagISO18000Buffer;
    private TextView mTagsCountText, mTagsTotalText;
    private TextView mTagsSpeedText, mTagsTimeText, mTagsOpTimeText;
    private LocalBroadcastManager lbm;
    private long mRefreshTime;
    private Context mContext;

    private boolean mDefault = true;

    private RadioGroup mSelectCommand;
    private int mSelectCmdValue = 0;

    public PageInventoryReal(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context)
                .inflate(layout.page_inventory_real, this);
        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
            mReader = mReaderHelper.getReader();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mSessionIdList = new ArrayList<String>();
        mInventoriedFlagList = new ArrayList<String>();
        m_curReaderSetting = mReaderHelper.getCurReaderSetting();
        m_curInventoryBuffer = mReaderHelper.getCurInventoryBuffer();
        m_curOperateTagBuffer = mReaderHelper.getCurOperateTagBuffer();
        m_curOperateTagISO18000Buffer = mReaderHelper
                .getCurOperateTagISO18000Buffer();

        mLogList = (LogList) findViewById(id.log_list);
        mStartStop = (TextView) findViewById(id.startstop);
        //mCbRealSet = (CheckBox) findViewById(id.check_real_set);
        //mLayoutRealSet = (LinearLayout) findViewById(id.layout_real_set);
        //mCbRealSession = (CheckBox) findViewById(id.check_real_session);

        mSessionIdTextView = findViewById(id.session_id_text);
        mInventoriedFlagTextView =  findViewById(id.inventoried_flag_text);
        mDropDownRow1 = findViewById(id.table_row_session_id);
        mDropDownRow2 = findViewById(id.table_row_inventoried_flag);
        mTagsCountText = findViewById(id.tags_count_text);
        mTagsTotalText = findViewById(id.tags_total_text);
        mTagsSpeedText = findViewById(id.tags_speed_text);
        mTagsTimeText =  findViewById(id.tags_time_text);
        mTagsOpTimeText =  findViewById(id.tags_op_time_text);
        mTagRealList =  findViewById(id.tag_real_list);
        mRealRoundEditText =  findViewById(id.real_round_text);

        mStartStop.setOnClickListener(setInventoryRealOnClickListener);

        lbm = LocalBroadcastManager.getInstance(mContext);

        IntentFilter itent = new IntentFilter();
        itent.addAction(ReaderHelper.BROADCAST_WRITE_LOG);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_READER_SETTING);
        lbm.registerReceiver(mRecv, itent);

        mDropDownRow1.setEnabled(mSelectCmdValue == 0);
        mDropDownRow2.setEnabled(mSelectCmdValue == 0);
        mDropDownRow1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showSpinWindow1();
            }
        });
        mDropDownRow2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showSpinWindow2();
            }
        });
        mSessionIdList.clear();
        mInventoriedFlagList.clear();
        String[] lists = getResources().getStringArray(R.array.session_id_list);
        for (int i = 0; i < lists.length; i++) {
            mSessionIdList.add(lists[i]);
        }
        lists = getResources().getStringArray(R.array.inventoried_flag_list);
        for (int i = 0; i < lists.length; i++) {
            mInventoriedFlagList.add(lists[i]);
        }

        mSpinerPopWindow1 = new SpinerPopWindow(mContext);
        mSpinerPopWindow1.refreshData(mSessionIdList, 1);
        mSpinerPopWindow1.setItemListener(new IOnItemSelectListener() {
            public void onItemClick(int pos) {
                setSessionIdText(pos);
            }
        });

        mSpinerPopWindow2 = new SpinerPopWindow(mContext);
        mSpinerPopWindow2.refreshData(mInventoriedFlagList, 0);
        mSpinerPopWindow2.setItemListener(new IOnItemSelectListener() {
            public void onItemClick(int pos) {
                setInventoriedFlagText(pos);
            }
        });

        updateView();

        if (mReaderHelper.getInventoryFlag()) {
            mHandler.removeCallbacks(mRefreshRunnable);
            mHandler.postDelayed(mRefreshRunnable, 2000);
        }

        mSelectCommand = (RadioGroup) findViewById(id.comand_select_radio);
        mSelectCommand.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case id.cmd_common:
                        mSelectCmdValue = -1;
                        break;
                    case id.cmd_S0:
                        mSelectCmdValue = 0;
                        break;
                    default:
                        mSelectCmdValue = 0;
                        break;
                }
                mDropDownRow1.setEnabled(mSelectCmdValue == 0);
                mDropDownRow2.setEnabled(mSelectCmdValue == 0);
            }
        });
        refreshText();
        refreshList();
        refreshStartStop(mReaderHelper.getInventoryFlag());
    }

    public void refresh() {
        m_curInventoryBuffer.clearInventoryRealResult();
        refreshList();
        refreshText();
        clearText();
        mRealRoundEditText.setText("1");
    }


    @SuppressWarnings("deprecation")
    private void refreshStartStop(boolean start) {
        if (start) {
            mStartStop.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.button_disenabled_background));
            mStartStop.setText(getResources()
                    .getString(R.string.stop_inventory));
        } else {
            mStartStop.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.button_background));
            mStartStop.setText(getResources().getString(
                    R.string.start_inventory));
        }
    }

    private Handler mUpdateViewHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            refreshText();
            refreshList();
        }
    };

    private Handler mHandler = new Handler();
    private Runnable mRefreshRunnable = new Runnable() {
        public void run() {
            refreshList();
            mHandler.postDelayed(this, 2000);
        }
    };
    private Handler mLoopHandler = new Handler();
    private Runnable mLoopRunnable = new Runnable() {
        public void run() {
            mReaderHelper.runLoopInventroy();
            mLoopHandler.postDelayed(this, 2000);
        }
    };

    private void setSessionIdText(int pos) {
        if (pos >= 0 && pos < mSessionIdList.size()) {
            String value = mSessionIdList.get(pos);
            mSessionIdTextView.setText(value);
            mPos1 = pos;
        }
    }

    private void setInventoriedFlagText(int pos) {
        if (pos >= 0 && pos < mInventoriedFlagList.size()) {
            String value = mInventoriedFlagList.get(pos);
            mInventoriedFlagTextView.setText(value);
            mPos2 = pos;
        }
    }

    private void showSpinWindow1() {
        mSpinerPopWindow1.setWidth(mDropDownRow1.getWidth());
        mSpinerPopWindow1.showAsDropDown(mDropDownRow1);
    }

    private void showSpinWindow2() {
        mSpinerPopWindow2.setWidth(mDropDownRow2.getWidth());
        mSpinerPopWindow2.showAsDropDown(mDropDownRow2);
    }

    private void updateView() {
        mPos1 = m_curInventoryBuffer.btSession;
        mPos2 = m_curInventoryBuffer.btTarget;
        if (m_curInventoryBuffer.lAntenna.size() <= 0)
            m_curInventoryBuffer.lAntenna.add((byte) 0x00);
        int nRepeat = m_curInventoryBuffer.btRepeat & 0xFF;
        mRealRoundEditText.setText(String.valueOf(nRepeat <= 0 ? 1 : nRepeat));

        if (mDefault) {
            mDefault = false;
            setSessionIdText(1);
            setInventoriedFlagText(mPos2);
        } else {
            setSessionIdText(mPos1);
            setInventoriedFlagText(mPos2);
        }
    }

    public void startStopFF() {

        refreshStartStop(mKeyF4Pressing);

        bTmpInventoryFlag = false;
        m_curInventoryBuffer.lAntenna.add((byte) 0x00);

        if (m_curInventoryBuffer.lAntenna.size() <= 0) {
            Toast.makeText(mContext,
                    getResources().getString(R.string.antenna_empty),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        m_curInventoryBuffer.bLoopInventoryReal = true;
        m_curInventoryBuffer.btRepeat = 0;

        String strRepeat = mRealRoundEditText.getText().toString();
        if (strRepeat == null || strRepeat.length() <= 0) {
            Toast.makeText(mContext,
                    getResources().getString(R.string.repeat_empty),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        m_curInventoryBuffer.btRepeat = (byte) Integer.parseInt(strRepeat);

        if ((m_curInventoryBuffer.btRepeat & 0xFF) <= 0) {
            Toast.makeText(mContext,
                    getResources().getString(R.string.repeat_min),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        //if (mCbRealSet.isChecked() && mCbRealSession.isChecked()) {
        if (mSelectCmdValue == 0) {
            m_curInventoryBuffer.bLoopCustomizedSession = true;
            m_curInventoryBuffer.btSession = (byte) (mPos1 & 0xFF);
            //m_curInventoryBuffer.btSession = (byte) mSelectCmdValue;
            m_curInventoryBuffer.btTarget = (byte) (mPos2 & 0xFF);
        } else {
            m_curInventoryBuffer.bLoopCustomizedSession = false;
        }

        if (!mKeyF4Pressing) {
            refreshText();
            mReaderHelper.setInventoryFlag(false);
            m_curInventoryBuffer.bLoopInventoryReal = false;

            mLoopHandler.removeCallbacks(mLoopRunnable);
            mHandler.removeCallbacks(mRefreshRunnable);
            refreshList();
            return;
        }

        mReaderHelper.setInventoryFlag(true);
        //mReaderHelper.clearInventoryTotal();
        //refreshText();
        byte btWorkAntenna = m_curInventoryBuffer.lAntenna
                .get(m_curInventoryBuffer.nIndexAntenna);
        if (btWorkAntenna < 0)
            btWorkAntenna = 0;
        // mReader.setWorkAntenna(m_curReaderSetting.btReadId, btWorkAntenna);
        mReaderHelper.runLoopInventroy();
        m_curReaderSetting.btWorkAntenna = btWorkAntenna;
        mRefreshTime = new Date().getTime();
        //refreshStartStop(true);
        mLoopHandler.removeCallbacks(mLoopRunnable);
        mLoopHandler.postDelayed(mLoopRunnable, 2000);
        mHandler.removeCallbacks(mRefreshRunnable);
        mHandler.postDelayed(mRefreshRunnable, 2000);
    }

    public void startstop() {
        bTmpInventoryFlag = false;
        m_curInventoryBuffer.clearInventoryPar();
        m_curInventoryBuffer.lAntenna.add((byte) 0x00);
        if (m_curInventoryBuffer.lAntenna.size() <= 0) {
            Toast.makeText(mContext,
                    getResources().getString(R.string.antenna_empty),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        m_curInventoryBuffer.bLoopInventoryReal = true;
        m_curInventoryBuffer.btRepeat = 0;

        String strRepeat = mRealRoundEditText.getText().toString();
        if (strRepeat == null || strRepeat.length() <= 0) {
            Toast.makeText(mContext,
                    getResources().getString(R.string.repeat_empty),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        m_curInventoryBuffer.btRepeat = (byte) Integer.parseInt(strRepeat);
        if ((m_curInventoryBuffer.btRepeat & 0xFF) <= 0) {
            Toast.makeText(mContext,
                    getResources().getString(R.string.repeat_min),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (mSelectCmdValue == 0) {
            m_curInventoryBuffer.bLoopCustomizedSession = true;
            m_curInventoryBuffer.btSession = (byte) (mPos1 & 0xFF);
            m_curInventoryBuffer.btTarget = (byte) (mPos2 & 0xFF);
        } else {
            m_curInventoryBuffer.bLoopCustomizedSession = false;
        }
        {
            if (!mStartStop.getText().toString()
                    .equals(getResources().getString(R.string.start_inventory))) {
                refreshText();
                mReaderHelper.setInventoryFlag(false);
                m_curInventoryBuffer.bLoopInventoryReal = false;

                refreshStartStop(false);
                mLoopHandler.removeCallbacks(mLoopRunnable);
                mHandler.removeCallbacks(mRefreshRunnable);
                refreshList();
                return;
            } else {
                refreshStartStop(true);
            }
        }
        mReaderHelper.setInventoryFlag(true);
        mReaderHelper.clearInventoryTotal();
        refreshText();

        byte btWorkAntenna = m_curInventoryBuffer.lAntenna
                .get(m_curInventoryBuffer.nIndexAntenna);
        if (btWorkAntenna < 0)
            btWorkAntenna = 0;
        // mReader.setWorkAntenna(m_curReaderSetting.btReadId, btWorkAntenna);
        mReaderHelper.runLoopInventroy();
        m_curReaderSetting.btWorkAntenna = btWorkAntenna;
        mRefreshTime = new Date().getTime();
        refreshStartStop(true);
        mLoopHandler.removeCallbacks(mLoopRunnable);
        mLoopHandler.postDelayed(mLoopRunnable, 2000);
        mHandler.removeCallbacks(mRefreshRunnable);
        mHandler.postDelayed(mRefreshRunnable, 2000);
    }

    private OnClickListener setInventoryRealOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (arg0.getId() == id.startstop) {
                startstop();
            }
        }
    };

    private void refreshList() {
        mTagRealList.refreshList();
    }

    //acc the total time;
    private static long TotalTime = System.currentTimeMillis();
    //acc the total time

    private void refreshText() {
        mTagsCountText.setText(String.valueOf(m_curInventoryBuffer.lsTagList
                .size()));
        mTagsTotalText
                .setText(String.valueOf(mReaderHelper.getInventoryTotal()));
        // if (m_curInventoryBuffer.nReadRate > 0) {

        mTagsSpeedText.setText(String.valueOf(m_curInventoryBuffer.nReadRate));

        // }
        mTagsTimeText.setText(String
                .valueOf(m_curInventoryBuffer.dtEndInventory.getTime()
                        - m_curInventoryBuffer.dtStartInventory.getTime()));
        synchronized (m_curInventoryBuffer) {
            if (m_curInventoryBuffer.nReadRate > 0) {
                mTagsOpTimeText.setText(String
                        .valueOf(m_curInventoryBuffer.nDataCount * 1000
                                / m_curInventoryBuffer.nReadRate));
            } else {
                mTagsOpTimeText.setText("0");
            }
        }
        mTagRealList.refreshText();
    }

    private void clearText() {
        mReaderHelper.setInventoryTotal(0);
        mTagsCountText.setText("0");
        mTagsTotalText.setText("0");
        mTagsSpeedText.setText("0");
        mTagsTimeText.setText("0");
        mTagsOpTimeText.setText("0");
        mTagRealList.clearText();
    }

    // add by lei.li 2016/11/14
    private boolean bTmpInventoryFlag = true;
    // add by lei.li 2016/11/14
    private final BroadcastReceiver mRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL)) {
                byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);
                switch (btCmd) {
                    case CMD.REAL_TIME_INVENTORY:
                    case CMD.CUSTOMIZED_SESSION_TARGET_INVENTORY:
                        if (!DEBUG) {
                            if (!mReaderHelper.getInventoryFlag()) {
                                if (!bTmpInventoryFlag) {
                                    bTmpInventoryFlag = true;
                                    mHandler.removeCallbacks(mRefreshRunnable);
                                    mHandler.postDelayed(mRefreshRunnable, 2000);
                                }
                            }
                        }
                        mUpdateViewHandler.sendEmptyMessage(0);
                        mHandler.removeCallbacks(mRefreshRunnable);
                        mHandler.postDelayed(mRefreshRunnable, 2000);
                        // add by lei.li 2016/11/14
                        mLoopHandler.removeCallbacks(mLoopRunnable);
                        mLoopHandler.postDelayed(mLoopRunnable, 2000);
                        //refreshText();
                        break;
                    case ReaderHelper.INVENTORY_ERR:
                    case ReaderHelper.INVENTORY_ERR_END:
                    case ReaderHelper.INVENTORY_END:
                        if (mReaderHelper.getInventoryFlag() /* || bTmpInventoryFlag */) {
                            mLoopHandler.removeCallbacks(mLoopRunnable);
                            mLoopHandler.postDelayed(mLoopRunnable, 2000);

                        } else {
                            mLoopHandler.removeCallbacks(mLoopRunnable);
                            // add by lei.li 2016/11/14
                            mHandler.removeCallbacks(mRefreshRunnable);
                            // add by lei.li 2016/11/14
                        }

                        mUpdateViewHandler.sendEmptyMessage(0);
                        break;
                }

            } else if (intent.getAction().equals(
                    ReaderHelper.BROADCAST_WRITE_LOG)) {
                mLogList.writeLog((String) intent.getStringExtra("log"),
                        intent.getIntExtra("type", ERROR.SUCCESS));
            } /*else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_READER_SETTING)) {
                String[] strs = mLogList.getTitleInfo().toString().split("  ");
                Log.d(TAG, "onReceive: " + strs[0]);
                if (m_curReaderSetting.btReaderStatus == 0x13) {
                    mLogList.setTitleInfo(strs[0] + "  ");
                } else if (m_curReaderSetting.btReaderStatus == -1) {
                } else {
                    if (!strs[strs.length - 1].equals("<" +
                            context.getResources().getString(R.string.auto_read_model) + ">")) {
                        SpannableString tSS = new SpannableString(strs[0] + "  " + "  <" +
                                context.getResources().getString(R.string.auto_read_model) + ">");
                        tSS.setSpan(new ForegroundColorSpan(Color.RED), 0, tSS.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        mLogList.setTitleInfo(tSS);
                    }
                }
            }*/
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mLogList.tryClose())
                return true;
        }
        Log.d(TAG, "onKeyDown: PageReal");
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    public void doDestroy() {
        // TODO Auto-generated method stub

        if (lbm != null)
            lbm.unregisterReceiver(mRecv);

        mLoopHandler.removeCallbacks(mLoopRunnable);
        mHandler.removeCallbacks(mRefreshRunnable);
    }
}

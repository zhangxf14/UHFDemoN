package com.uhf.uhf.activity.setpage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.reader.base.CMD;
import com.reader.base.ERROR;
import com.reader.base.MessageTran;
import com.reader.base.ReaderBase;
import com.reader.helper.ISO180006BOperateTagBuffer;
import com.reader.helper.InventoryBuffer;
import com.reader.helper.OperateTagBuffer;
import com.reader.helper.ReaderHelper;
import com.reader.helper.ReaderSetting;
import com.uhf.uhf.widget.LogList;
import com.uhf.uhf.R;
import com.uhf.uhf.UHFApplication;
import com.uhf.uhf.activity.BaseActivity;

/**
 * Created by Administrator on 2018/4/28.
 */

public class PageReaderStatus extends BaseActivity {
    private LogList mLogList;

    private TextView mGet;
    private TextView mReset;

    private EditText mReaderStatusInfoEdit;

    private ReaderHelper mReaderHelper;
    private ReaderBase mReader;

    private static ReaderSetting m_curReaderSetting;
    private static InventoryBuffer m_curInventoryBuffer;
    private static OperateTagBuffer m_curOperateTagBuffer;
    private static ISO180006BOperateTagBuffer m_curOperateTagISO18000Buffer;

    private LocalBroadcastManager lbm;

    @Override
    protected void onResume() {
        if (mReader != null) {
            if (!mReader.IsAlive())
                mReader.StartWait();
        }
        super.onResume();
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_reader_status);
        ((UHFApplication) getApplication()).addActivity(this);

        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
            mReader = mReaderHelper.getReader();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        m_curReaderSetting = mReaderHelper.getCurReaderSetting();
        m_curInventoryBuffer = mReaderHelper.getCurInventoryBuffer();
        m_curOperateTagBuffer = mReaderHelper.getCurOperateTagBuffer();
        m_curOperateTagISO18000Buffer = mReaderHelper.getCurOperateTagISO18000Buffer();

        mLogList = (LogList) findViewById(R.id.log_list);
        mGet = (TextView) findViewById(R.id.get);
        mReaderStatusInfoEdit = (EditText) findViewById(R.id.reader_status_info);
        mReset = (TextView) findViewById(R.id.resort_normal);

        mGet.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mReader.sendBuffer(new byte[]{(byte)0xA0,(byte)0x03,(byte)0xFF,(byte)0xA1,
                        (byte)0xBD});
            }
        });

        mReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    byte status = (byte)Integer.parseInt(mReaderStatusInfoEdit.getText().toString(),16);
                    MessageTran messageTran = new MessageTran(m_curReaderSetting.btReadId,(byte)0xA0,new byte[]{status});
                    mReader.sendBuffer(messageTran.getAryTranData());
                    m_curReaderSetting.btReaderStatus = status;
                } catch (Exception ex) {
                    Toast.makeText(UHFApplication.getContext(),ex.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });

        lbm  = LocalBroadcastManager.getInstance(this);

        IntentFilter itent = new IntentFilter();
        itent.addAction(ReaderHelper.BROADCAST_WRITE_LOG);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_READER_SETTING);
        lbm.registerReceiver(mRecv, itent);

        updateView();
    }

    private void updateView() {
        String[] strs = mLogList.getTitleInfo().toString().split("  ");
        if (m_curReaderSetting.btReaderStatus == 0x13) {
                mLogList.setTitleInfo(strs[0] + "  ");
        } else if (m_curReaderSetting.btReaderStatus == -1) {

        } else {
            if (!strs[strs.length - 1].equals("<" +
                    getResources().getString(R.string.auto_read_model) + ">")) {
                SpannableString tSS = new SpannableString(strs[0] + "  " + "  <" +
                        getResources().getString(R.string.auto_read_model) +">");
                tSS.setSpan(new ForegroundColorSpan(Color.RED), 0, tSS.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mLogList.setTitleInfo(tSS);
            }
        }
        mReaderStatusInfoEdit.setText(String.format("%02x",m_curReaderSetting.btReaderStatus));
    }

    private final BroadcastReceiver mRecv = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_READER_SETTING)) {
                byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);

                if (btCmd == CMD.QUERY_READER_STATUS || btCmd == CMD.SET_READER_STATUS) {
                    mReaderStatusInfoEdit.setText(String.format("%02x",m_curReaderSetting.btReaderStatus));
                    //updateView();
                }
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_WRITE_LOG)) {
                mLogList.writeLog((String)intent.getStringExtra("log"), intent.getIntExtra("type", ERROR.SUCCESS));
            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mLogList.tryClose()) return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        if (lbm != null)
            lbm.unregisterReceiver(mRecv);
    }
}

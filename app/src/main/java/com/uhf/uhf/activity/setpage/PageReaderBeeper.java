package com.uhf.uhf.activity.setpage;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.util.BeeperUtils;
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
import com.uhf.uhf.UHFApplication;
import com.uhf.uhf.activity.BaseActivity;
import com.util.PreferenceUtil;

public class PageReaderBeeper extends BaseActivity {
	private LogList mLogList;
	
	private TextView mSet;
	
	private RadioGroup mGroupBeeper;
	
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
		setContentView(layout.page_reader_beeper);
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

		mLogList = (LogList) findViewById(id.log_list);
		mSet = (TextView) findViewById(id.set);
		mGroupBeeper =  (RadioGroup) findViewById(id.group_beeper);
		
		mSet.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				byte btMode = 0;
				BeeperUtils.BeepMode beepMode;
				switch (mGroupBeeper.getCheckedRadioButtonId()) {
				case id.set_beeper_quiet:
					btMode = 1;
					beepMode = BeeperUtils.BeepMode.QUITE;
					break;
				case id.set_beeper_all:
					btMode = 2;
					beepMode = BeeperUtils.BeepMode.BEEP_INVENTORIED;
					break;
				case id.set_beeper_one:
					btMode = 3;
					beepMode = BeeperUtils.BeepMode.BEEP_PER_TAG;
					break;
				default:
					beepMode = BeeperUtils.BeepMode.BEEP_INVENTORIED;
                    btMode  = 0;
					break;
				}
				PreferenceUtil.commitInt(BeeperUtils.BEEPER_MODEL,btMode);
				BeeperUtils.setBeepMode(beepMode);
				mLogList.writeLog(getApplication().getResources().getString(R.string.set_read_sound),0x10);
			}
		});
		
		lbm  = LocalBroadcastManager.getInstance(this);
		
		IntentFilter itent = new IntentFilter();
		itent.addAction(ReaderHelper.BROADCAST_WRITE_LOG);
		lbm.registerReceiver(mRecv, itent);
		updateView();
	}
	
	private void updateView() {
		if (m_curReaderSetting.btBeeperMode == 0) {
			mGroupBeeper.check(id.set_beeper_quiet);
		} else if (m_curReaderSetting.btBeeperMode == 1) {
			mGroupBeeper.check(id.set_beeper_all);
		} else if (m_curReaderSetting.btBeeperMode == +2) {
			mGroupBeeper.check(id.set_beeper_one);
		}
	}
	
	private final BroadcastReceiver mRecv = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ReaderHelper.BROADCAST_WRITE_LOG)) {
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


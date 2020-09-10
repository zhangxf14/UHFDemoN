package com.uhf.uhf.fragment;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.reader.base.ERROR;
import com.reader.base.MessageTran;
import com.reader.base.ReaderBase;
import com.reader.base.StringTool;
import com.reader.helper.ReaderHelper;
import com.uhf.uhf.R;
import com.uhf.uhf.activity.setpage.PageShowTestTempChart;
import com.uhf.uhf.widget.FuDanTagList;
import com.uhf.uhf.widget.HexEditTextBox;
import com.uhf.uhf.widget.LogList;
import com.util.NumberUtils;
import com.util.PreferenceUtil;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PageReaderFuDanCmdFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    private TextView mutilParaLabel;
    private HexEditTextBox mutilPwd, mutilPara;
    private HexEditTextBox rtcPwd, rtcOpt0, rtcOpt1, rtcOpt2, rtcOpt3;
    private HexEditTextBox readPwd, readOpt0, readOpt1, readOpt2, readOpt3;
    private HexEditTextBox writePwd, writeOpt0, writeOpt1, writeOpt2, writeOpt3, writeData;

    private HexEditTextBox rtcMinute, rtcSecond, rtcTempTop, rtcTempBottom, rtcTimes;

    private Button send, rtcSend, readSend, writeSend, readRtcTestResult;

    private Spinner selectOperation;

    private FuDanTagList fuDanTagList;

    private ReaderHelper mReaderHelper;
    private ReaderBase mReader;

    private LocalBroadcastManager lbm;

    private FuDanTagList pageTagAccess;

    private LogList mLogList;

    private View root;

    private ProgressDialog progressDialog;
    private String title;
    private String epc;
    private View epcItem;
    private TextView epcShow;

    private boolean testTempIsEnd = true;

    private Handler testHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0:
                    progressDialog.setProgress(msg.arg1);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private int progress = 0;

    private final BroadcastReceiver mRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_OPERATE_TAG)) {
                byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);
                Log.d("btCmd:", String.format("%02x", btCmd));
                if (btCmd == (byte) 0xF1) {
                    drawTempChart();
                    updateProgressDialog(mReaderHelper.getCurOperateTagBuffer().TempCount, false);
                } else if (btCmd == (byte) 0x81) {
                    updateView();
                } else if (btCmd == (byte) 0xFF) {
                    updateProgressDialog(0, false);
                }
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_WRITE_LOG)) {
                int status = intent.getIntExtra("type", ERROR.SUCCESS);
                if (status != ERROR.SUCCESS) {
                    updateProgressDialog(status, false);
                }
                mLogList.writeLog(intent.getStringExtra("log"), status);
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_TEST_TEMP_PROGRESS)) {
                updateProgressDialog(intent.getIntExtra("progress", 0), true);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.act_fudan_command, container, false);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        init();
        initView(root);
    }

    public void setEpc(String epc) {
        Log.d(TAG, "SetEpc:" + epc);
        if (epc == "") {
            epcItem.setVisibility(View.GONE);
        } else {
            epcItem.setVisibility(View.VISIBLE);
            epcShow.setText(epc);
        }
    }

    private void init() {
        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
            mReader = mReaderHelper.getReader();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (service.isShutdown()) {
            service = Executors.newScheduledThreadPool(1);
        }
        setTimerTask();

        title = getResources().getString(R.string.test_temperature_running);

        lbm = LocalBroadcastManager.getInstance(this.getActivity());

        IntentFilter itent = new IntentFilter();
        itent.addAction(ReaderHelper.BROADCAST_WRITE_LOG);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_OPERATE_TAG);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_TEST_TEMP_PROGRESS);
        lbm.registerReceiver(mRecv, itent);
    }

    private void setTimerTask() {
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ((progress == progressDialog.getProgress()) && progressDialog.isShowing() && (!testTempIsEnd)) {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Reading temperature results failed!", Toast.LENGTH_SHORT).show();
                        }
                        progress = progressDialog.getProgress();
                        Log.d(TAG, "Progress: " + progress);
                    }
                });
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
    }

    private void updateView() {
        pageTagAccess.refreshList();
    }

    private void drawTempChart() {
        this.getActivity().startActivity(new Intent(this.getActivity(), PageShowTestTempChart.class));
    }

    private void initView(View root) {

        progressDialog = new ProgressDialog(getActivity());

        epcItem = root.findViewById(R.id.title_epc);
        epcShow = root.findViewById(R.id.epc_text);
        fuDanTagList = root.findViewById(R.id.fudan_tag_list);
        selectOperation = root.findViewById(R.id.operation_command);
        pageTagAccess = root.findViewById(R.id.fudan_tag_list);
        mLogList = root.findViewById(R.id.log_list);

        selectOperation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mutilPara.setEnabled(true);
                mutilParaLabel.setEnabled(true);
                switch (position) {
                    case 0:
                        mutilParaLabel.setText(R.string.tag_meas_opt);
                        mutilPara.setText("00000004");
                        break;
                    case 1:
                        mutilParaLabel.setText(R.string.stop_rtc_xoredPWD);
                        mutilPara.setText("00000000");
                        break;
                    case 2:
                        mutilParaLabel.setEnabled(false);
                        mutilPara.setEnabled(false);
                        mutilPara.setText("00000000");
                        break;
                    case 3:
                        mutilParaLabel.setText(R.string.ptr);
                        mutilPara.setText("00000000");
                        break;
                    case 4:
                        mutilParaLabel.setText(R.string.tlogger_opt3);
                        mutilPara.setText("00000000");
                        break;
                    case 5:
                        mutilParaLabel.setText(R.string.auth_xoredPWD);
                        mutilPara.setText("00000000");
                        break;
                    case 6:
                        mutilParaLabel.setText(R.string.tlogger_opt2);
                        mutilPara.setText("00080000");
                        break;
                    case 7:
                        mutilParaLabel.setText(R.string.tag_meas_opt);
                        mutilPara.setText("00000010");
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mutilPwd = root.findViewById(R.id.mutil_pwd);
        mutilPara = root.findViewById(R.id.tag_meas_opt);
        mutilParaLabel = root.findViewById(R.id.mutil_para_label);

        rtcPwd = root.findViewById(R.id.rtc_pwd);
        //rtcOpt0 = (HexEditTextBox) findViewById(R.id.rtc_opt0);
        rtcOpt1 = root.findViewById(R.id.rtc_opt1);
        rtcOpt2 = root.findViewById(R.id.rtc_opt2);
        rtcOpt3 = root.findViewById(R.id.rtc_opt3);

        rtcMinute = root.findViewById(R.id.rtc_time);
        rtcSecond = root.findViewById(R.id.rtc_interval);
        rtcTempTop = root.findViewById(R.id.rtc_temp_top);
        rtcTempBottom = root.findViewById(R.id.rtc_temp_bottom);
        rtcTimes = root.findViewById(R.id.rtc_temp_times);

        rtcMinute.setText(PreferenceUtil.getString("delay","0"));
        rtcSecond.setText(PreferenceUtil.getString("interval","1"));
        rtcTempTop.setText(PreferenceUtil.getString("upper","40"));
        rtcTempBottom.setText(PreferenceUtil.getString("lower","0"));
        rtcTimes.setText(PreferenceUtil.getString("number","10"));

        readPwd = root.findViewById(R.id.read_mem_pwd);
        readOpt0 = root.findViewById(R.id.read_mem_opt2);
        readOpt1 = root.findViewById(R.id.read_mem_xored_pwd);
        readOpt2 = root.findViewById(R.id.read_mem_add);
        readOpt3 = root.findViewById(R.id.read_mem_len);


        writePwd = root.findViewById(R.id.write_mem_pwd);
        writeOpt0 = root.findViewById(R.id.write_mem_opt2);
        writeOpt1 = root.findViewById(R.id.write_mem_xored_pwd);
        writeOpt2 = root.findViewById(R.id.write_mem_add);
        writeOpt3 = root.findViewById(R.id.write_mem_len);
        writeData = root.findViewById(R.id.write_mem_data);


        send = root.findViewById(R.id.send);
        rtcSend = root.findViewById(R.id.rtc_send);
        readRtcTestResult = root.findViewById(R.id.rtc_read_temp);
        readSend = root.findViewById(R.id.read_mem_send);
        writeSend = root.findViewById(R.id.write_mem_send);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fuDanTagList.clearBuffer();
                byte[] sendData;
                if (selectOperation.getSelectedItemPosition() == 2) {
                    sendData = new byte[10];
                } else {
                    sendData = new byte[14];
                }
                byte cmd = 0x01;
                sendData[0] = (byte) 0xA0;
                sendData[2] = (byte) 0xFF;
                sendData[3] = (byte) 0xFD;
                switch (selectOperation.getSelectedItemPosition()) {
                    case 0:
                        mReaderHelper.getCurOperateTagBuffer().type = 0x04;
                        cmd = 0x01;
                        mReaderHelper.getCurOperateTagBuffer().readLen = 0x02;
                        break;
                    case 1:
                        cmd = 0x03;
                        break;
                    case 2:
                        cmd = 0x04;//no para
                        sendData = new byte[10];
                        sendData[0] = (byte) 0xA0;
                        sendData[2] = (byte) 0xFF;
                        sendData[3] = (byte) 0xFD;
                        break;
                    case 3:
                        cmd = 0x10;
                        mReaderHelper.getCurOperateTagBuffer().readLen = 0x02;
                        break;
                    case 4:
                        cmd = 0x11;
                        mReaderHelper.getCurOperateTagBuffer().readLen = 0x02;
                        break;
                    case 5:
                        cmd = 0x12;
                        break;
                    case 6:
                        cmd = 0x20;
                        break;
                }
                mReaderHelper.getCurOperateTagBuffer().fuDanOpCode = cmd;
                sendData[1] = (byte) (sendData.length - 2);
                sendData[4] = cmd;
                byte[] pwd = StringTool.hexStringToByteArray(mutilPwd.getText().toString());
                byte[] para = StringTool.hexStringToByteArray(mutilPara.getText().toString());

                if (cmd == 0x01) {
                    if (para[para.length - 1] == 0x04) {
                        mReaderHelper.getCurOperateTagBuffer().type = 0x04;
                    } else {
                        mReaderHelper.getCurOperateTagBuffer().type = 0x08;
                    }
                }

                if (pwd.length != 4 || para.length != 4) {
                    Toast.makeText(PageReaderFuDanCmdFragment.this.getContext(), "Parameter is error!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectOperation.getSelectedItemPosition() == 1 ||
                        selectOperation.getSelectedItemPosition() == 5)
                    for (int i = 0; i < para.length / 2; i++) {
                        byte tmp = para[i];
                        para[i] = para[para.length - i - 1];
                        para[para.length - i - 1] = tmp;
                    }

                System.arraycopy(pwd, 0, sendData, 5, pwd.length);
                if (selectOperation.getSelectedItemPosition() != 2) {
                    System.arraycopy(para, 0, sendData, 5 + pwd.length, para.length);
                }


                sendData[sendData.length - 1] = MessageTran.checkSum(sendData, 0, sendData.length - 1);
                try {
                   int rs = ReaderHelper.getDefaultHelper().getReader().sendBuffer(sendData);
                   if (rs==-1){
                       Toast.makeText(PageReaderFuDanCmdFragment.this.getContext(), "failed", Toast.LENGTH_SHORT).show();
                   }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(PageReaderFuDanCmdFragment.this.getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        rtcSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fuDanTagList.clearBuffer();
                mReaderHelper.getCurOperateTagBuffer().fuDanOpCode = 0x02;
                mReaderHelper.getCurOperateTagBuffer().type = 0xF3;
                byte[] sendData = new byte[26];
                sendData[0] = (byte) 0xA0;
                sendData[2] = (byte) 0xFF;
                sendData[1] = 24;
                sendData[3] = (byte) 0xFD;
                sendData[4] = (byte) 0x02;



                int minuteValue = Integer.parseInt(rtcMinute.getText().toString());
                int secondValue = Integer.parseInt(rtcSecond.getText().toString());
                int tempTopValue = Integer.parseInt(rtcTempTop.getText().toString());// >= 0 ? (short)(Convert.ToInt32(hexTextBox5.Text) * 4) : (short)(Convert.ToInt32(hexTextBox5.Text) * 4 + 1024);
                int tempBottomValue = Integer.parseInt(rtcTempBottom.getText().toString()); //>= 0 ? (short)(Convert.ToInt32(hexTextBox20.Text) * 4) : (short)(Convert.ToInt32(hexTextBox20.Text) * 4 + 1024);
                int timesValue = Integer.parseInt(rtcTimes.getText().toString());
                //保存参数到本地
                PreferenceUtil.commitString("delay",rtcMinute.getText().toString());
                PreferenceUtil.commitString("interval",rtcSecond.getText().toString());
                PreferenceUtil.commitString("upper",rtcTempTop.getText().toString());
                PreferenceUtil.commitString("lower",rtcTempBottom.getText().toString());
                PreferenceUtil.commitString("number",rtcTimes.getText().toString());

                if (minuteValue < 0 || minuteValue > 65535 || secondValue < 0 || secondValue > 65535
                        || tempTopValue < -128 || tempTopValue > 127 || tempBottomValue < -128 || tempBottomValue > 127 || timesValue < 0 || timesValue > 65535) {
                    Toast.makeText(PageReaderFuDanCmdFragment.this.getContext(), "Parameter error!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int seed = 0x03FF;
                tempTopValue = (tempTopValue * 4) & 0x03FF;
                tempBottomValue = (tempBottomValue * 4) & 0x03FF;


                byte[] minute = NumberUtils.unsignedShortToByte2(minuteValue);
                byte[] second = NumberUtils.unsignedShortToByte2(secondValue);
                byte[] tempTop = NumberUtils.unsignedShortToByte2(tempTopValue);
                byte[] tempBottom = NumberUtils.unsignedShortToByte2(tempBottomValue);
                byte[] times = NumberUtils.unsignedShortToByte2(timesValue);

                String[] pwd = StringTool.stringToStringArray(rtcPwd.getText().toString(), 2);
                //String[] op3 = CCommondMethod.StringToStringArray(hexTextBox7.Text, 2);

                if (pwd == null || pwd.length != 4) {
                    Toast.makeText(PageReaderFuDanCmdFragment.this.getContext(), "Parameter error!", Toast.LENGTH_SHORT).show();
                    return;
                }

                byte[] btPwd = StringTool.stringArrayToByteArray(pwd, pwd.length);
                byte[] btOp0 = new byte[4];
                btOp0[0] = minute[0];
                btOp0[1] = minute[1];
                btOp0[2] = second[0];
                btOp0[3] = second[1];

                byte[] btOp1 = new byte[4];
                btOp1[0] = tempTop[0];
                btOp1[1] = tempTop[1];
                btOp1[2] = tempBottom[0];
                btOp1[3] = tempBottom[1];

                byte[] btOp2 = new byte[4];
                btOp2[2] = times[0];
                btOp2[3] = times[1];
                byte[] btOp3 = new byte[4];

                System.arraycopy(btPwd, 0, sendData, 5, btPwd.length);
                System.arraycopy(btOp0, 0, sendData, 5 + btPwd.length, btOp0.length);
                System.arraycopy(btOp1, 0, sendData, 5 + btPwd.length + btOp0.length, btOp1.length);
                System.arraycopy(btOp2, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length, btOp2.length);
                System.arraycopy(btOp3, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length + btOp2.length, btOp3.length);

                byte[] checkData = new byte[25];
                System.arraycopy(sendData, 0, checkData, 0, checkData.length);
                sendData[sendData.length - 1] = MessageTran.checkSum(checkData, 0, checkData.length);

                Log.d("XXXXXXXXX", StringTool.byteArrayToString(sendData, 0, sendData.length));

                mReaderHelper.getCurOperateTagBuffer().rtcTestTemp = sendData;

                //写入测温间隔
                writeMemory("0028",rtcSecond.getText().toString());
                //写入温度上限
//                writeMemory("0024","");

                byte[] saveTime = {(byte) 0xA0, 0x17, (byte) 0xFF, (byte) 0xFD,
                        0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x04, 0x0A, 0x00, 0x00, 0x00, 0x0B};
                saveTime[5] = btPwd[0];
                saveTime[6] = btPwd[1];
                saveTime[7] = btPwd[2];
                saveTime[8] = btPwd[3];


                int time = (int) (System.currentTimeMillis() / 1000);
                //Console.WriteLine("time:" + time);
                Log.d("KKKKKKKKKK", "Time:" + new Date(time * 1000L));
                saveTime[saveTime.length - 5] = NumberUtils.intToByte4(time)[0];
                saveTime[saveTime.length - 4] = NumberUtils.intToByte4(time)[1];
                saveTime[saveTime.length - 3] = NumberUtils.intToByte4(time)[2];
                saveTime[saveTime.length - 2] = NumberUtils.intToByte4(time)[3];
                saveTime[saveTime.length - 1] = MessageTran.checkSum(saveTime, 0, saveTime.length - 1);
                mReader.sendBuffer(saveTime);
            }
        });

        readRtcTestResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progressDialog.isShowing()) {
                    return;
                }
                if (testTempIsEnd) {
                    testTempIsEnd = false;
                } else {
                    return;
                }

                fuDanTagList.clearBuffer();
                mReader.sendBuffer(new byte[]{(byte) 0xA0, 0x0C, (byte) 0xFF, (byte) 0xFD, 0x10,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xC0, (byte) 0x91, (byte) 0xF7});
                mReaderHelper.getCurOperateTagBuffer().fuDanOpCode = 0x10;
                mReaderHelper.getCurOperateTagBuffer().type = 0xF0;
                // m_curOperateTagBuffer.fuDanOpCode = 0x13;

                byte[] sendData = new byte[22];
                sendData[0] = (byte) 0xA0;
                sendData[2] = (byte) 0xFF;
                sendData[1] = 20;
                sendData[3] = (byte) 0xFD;
                sendData[4] = (byte) 0x13;



                String[] pwd = StringTool.stringToStringArray(readPwd.getText().toString(), 2);
                String[] op0 = StringTool.stringToStringArray(readOpt0.getText().toString(), 2);
                String[] op1 = StringTool.stringToStringArray(readOpt1.getText().toString(), 2);
                String[] op2 = StringTool.stringToStringArray(readOpt2.getText().toString(), 2);
                String[] op3 = StringTool.stringToStringArray(readOpt3.getText().toString(), 2);

                mReaderHelper.getCurOperateTagBuffer().TempCount = Integer.parseInt(rtcTimes.getText().toString());
                mReaderHelper.getCurOperateTagBuffer().testTempProgress = 0;

                if (pwd == null || op0 == null || op1 == null || op2 == null || op3 == null ||
                        pwd.length != 4 || op0.length != 4 || op2.length != 2 || op3.length != 2) {
                    Toast.makeText(PageReaderFuDanCmdFragment.this.getContext(), "Parameter error!", Toast.LENGTH_SHORT).show();
                    return;
                }

                byte[] btPwd = StringTool.stringArrayToByteArray(pwd, pwd.length);
                byte[] btOp0 = StringTool.stringArrayToByteArray(op0, op0.length);
                byte[] btOp1 = StringTool.stringArrayToByteArray(op1, op1.length);
                byte[] btOp2 = StringTool.stringArrayToByteArray(op2, op2.length);
                byte[] btOp3 = StringTool.stringArrayToByteArray(op3, op3.length);

                for (int i = 0; i < btOp1.length / 2; i++) {
                    byte tmp = btOp1[i];
                    btOp1[i] = btOp1[btOp1.length - i - 1];
                    btOp1[btOp1.length - i - 1] = tmp;
                }

                mReaderHelper.getCurOperateTagBuffer().readLen = btOp3[0] * 256 + btOp3[1];
                if (mReaderHelper.getCurOperateTagBuffer().readLen < 4) {
                    Toast.makeText(PageReaderFuDanCmdFragment.this.getContext(), "Parameter error!", Toast.LENGTH_SHORT).show();
                    return;
                }

                System.arraycopy(btPwd, 0, sendData, 5, btPwd.length);
                System.arraycopy(btOp0, 0, sendData, 5 + btPwd.length, btOp0.length);
                System.arraycopy(btOp1, 0, sendData, 5 + btPwd.length + btOp0.length, op1.length);
                System.arraycopy(btOp2, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length, btOp2.length);
                System.arraycopy(btOp3, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length + btOp2.length, btOp3.length);
                byte[] checkData = new byte[21];
                mReaderHelper.getCurOperateTagBuffer().testTempInterval = Integer.parseInt(rtcSecond.getText().toString());
                System.arraycopy(sendData, 0, checkData, 0, checkData.length);
                sendData[sendData.length - 1] = MessageTran.checkSum(checkData, 0, checkData.length);
                mReaderHelper.getCurOperateTagBuffer().readMemCmd = sendData;
                sendData[sendData.length - 5] = 0x10;
                sendData[sendData.length - 4] = 0x00;
                buildProgressDialog(title, mReaderHelper.getCurOperateTagBuffer().TempCount);
            }
        });

        readSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fuDanTagList.clearBuffer();
                byte[] sendData = new byte[22];
                sendData[0] = (byte) 0xA0;
                sendData[2] = (byte) 0xFF;
                sendData[1] = (byte) 20;
                sendData[3] = (byte) 0xFD;
                sendData[4] = (byte) 0x13;

                mReaderHelper.getCurOperateTagBuffer().fuDanOpCode = (byte) 0x13;
                mReaderHelper.getCurOperateTagBuffer().type = 0xF2;
                byte[] btPwd = StringTool.hexStringToByteArray(readPwd.getText().toString());
                byte[] btOp0 = StringTool.hexStringToByteArray(readOpt0.getText().toString());
                byte[] btOp1 = StringTool.hexStringToByteArray(readOpt1.getText().toString());
                byte[] btOp2 = StringTool.hexStringToByteArray(readOpt2.getText().toString());
                byte[] btOp3 = StringTool.hexStringToByteArray(readOpt3.getText().toString());

                for (int i = 0; i < btOp1.length / 2; i++) {
                    byte tmp = btOp1[i];
                    btOp1[i] = btOp1[btOp1.length - i - 1];
                    btOp1[btOp1.length - i - 1] = tmp;
                }

                mReaderHelper.getCurOperateTagBuffer().readLen = NumberUtils.byte2ToUnsignedShort(btOp3);

                System.arraycopy(btPwd, 0, sendData, 5, btPwd.length);
                System.arraycopy(btOp0, 0, sendData, 5 + btPwd.length, btOp0.length);
                System.arraycopy(btOp1, 0, sendData, 5 + btPwd.length + btOp0.length, btOp1.length);
                System.arraycopy(btOp2, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length, btOp2.length);
                System.arraycopy(btOp3, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length + btOp2.length, btOp3.length);

                sendData[sendData.length - 1] = MessageTran.checkSum(sendData, 0, sendData.length - 1);

                try {
                    ReaderHelper.getDefaultHelper().getReader().sendBuffer(sendData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        writeSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fuDanTagList.clearBuffer();
                byte[] sendData = new byte[25];
                sendData[0] = (byte) 0xA0;
                sendData[2] = (byte) 0xFF;
                sendData[1] = (byte) 23;
                sendData[3] = (byte) 0xFD;
                sendData[4] = (byte) 0x14;

                mReaderHelper.getCurOperateTagBuffer().fuDanOpCode = 0x14;
                mReaderHelper.getCurOperateTagBuffer().type = 0xF2;

                byte[] btPwd = StringTool.hexStringToByteArray(writePwd.getText().toString());
                byte[] btOp0 = StringTool.hexStringToByteArray(writeOpt0.getText().toString());
                byte[] btOp1 = StringTool.hexStringToByteArray(writeOpt1.getText().toString());
                byte[] btOp2 = StringTool.hexStringToByteArray(writeOpt2.getText().toString());
                byte[] btOp3 = StringTool.hexStringToByteArray(writeOpt3.getText().toString());
                byte[] btData = StringTool.hexStringToByteArray(writeData.getText().toString());

                for (int i = 0; i < btOp1.length / 2; i++) {
                    byte tmp = btOp1[i];
                    btOp1[i] = btOp1[btOp1.length - i - 1];
                    btOp1[btOp1.length - i - 1] = tmp;
                }

                System.arraycopy(btPwd, 0, sendData, 5, btPwd.length);
                System.arraycopy(btOp0, 0, sendData, 5 + btPwd.length, btOp0.length);
                System.arraycopy(btOp1, 0, sendData, 5 + btPwd.length + btOp0.length, btOp1.length);
                System.arraycopy(btOp2, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length, btOp2.length);
                System.arraycopy(btOp3, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length + btOp2.length, btOp3.length);
                System.arraycopy(btData, 0, sendData,
                        5 + btPwd.length + btOp0.length + btOp1.length + btOp2.length + btOp3.length, btData.length);

                sendData[sendData.length - 1] = MessageTran.checkSum(sendData, 0, sendData.length - 1);

                try {
                    ReaderHelper.getDefaultHelper().getReader().sendBuffer(sendData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void  writeMemory(String addr,String data){
        fuDanTagList.clearBuffer();
        byte[] sendData = new byte[25];
        sendData[0] = (byte) 0xA0;
        sendData[2] = (byte) 0xFF;
        sendData[1] = (byte) 23;
        sendData[3] = (byte) 0xFD;
        sendData[4] = (byte) 0x14;

        mReaderHelper.getCurOperateTagBuffer().fuDanOpCode = 0x14;
        mReaderHelper.getCurOperateTagBuffer().type = 0xF2;

        byte[] btPwd = StringTool.hexStringToByteArray(writePwd.getText().toString());
        byte[] btOp0 = StringTool.hexStringToByteArray(writeOpt0.getText().toString());
        byte[] btOp1 = StringTool.hexStringToByteArray(writeOpt1.getText().toString());
        byte[] btOp2 = StringTool.hexStringToByteArray(addr);//writeOpt2.getText().toString()
        byte[] btOp3 = StringTool.hexStringToByteArray(writeOpt3.getText().toString());//
        byte[] btData = StringTool.hexStringToByteArray(data);//writeData.getText().toString()

        for (int i = 0; i < btOp1.length / 2; i++) {
            byte tmp = btOp1[i];
            btOp1[i] = btOp1[btOp1.length - i - 1];
            btOp1[btOp1.length - i - 1] = tmp;
        }

        System.arraycopy(btPwd, 0, sendData, 5, btPwd.length);
        System.arraycopy(btOp0, 0, sendData, 5 + btPwd.length, btOp0.length);
        System.arraycopy(btOp1, 0, sendData, 5 + btPwd.length + btOp0.length, btOp1.length);
        System.arraycopy(btOp2, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length, btOp2.length);
        System.arraycopy(btOp3, 0, sendData, 5 + btPwd.length + btOp0.length + btOp1.length + btOp2.length, btOp3.length);
        System.arraycopy(btData, 0, sendData,
                5 + btPwd.length + btOp0.length + btOp1.length + btOp2.length + btOp3.length, btData.length);

        sendData[sendData.length - 1] = MessageTran.checkSum(sendData, 0, sendData.length - 1);

        try {
            ReaderHelper.getDefaultHelper().getReader().sendBuffer(sendData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void updateProgressDialog(int progress, boolean isNormal) {
        if (!isNormal) {
            if (progressDialog != null) {
                progressDialog.dismiss();
                testTempIsEnd = true;
            }
            return;
        }
        int max = progressDialog.getMax();
        if (max == progress) {
            progressDialog.dismiss();
            testTempIsEnd = true;
        } else {
            progressDialog.setProgress(progress);
        }
    }

    private void buildProgressDialog(String title, int max) {
        if (!progressDialog.isShowing()) {
            progressDialog.setCancelable(false);
            progressDialog.setMessage(title);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.setMax(max);
            progressDialog.show();
        }
    }

    @Override
    public void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        if (lbm != null)
            lbm.unregisterReceiver(mRecv);
        service.shutdownNow();
    }
}

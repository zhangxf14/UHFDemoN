package com.uhf.uhf.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nativec.tools.ModuleManager;
import com.reader.base.ERROR;
import com.reader.base.ReaderBase;
import com.reader.base.StringTool;
import com.reader.helper.InventoryBuffer;
import com.reader.helper.ReaderHelper;
import com.reader.helper.ReaderSetting;
import com.uhf.uhf.R;
import com.uhf.uhf.adapter.LeftSlideActionBaseAdapter;
import com.uhf.uhf.widget.LeftSlideActionListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestTempInventoryFragment extends Fragment {
    private View root;
    private TextView startStop;

    private LeftSlideActionListView leftSlideActionListView;
    private InventoryTagAdapter inventoryTagAdapter;
    private List<String> epcList = new ArrayList<>();

    private ReaderHelper mReaderHelper;
    private ReaderBase mReader;
    private InventoryBuffer m_curInventoryBuffer;
    private ReaderSetting m_curReaderSetting;

    private LocalBroadcastManager lbm;

    private String epc = "";

    private FragmentMessageListener listener;

    private final String TAG = this.getClass().getSimpleName();

    private boolean mKeyF4Pressing = false;


    private LeftSlideActionBaseAdapter.OnItemActionListener onItemActionListener = new LeftSlideActionBaseAdapter.OnItemActionListener() {
        @Override
        public void onItemRemove(int position) {
            epc = epcList.get(position);
            Log.d(TAG,"EPC:" + epc);
            byte[] values = StringTool.hexStringToByteArray(epc);
            mReader.setTagMask((byte)0xFF,(byte) 0x01,(byte)0x00,(byte)0x00,(byte)0x01,
                    (byte)0x20,(byte)0x60,values);
        }
    };


    private final BroadcastReceiver mRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL)) {
                epcList.clear();
                for (InventoryBuffer.InventoryTagMap tagMap : m_curInventoryBuffer.lsTagList) {
                    epcList.add(tagMap.strEPC);
                }
                inventoryTagAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_TAG_MASK)) {
                if (intent.getByteExtra("status", ERROR.SUCCESS) == ERROR.SUCCESS) {
                    listener.sendMessage(epc);
                } else {
                    Toast.makeText(getContext(),"Set mask failed!",Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.page_reader_fudan_test_temperature, container, false);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        initView(root);
        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (lbm != null) {
            lbm.unregisterReceiver(mRecv);
        }
    }

    private void initView(View root) {
        startStop = root.findViewById(R.id.start_stop);
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mKeyF4Pressing=true;
                startstop();
            }
        });

        leftSlideActionListView = root.findViewById(R.id.test_temp_inventory);
        inventoryTagAdapter = new InventoryTagAdapter(this.getContext(),epcList);
        leftSlideActionListView.setAdapter(inventoryTagAdapter);
        leftSlideActionListView.setOnItemRemoveListener(onItemActionListener);
    }

    private void init() {
        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
            mReader = mReaderHelper.getReader();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        m_curInventoryBuffer = mReaderHelper.getCurInventoryBuffer();
        m_curReaderSetting = mReaderHelper.getCurReaderSetting();


        lbm = LocalBroadcastManager.getInstance(this.getActivity());

        IntentFilter itent = new IntentFilter();
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL);
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_TAG_MASK);
        lbm.registerReceiver(mRecv, itent);
    }

    public void startstop() {
//        refreshStartStop(mKeyF4Pressing);
        if (!startStop.getText().toString()
                .equals(getResources().getString(R.string.start_inventory)) | !mKeyF4Pressing) {
            mReaderHelper.setInventoryFlag(false);
            m_curInventoryBuffer.bLoopInventoryReal = false;
            refreshStartStop(false);
            return;
        }
        clearResult();
        m_curInventoryBuffer.lAntenna.add((byte) 0x00);
        m_curInventoryBuffer.nIndexAntenna = 0;
        m_curInventoryBuffer.bLoopInventoryReal = true;
        m_curInventoryBuffer.btRepeat = 1;
        m_curInventoryBuffer.bLoopCustomizedSession = false;
        refreshStartStop(mKeyF4Pressing);
        mReaderHelper.setInventoryFlag(true);
        mReaderHelper.runLoopInventroy();
    }

    private void clearResult() {
        m_curInventoryBuffer.clearInventoryPar();
        mReaderHelper.clearInventoryTotal();
        m_curInventoryBuffer.clearInventoryRealResult();
        epcList.clear();
        inventoryTagAdapter.notifyDataSetChanged();
    }

    private void refreshStartStop(boolean start) {
        if (start) {
            startStop.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.button_disenabled_background));
            startStop.setText(getResources()
                    .getString(R.string.stop_inventory));
        } else {
            startStop.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.button_background));
            startStop.setText(getResources().getString(
                    R.string.start_inventory));
        }
    }

    private class InventoryTagAdapter extends LeftSlideActionBaseAdapter {

        private List<String> mContentList;
        public InventoryTagAdapter(Context context,List<String> contentList) {
            super(context);
            this.mContentList = contentList;
        }

        @Override
        public int getCount() {
            return mContentList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getSubView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        R.layout.listview_item_customdapter, parent, false);
                holder = new ViewHolder();
                holder.epcText = convertView.findViewById(R.id.epc_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            String itemData = mContentList.get(position);
            holder.epcText.setText(itemData);
            return convertView;
        }
    }

    private class ViewHolder {
        TextView epcText;
    }

    public void setFragmentMessageListener(FragmentMessageListener listener) {
        this.listener = listener;
    }

//    @Override
    public boolean onKeyDown(KeyEvent event) {
        boolean ret = false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
            case KeyEvent.KEYCODE_BACK:
                askForOut();
                break;
            case KeyEvent.KEYCODE_F4: {
                if (!mKeyF4Pressing) {
                    mKeyF4Pressing = true;
                    startstop();
                }
            }

            break;
            default:
                break;
        }
        return ret;
    }

    public boolean onKeyUp(KeyEvent event) {
        boolean ret = false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
            case KeyEvent.KEYCODE_BACK:
                break;
            case KeyEvent.KEYCODE_F4: {
//                if (!mKeyF4Pressing) {
                    mKeyF4Pressing = false;
                    startstop();
//                }
            }

            break;
            default:
                break;
        }
        return ret;
    }
    private void askForOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(getString(R.string.alert_diag_title)).
                setMessage(getString(R.string.are_you_sure_to_exit)).
                setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //close the module
                                ModuleManager.newInstance().setUHFStatus(false);
                                getActivity().getApplication().onTerminate();
                            }
                        }).setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setCancelable(false).show();
    }

}

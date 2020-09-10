package com.uhf.uhf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import com.reader.helper.OperateTagBuffer;
import com.reader.helper.OperateTagBuffer.OperateTagMap;
import com.reader.helper.ReaderHelper;
import com.uhf.uhf.R;
import com.uhf.uhf.adapter.AccessListAdapter;

import java.util.ArrayList;
import java.util.List;


public class FuDanTagList extends LinearLayout {
    private Context mContext;
    private TableRow mTagAccessRow;
    private ImageView mTagAccessImage;
    private TextView mListTextInfo;

    private ReaderHelper mReaderHelper;

    private List<OperateTagMap> data;
    private AccessListAdapter mAccessListAdapter;
    private ListView mTagAccessList;

    private View mTagsAccessListScrollView;
    private WindowManager wm;

    private static OperateTagBuffer m_curOperateTagBuffer;

    public FuDanTagList(Context context, AttributeSet attrs) {
        super(context, attrs);
        initContext(context);
    }

    public FuDanTagList(Context context) {
        super(context);
        initContext(context);
    }

    private void initContext(Context context) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.fudan_tag_list, this);

        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        data = new ArrayList();

        m_curOperateTagBuffer = mReaderHelper.getCurOperateTagBuffer();

        mTagsAccessListScrollView = findViewById(R.id.tags_access_list_scroll_view);
        wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        LayoutParams lp = (LayoutParams) mTagsAccessListScrollView.getLayoutParams();
        //lp.height = 0;
        mTagsAccessListScrollView.setLayoutParams(lp);
        mTagsAccessListScrollView.invalidate();

        mTagAccessRow = (TableRow) findViewById(R.id.table_row_tag_access);
        mTagAccessImage = (ImageView) findViewById(R.id.image_prompt);
        mTagAccessImage.setImageDrawable(getResources().getDrawable(R.drawable.up));
        mListTextInfo = (TextView) findViewById(R.id.list_text_info);
        mListTextInfo.setText(getResources().getString(R.string.open_tag_list));

        mTagAccessRow.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                LayoutParams lp = (LayoutParams) mTagsAccessListScrollView.getLayoutParams();


                /*if (lp.height <= 0) {
                    lp.height = wm.getDefaultDisplay().getHeight() / 3;
                    mTagsAccessListScrollView.setLayoutParams(lp);
                    mTagsAccessListScrollView.invalidate();

                    mTagAccessImage.setImageDrawable(getResources().getDrawable(R.drawable.down));
                    mListTextInfo.setText(getResources().getString(R.string.close_tag_list));
                } else {
                    //mTagsRealListScrollView.setVisibility(View.GONE);

                    lp.height = 0;
                    mTagsAccessListScrollView.setLayoutParams(lp);
                    mTagsAccessListScrollView.invalidate();

                    mTagAccessImage.setImageDrawable(getResources().getDrawable(R.drawable.up));
                    mListTextInfo.setText(getResources().getString(R.string.open_tag_list));
                }*/

                // add by lei.li 2016/11/12 this is method not perfect.
                Log.e("execute this code!", "UUUUUUUUUUUUUUUUUUUUUUUUUUU");
				/*
				if (mTagAccessList.getChildCount() != 0) {
					mTagsAccessListScrollView.findViewById(R.id.tag_access_type)
							.getLayoutParams().width = mTagAccessList.getChildAt(0)
							.getWidth();
					mTagsAccessListScrollView.findViewById(R.id.epc_text)
							.getLayoutParams().width = lengthestData("epc");
					mTagsAccessListScrollView.findViewById(R.id.data_text).getLayoutParams().width = lengthestData("data");
				//	mTagsAccessListScrollView.findViewById(R.id.tag_access_type).invalidate();
					Log.e("execute this code!", "exexexexexexexexexe");
				} */
                // add by lei.li 2016/11/12
            }
        });

        mTagAccessList = (ListView) findViewById(R.id.tag_real_list_view);
        mAccessListAdapter = new AccessListAdapter(mContext, data);
        mTagAccessList.setAdapter(mAccessListAdapter);
    }

    public final void clearBuffer() {
        m_curOperateTagBuffer.clearBuffer();
        refreshList();
    }

    public final void refreshText() {
    }

    public final void refreshList() {
        data.clear();
        data.addAll(m_curOperateTagBuffer.lsTagList);
        mAccessListAdapter.notifyDataSetChanged();
        // add by lei.li 2016/11/12 this contion never
		/*Log.e("execute this code!", "UUUUUUUUUUUUUUUUUUUUUUUUUUU");
		if (mTagAccessList.getChildCount() != 0) {
			mTagsAccessListScrollView.findViewById(R.id.tag_access_type)
					.getLayoutParams().width = mTagAccessList.getChildAt(0)
					.getWidth();
			mTagsAccessListScrollView.findViewById(R.id.epc_text)
					.getLayoutParams().width = lengthestData("epc");
			mTagsAccessListScrollView.findViewById(R.id.data_text).getLayoutParams().width = lengthestData("data");
		//	mTagsAccessListScrollView.findViewById(R.id.tag_access_type).invalidate();
			Log.e("execute this code!", "exexexexexexexexexe");
		}*/
        // add by lei.li 2016/11/12
    }
}

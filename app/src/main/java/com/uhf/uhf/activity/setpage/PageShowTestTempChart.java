package com.uhf.uhf.activity.setpage;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.reader.helper.OperateTagBuffer;
import com.reader.helper.ReaderHelper;
import com.uhf.uhf.R;
import com.uhf.uhf.activity.BaseActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PageShowTestTempChart extends BaseActivity {

    private ReaderHelper mReaderHelper;
    private LineChart lineChart;
    private ImageButton close;

    private TextView status, totalNumber, upperLimit, upperLimitNo, lowerLimit, lowerLimitNo, interval, startTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature_line_chart);
        //((UHFApplication) getApplication()).addActivity(this);

        try {
            mReaderHelper = ReaderHelper.getDefaultHelper();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        initView();
        initData();
    }

    private void initView() {
        lineChart = (LineChart) findViewById(R.id.temperature_display);
        close = (ImageButton) findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        status = (TextView) findViewById(R.id.rtc_test_status);
        totalNumber = (TextView) findViewById(R.id.rtc_test_total_number);
        upperLimit = (TextView) findViewById(R.id.rtc_test_temp_upper_limit);
        upperLimitNo = (TextView) findViewById(R.id.rtc_test_temp_upper_limit_number);
        lowerLimit = (TextView) findViewById(R.id.rtc_test_temp_lower_limit);
        lowerLimitNo = (TextView) findViewById(R.id.rtc_test_temp_lower_limit_number);
        interval = (TextView) findViewById(R.id.rtc_test_temp_interval);
        startTime = (TextView) findViewById(R.id.rtc_test_temp_start_time);
    }

    private void initData() {
        DecimalFormat df = new DecimalFormat("#.00");
        OperateTagBuffer buffer = mReaderHelper.getCurOperateTagBuffer();
        LineData lineData = new LineData();
        lineChart.setData(lineData);
        List<Entry> rawData = new ArrayList<>();
        double max = 0;
        double min = 0;
        int overMaxCount = 0;
        int overMinCount = 0;
        for (int i = 0; i < buffer.points.size(); i++) {
            rawData.add(new Entry((float) (i + 0), Float.parseFloat(df.format(buffer.points.get(i).floatValue()))));
            float temp = buffer.points.get(i).floatValue();
            if (i == 0) {
                max = min = temp;
            } else {
                if (max < temp) {
                    max = temp;
                }
                if (min > temp) {
                    min = temp;
                }

            }
            if (buffer.topTemp < temp) {
                overMaxCount++;
            }
            if (buffer.bottomTemp > temp) {
                overMinCount++;
            }
        }
        LineDataSet dataSet = new LineDataSet(rawData, "℃");
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setCircleRadius(3);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return new DecimalFormat("#.00").format(value);
            }
        });

        lineChart.getAxisRight().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return new DecimalFormat("#.00").format(value);
            }
        });

        lineData.addDataSet(dataSet);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();

        status.setText(buffer.testingCount != 0 ? "Testing" : "End");
        totalNumber.setText(buffer.TempCount + "/" + buffer.testTempCount);
        upperLimit.setText(buffer.topTemp + "℃");
        upperLimitNo.setText(overMaxCount + "");
        lowerLimit.setText(buffer.bottomTemp + "℃");
        lowerLimitNo.setText(overMinCount + "");
        interval.setText(buffer.testTempInterval + "S");
        startTime.setText(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(buffer.timeStamp * 1000L)));
    }

}

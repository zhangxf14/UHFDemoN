package com.uhf.uhf.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import com.github.mikephil.charting.charts.LineChart;
import com.uhf.uhf.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemperatureChartDialog {
    private AlertDialog.Builder builder;
    private int layout;
    private AlertDialog dialog;
    private Context mContext;
    private Window window;

    private LineChart lineChart;

    private List<Map.Entry> xyAxis = new ArrayList<>();

    public TemperatureChartDialog(Context context, int layout) {
        builder = new AlertDialog.Builder(context);
        mContext = context;
        this.layout = layout;
    }

    @Deprecated
    public TemperatureChartDialog(Context context, int theme, int layout) {
        builder = new AlertDialog.Builder(context, theme);
        mContext = context;
        this.layout = layout;
    }

    private void initView() {
        lineChart = (LineChart) getViewById(R.id.temperature_display);

    }

    private void initData() {
        for (int i = 0; i < 12; i++);
            //xyAxis.add(new Map.Entry(i, new Random().nextInt(300)));
    }

    public AlertDialog.Builder getBuilder() {
        return builder;
    }

    /**
     * get the object of window
     *
     * @return
     */
    public Window getDialog() {
        dialog = builder.create();
        dialog.setView(new EditText(mContext));
        dialog.show();
        window = dialog.getWindow();
        window.setContentView(layout);
        return window;
    }

    /**
     * get view via the id of view
     *
     * @param id
     * @return
     */
    public View getViewById(int id) {
        if (dialog == null) getDialog();
        return dialog.findViewById(id);
    }

    /**
     * close the dialog
     */
    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}

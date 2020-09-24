package com.sshdaemon.util;

import android.content.Context;
import android.widget.TableRow;
import android.widget.TextView;

public class TextViewHelper {

    public static TextView createTextView(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        return textView;
    }
}

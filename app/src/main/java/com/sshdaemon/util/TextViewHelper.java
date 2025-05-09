package com.sshdaemon.util;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TableRow;
import android.widget.TextView;

public class TextViewHelper {

    public static TextView createTextView(Context context, String text) {
        var textView = new TextView(context);
        textView.setText(text);
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        textView.setSingleLine();
        textView.setTextSize(11);
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        return textView;
    }
}

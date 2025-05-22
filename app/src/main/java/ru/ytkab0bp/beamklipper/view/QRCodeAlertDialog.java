package ru.ytkab0bp.beamklipper.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import ru.ytkab0bp.beamklipper.R;
import ru.ytkab0bp.beamklipper.utils.ViewUtils;

public class QRCodeAlertDialog extends MaterialAlertDialogBuilder {
    public QRCodeAlertDialog(@NonNull Context context, String link) {
        super(context);

        setTitle(R.string.QRCode);
        setPositiveButton(R.string.QROpen, (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))));
        setNegativeButton(R.string.QRCancel, null);
        setNeutralButton(R.string.QRCopy, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", link);
            clipboard.setPrimaryClip(clip);
        });

        FrameLayout fl = new FrameLayout(context);
        ImageView iv = new ImageView(context);
        QRGEncoder encoder = new QRGEncoder(link, null, QRGContents.Type.TEXT, ViewUtils.dp(250));
        encoder.setColorWhite(ViewUtils.resolveColor(context, android.R.attr.textColorPrimary));
        encoder.setColorBlack(0);
        iv.setImageBitmap(encoder.getBitmap());
        fl.addView(iv, new FrameLayout.LayoutParams(ViewUtils.dp(250), ViewUtils.dp(250), Gravity.CENTER));
        setView(fl);
    }
}

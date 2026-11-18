package com.example.pdfpocket;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final int PICK_PDF = 41;
    private PdfRenderer renderer;
    private ParcelFileDescriptor fileDescriptor;
    private int pageIndex = 0;
    private TextView title, pageLabel, emptyLabel;
    private Button prevButton, nextButton;
    private ZoomImageView pageImage;
    private LinearLayout readerPanel;
    private String documentName = "No document open";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        if (getIntent() != null && Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            if (uri != null) openUri(uri);
        }
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244, 246, 250));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18), dp(12), dp(18), dp(12));
        top.setBackgroundColor(Color.rgb(18, 28, 48));
        top.setOrientation(LinearLayout.VERTICAL);

        TextView brand = text("PDF POCKET", 12, Color.rgb(155, 190, 255));
        brand.setLetterSpacing(0.12f);
        title = text(documentName, 18, Color.WHITE);
        title.setMaxLines(1);
        top.addView(brand);
        top.addView(title);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(76)));

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(12), dp(8), dp(12), dp(8));
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setBackgroundColor(Color.WHITE);
        Button open = button("＋ Open PDF");
        open.setOnClickListener(v -> choosePdf());
        actions.addView(open, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button reset = button("Reset zoom");
        reset.setOnClickListener(v -> { if (pageImage != null) pageImage.resetZoom(); });
        actions.addView(reset, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(actions);

        readerPanel = new LinearLayout(this);
        readerPanel.setOrientation(LinearLayout.VERTICAL);
        readerPanel.setGravity(Gravity.CENTER);
        readerPanel.setPadding(dp(8), dp(8), dp(8), dp(8));

        emptyLabel = text("Your PDFs, in one place.\n\nTap “Open PDF” to choose a file\nfrom your phone.", 18, Color.rgb(92, 103, 122));
        emptyLabel.setGravity(Gravity.CENTER);
        readerPanel.addView(emptyLabel, new LinearLayout.LayoutParams(-1, -1));
        root.addView(readerPanel, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8), dp(6), dp(8), dp(10));
        footer.setBackgroundColor(Color.WHITE);
        prevButton = button("‹ Previous");
        prevButton.setOnClickListener(v -> showPage(pageIndex - 1));
        pageLabel = text("Page 0 / 0", 14, Color.DKGRAY);
        pageLabel.setGravity(Gravity.CENTER);
        nextButton = button("Next ›");
        nextButton.setOnClickListener(v -> showPage(pageIndex + 1));
        footer.addView(prevButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        footer.addView(pageLabel, new LinearLayout.LayoutParams(0, dp(48), 1));
        footer.addView(nextButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(footer);
        setContentView(root);
        updateControls();
    }

    private void choosePdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (Exception ignored) {}
            openUri(uri);
        }
    }

    private void openUri(Uri uri) {
        closeDocument();
        try {
            fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (fileDescriptor == null) throw new IOException("Unable to open this file.");
            renderer = new PdfRenderer(fileDescriptor);
            if (renderer.getPageCount() == 0) throw new IOException("This PDF has no pages.");
            pageIndex = 0;
            String last = uri.getLastPathSegment();
            documentName = (last == null || last.trim().isEmpty()) ? "PDF document" : last;
            title.setText(documentName);
            renderCurrentPage();
        } catch (Exception e) {
            closeDocument();
            Toast.makeText(this, "Could not open PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderCurrentPage() {
        if (renderer == null) return;
        try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
            int maxWidth = Math.max(dp(320), getResources().getDisplayMetrics().widthPixels - dp(32));
            float ratio = (float) page.getHeight() / (float) page.getWidth();
            int width = Math.min(maxWidth, 1800);
            int height = Math.max(1, (int)(width * ratio));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            if (pageImage == null) {
                readerPanel.removeAllViews();
                pageImage = new ZoomImageView(this);
                pageImage.setBackgroundColor(Color.rgb(225, 229, 236));
                readerPanel.addView(pageImage, new LinearLayout.LayoutParams(-1, -1));
            }
            pageImage.setImageBitmap(bitmap);
            pageImage.resetZoom();
            emptyLabel = null;
            updateControls();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to render page.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPage(int index) {
        if (renderer == null) return;
        if (index < 0 || index >= renderer.getPageCount()) return;
        pageIndex = index;
        renderCurrentPage();
    }

    private void updateControls() {
        int count = renderer == null ? 0 : renderer.getPageCount();
        pageLabel.setText("Page " + (count == 0 ? 0 : pageIndex + 1) + " / " + count);
        prevButton.setEnabled(count > 0 && pageIndex > 0);
        nextButton.setEnabled(count > 0 && pageIndex < count - 1);
        prevButton.setAlpha(prevButton.isEnabled() ? 1f : 0.45f);
        nextButton.setAlpha(nextButton.isEnabled() ? 1f : 0.45f);
    }

    private void closeDocument() {
        if (renderer != null) { try { renderer.close(); } catch (Exception ignored) {} renderer = null; }
        if (fileDescriptor != null) { try { fileDescriptor.close(); } catch (Exception ignored) {} fileDescriptor = null; }
        pageImage = null;
        if (readerPanel != null) {
            readerPanel.removeAllViews();
            emptyLabel = text("Your PDFs, in one place.\n\nTap “Open PDF” to choose a file\nfrom your phone.", 18, Color.rgb(92, 103, 122));
            emptyLabel.setGravity(Gravity.CENTER);
            readerPanel.addView(emptyLabel, new LinearLayout.LayoutParams(-1, -1));
        }
        updateControls();
    }

    @Override protected void onDestroy() {
        closeDocument();
        super.onDestroy();
    }

    private static class ZoomImageView extends ImageView {
        private final ScaleGestureDetector detector;
        private float scale = 1f;
        private final Matrix matrix = new Matrix();
        private float startX, startY;
        private float translateX, translateY;

        ZoomImageView(Activity activity) {
            super(activity);
            setScaleType(ScaleType.MATRIX);
            detector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    scale = Math.max(1f, Math.min(5f, scale * d.getScaleFactor()));
                    applyMatrix();
                    return true;
                }
            });
        }

        void resetZoom() {
            scale = 1f; translateX = 0; translateY = 0;
            post(this::applyMatrix);
        }

        private void applyMatrix() {
            if (getDrawable() == null) return;
            float vw = getWidth(), vh = getHeight();
            float dw = getDrawable().getIntrinsicWidth(), dh = getDrawable().getIntrinsicHeight();
            if (vw <= 0 || vh <= 0 || dw <= 0 || dh <= 0) return;
            float fit = Math.min(vw / dw, vh / dh);
            float s = fit * scale;
            matrix.setScale(s, s);
            float x = (vw - dw * s) / 2f + translateX;
            float y = (vh - dh * s) / 2f + translateY;
            matrix.postTranslate(x, y);
            setImageMatrix(matrix);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            detector.onTouchEvent(event);
            if (event.getPointerCount() == 1 && scale > 1f) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = event.getX() - translateX;
                    startY = event.getY() - translateY;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    translateX = event.getX() - startX;
                    translateY = event.getY() - startY;
                    applyMatrix();
                }
            }
            return true;
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            applyMatrix();
        }
    }
}

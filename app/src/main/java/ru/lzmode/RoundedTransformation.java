package ru.lzmode;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.squareup.picasso.Transformation;

public class RoundedTransformation implements Transformation {
    private final int radius;
    private final int margin;

    public RoundedTransformation(int radius, int margin) {
        this.radius = radius;
        this.margin = margin;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(source, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));

        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawRoundRect(margin, margin, source.getWidth() - margin, source.getHeight() - margin, radius, radius, paint);

        if (source != output) {
            source.recycle();
        }

        return output;
    }

    @Override
    public String key() {
        return "rounded(radius=" + radius + ", margin=" + margin + ")";
    }
}

package com.example.syncrise;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SleepCycleVisualizerView extends View {

    private Paint paintLine;
    private Path path;

    public SleepCycleVisualizerView(Context context) {
        super(context);
        init();
    }

    public SleepCycleVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SleepCycleVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLine.setColor(Color.parseColor("#00F0FF")); // Neon cyan tone
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(12f);

        path = new Path();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        path.reset();
        
        float midY = height / 2f;
        float amplitude = height / 3f;
        
        // Let's draw a stylized sine-wave representing 3 sleep cycles (90 minutes each)
        int numCycles = 3;
        float cycleWidth = width / (float) numCycles;

        path.moveTo(0, midY);

        for (int i = 0; i < numCycles; i++) {
            float startX = i * cycleWidth;
            
            // Draw one cycle (Light sleep -> Deep sleep -> REM)
            // Cubic bezier curve approximation for smooth waves
            path.cubicTo(
                startX + cycleWidth * 0.25f, midY + amplitude, // Control point 1 (Deep sleep dip)
                startX + cycleWidth * 0.75f, midY - amplitude, // Control point 2 (REM sleep peak)
                startX + cycleWidth, midY                      // End point
            );
        }

        canvas.drawPath(path, paintLine);
    }
}
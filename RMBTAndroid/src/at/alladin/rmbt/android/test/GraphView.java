/*******************************************************************************
 * Copyright 2013 alladin-IT OG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.alladin.rmbt.android.test;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import at.alladin.openrmbt.android.R;

public class GraphView extends View
{
    private boolean recycled;
    
    final List<Graph> graphs = new ArrayList<Graph>();
    final int width;
    final int height;
    
    private float scale = 1f;
    
    private final Paint bitmapPaint;
    
    final int graphWidth;
    final int graphHeight;
    final float graphStrokeWidth;
    
    final Bitmap genBackgroundBitmap;
    final Bitmap gridBitmap;
    final float gridX;
    final float gridY;
    final float graphX;
    final float graphY;
    
    private final Bitmap reflectionBitmap;
    private final float reflectionX;
    private final float reflectionY;
    
    public GraphView(final Context context, final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);
        
        final Resources res = context.getResources();
        
        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);
        
        final int relW = 593;
        final int relH = 237;
        
        final Bitmap backgroundBitmap = getBitmap(res, R.drawable.test_box_small);
        width = backgroundBitmap.getWidth();
        height = backgroundBitmap.getHeight();
        genBackgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(genBackgroundBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, bitmapPaint);
        
        if (! isInEditMode())
            backgroundBitmap.recycle();
        
        reflectionBitmap = getBitmap(res, R.drawable.test_box_reflection_small);
        reflectionX = coordFW(7, relW);
        reflectionY = coordFH(7, relH);
        
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(coordFH(15, relH));
        paint.setColor(Color.parseColor("#C8ffffff"));
        paint.setTextAlign(Align.LEFT);
        canvas.drawText("0", coordFW(88, relW), coordFH(220, relH), paint);
        paint.setTextAlign(Align.RIGHT);
        canvas.drawText("8", coordFW(567, relW), coordFH(220, relH), paint);
        paint.setTextAlign(Align.CENTER);
        canvas.drawText("sec", coordFW(326, relW), coordFH(220, relH), paint);
        
        paint.setTextAlign(Align.LEFT);
        paint.setColor(Color.parseColor("#C800f940"));
        canvas.drawText(String.format("– %s", res.getString(R.string.test_mbps)), coordFW(9, relW), coordFH(110, relH),
                paint);
        paint.setColor(Color.parseColor("#C8f8a000"));
        canvas.drawText(String.format("– %s", res.getString(R.string.test_dbm)), coordFW(9, relW), coordFH(130, relH),
                paint);
        
        paint.setTextAlign(Align.RIGHT);
        paint.setColor(Color.parseColor("#C800f940"));
        canvas.drawText("0", coordFW(72, relW), coordFH(220, relH), paint);
        canvas.drawText("100", coordFW(72, relW), coordFH(38, relH), paint);
        paint.setColor(Color.parseColor("#C8f8a000"));
        canvas.drawText("-110", coordFW(72, relW), coordFH(195, relH), paint);
        canvas.drawText("-30", coordFW(72, relW), coordFH(58, relH), paint);
        
        gridBitmap = getBitmap(res, R.drawable.test_grid);
        gridX = coordFW(55, relW);
        gridY = coordFH(16, relH);
        graphX = coordFW(80, relW);
        graphY = coordFH(16, relH);
        graphWidth = coordW(493, relW);
        graphHeight = coordH(183, relH);
        graphStrokeWidth = coordFW(4, relW);
    }
    
    public GraphView(final Context context, final AttributeSet attrs)
    {
        this(context, attrs, 0);
    }
    
    public GraphView(final Context context)
    {
        this(context, null, 0);
    }
    
    protected Bitmap getBitmap(Resources res, int id)
    {
        final BitmapDrawable drawable = (BitmapDrawable) res.getDrawable(id);
        return drawable.getBitmap();
    }
    
    protected float coordFW(final int x, final int y)
    {
        return (float) x / y * width;
    }
    
    protected float coordFH(final int x, final int y)
    {
        return (float) x / y * height;
    }
    
    protected int coordW(final int x, final int y)
    {
        return Math.round((float) x / y * width);
    }
    
    protected int coordH(final int x, final int y)
    {
        return Math.round((float) x / y * height);
    }
    
    public int getGraphWidth()
    {
        return graphWidth;
    }
    
    public int getGraphHeight()
    {
        return graphHeight;
    }
    
    public float getGraphStrokeWidth()
    {
        return graphStrokeWidth;
    }
    
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
    {
        final int paddingH = getPaddingLeft() + getPaddingRight();
        final int paddingW = getPaddingTop() + getPaddingBottom();
        
        // super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = MeasureSpec.getSize(widthMeasureSpec);
        final int newW = width + paddingH;
        switch (MeasureSpec.getMode(widthMeasureSpec))
        {
        case MeasureSpec.AT_MOST:
            if (newW < w)
                w = newW;
            break;
        
        case MeasureSpec.EXACTLY:
            break;
        
        case MeasureSpec.UNSPECIFIED:
            w = newW;
            break;
        }
        scale = (float) (w - getPaddingLeft() - getPaddingRight()) / width;
        
        int h = MeasureSpec.getSize(heightMeasureSpec);
        final int newH = Math.round(height * scale) + paddingW;
        switch (MeasureSpec.getMode(heightMeasureSpec))
        {
        case MeasureSpec.AT_MOST:
            if (newH < h)
                h = newH;
            break;
        
        case MeasureSpec.EXACTLY:
            break;
        
        case MeasureSpec.UNSPECIFIED:
            h = newH;
            break;
        }
        
        setMeasuredDimension(w, h);
    }
    
    @Override
    protected void onDraw(final Canvas canvas)
    {
        if (recycled)
            return;
        
        final int canvasSave = canvas.save();
        
        canvas.translate(getPaddingLeft(), getPaddingTop());
        canvas.scale(scale, scale);
        
        canvas.drawBitmap(genBackgroundBitmap, 0, 0, bitmapPaint);
        canvas.drawBitmap(gridBitmap, gridX, gridY, bitmapPaint);
        
        final int canvasSave2 = canvas.save();
        canvas.translate(graphX, graphY);
        
        for (final Graph graph : graphs)
            graph.draw(canvas);
        
        canvas.restoreToCount(canvasSave2);
        
        canvas.drawBitmap(reflectionBitmap, reflectionX, reflectionY, bitmapPaint);
        
        canvas.restoreToCount(canvasSave);
    }
    
    public void addGraph(final Graph graph)
    {
        graphs.add(graph);
    }
    
    public void recycle()
    {
        recycled = true;
        genBackgroundBitmap.recycle();
        gridBitmap.recycle();
    }
}

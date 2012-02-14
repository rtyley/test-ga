package com.github.mobile.gauges.ui.airtraffic;

import static android.graphics.Bitmap.createScaledBitmap;
import static java.lang.Math.PI;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.github.mobile.gauges.R.color;
import com.github.mobile.gauges.R.drawable;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.animation.ValueAnimator.AnimatorUpdateListener;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * View to display an Air Traffic map
 */
public class AirTrafficView extends View {

    /**
     * The maximum numbers of hits to retain
     */
    private static final int MAX_HITS = 500;

    /**
     * Size scales the ring goes through while being animated
     */
    private static final float[] RING_SIZES = new float[] { .5F, .625F, .75F, .875F, 1.0F };

    /**
     * Ring Animation
     */
    public class RingAnimation {

        private int state;

        private final Bitmap ring;

        private final float x;

        private final float y;

        /**
         * Create animation for hit
         *
         * @param x
         * @param y
         * @param ring
         */
        public RingAnimation(final float x, final float y, final Bitmap ring) {
            this.ring = ring;
            this.x = x;
            this.y = y;
        }

        /**
         * @param state
         */
        public void setState(final int state) {
            this.state = state;
        }

        /**
         * @return size
         */
        public int getState() {
            return state;
        }

        /**
         * Draw ring on canvas
         *
         * @param canvas
         * @param ringPaint
         *            paint used for drawing rings, won't affect other UI elements
         */
        public void onDraw(final Canvas canvas, final Paint ringPaint) {
            if (state >= RING_SIZES.length)
                return;

            RectF destination = new RectF();
            int width = Math.round(ringWidth * RING_SIZES[state]);
            int height = Math.round(ringHeight * RING_SIZES[state]);
            destination.top = y - height / 2;
            destination.left = x - width / 2;
            destination.right = destination.left + width;
            destination.bottom = destination.top + height;

            ringPaint.setAlpha(Math.round(255F - (((float) state / RING_SIZES.length) * 255F)));
            canvas.drawBitmap(ring, resourceProvider.getRingBounds(), destination, ringPaint);
        }
    }

    private static final String MAP_LABEL = "AirTraffic Live";

    /**
     * Divisor used to compute the scaling value
     * <p>
     * Constant taken from gaug.es site
     */
    private static final double SCALE_DIVISOR = 720.0;

    /**
     * Corrector used to adjust X position
     */
    private static final double X_CORRECTOR = 1.1;

    /**
     * Corrector used to adjust Y position
     */
    private static final double Y_CORRECTOR = 70.0;

    /**
     * Multiplier used to compute the scaling value
     * <p>
     * Constant taken from gaug.es site
     */
    private static final double SCALE_MULTIPLIER = 0.169;

    /**
     * Constant taken from gaug.es site
     */
    private static final double PIXELS_PER_LONGITUDE_DEGREE = 16.0 / 360.0;

    /**
     * Constant taken from gaug.es site
     */
    private static final double NEGATIVE_PIXELS_PER_LONGITUDE_RADIAN = -(16.0 / (2.0 * PI));

    /**
     * Constant taken from gaug.es site
     */
    private static final double BITMAP_ORIGIN = 16.0 / 2.0;

    private AirTrafficResourceProvider resourceProvider;

    /**
     * Scale value used based on map image dimensions
     */
    private double scale;

    /**
     * Correction value used to adjust scaled y position
     */
    private double yCorrector;

    /**
     * Correction value used to adjust scaled x position
     */
    private double xCorrector;

    private int pinHeight;

    private int pinWidth;

    private int ringHeight;

    private int ringWidth;

    private float mapLabelWidth;

    private boolean running = true;

    private final Collection<ObjectAnimator> rings = new ConcurrentLinkedQueue<ObjectAnimator>();

    private double xMapScale;

    private double yMapScale;

    private Bitmap map;

    private Bitmap fittedMap;

    private final Paint mapPaint = new Paint();

    private final Paint ringPaint = new Paint();

    private final Queue<Hit> hits = new ConcurrentLinkedQueue<Hit>();

    /**
     * Constructor. Create objects used throughout the life of the View: the Paint and the animator
     *
     * @param context
     * @param attrs
     */
    public AirTrafficView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Resources resources = getResources();
        map = BitmapFactory.decodeResource(resources, drawable.map);

        mapPaint.setColor(resources.getColor(color.text));
        mapPaint.setAntiAlias(true);
        mapPaint.setSubpixelText(true);
        mapPaint.setFilterBitmap(true);

        ringPaint.setAntiAlias(true);
        ringPaint.setFilterBitmap(true);
    }

    /**
     * Set the height to use when drawing text on the map
     *
     * @param height
     * @return this view
     */
    public AirTrafficView setLabelHeight(final float height) {
        mapPaint.setTextSize(height);
        mapLabelWidth = mapPaint.measureText(MAP_LABEL);
        return this;
    }

    /**
     * Set resource provider
     *
     * @param provider
     * @return this view
     */
    public AirTrafficView setResourceProvider(final AirTrafficResourceProvider provider) {
        this.resourceProvider = provider;

        pinHeight = provider.getPinHeight();
        pinWidth = provider.getPinWidth();

        ringHeight = provider.getRingHeight();
        ringWidth = provider.getRingWidth();

        return this;
    }

    @Override
    protected void onSizeChanged(final int width, final int height, final int oldw, final int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);

        xMapScale = (double) width / map.getWidth();
        yMapScale = (double) height / map.getHeight();

        double relativeWidth = map.getWidth() / SCALE_DIVISOR;
        scale = relativeWidth * SCALE_MULTIPLIER;

        xCorrector = X_CORRECTOR * relativeWidth;
        yCorrector = Y_CORRECTOR * relativeWidth;

        fittedMap = createScaledBitmap(map, width, height, true);

        hits.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (fittedMap != null)
            canvas.drawBitmap(fittedMap, 0, 0, mapPaint);

        if (mapLabelWidth > 0)
            canvas.drawText(MAP_LABEL, fittedMap.getWidth() / 2 - mapLabelWidth / 2,
                    fittedMap.getHeight() - mapPaint.getTextSize(), mapPaint);

        for (Hit hit : hits)
            drawPin(hit, canvas);

        for (ObjectAnimator ring : rings)
            ((RingAnimation) ring.getTarget()).onDraw(canvas, ringPaint);
    }

    /**
     * Calculate the x location of the given hit on the map
     *
     * @param hit
     * @return x coordinate
     */
    protected float calculateScreenX(final Hit hit) {
        // Determine the x positions to draw the hit at.
        // This code was taken from the gaug.es site
        double globalX = (BITMAP_ORIGIN + hit.lon * PIXELS_PER_LONGITUDE_DEGREE) * 256.0;
        float x = (float) ((globalX * scale) - xCorrector);

        // Take absolute positions on actual map and scale to actual screen size since map image may have been
        // scaled
        return (float) (x * xMapScale);
    }

    /**
     * Calculate the y location of the given hit on the map
     *
     * @param hit
     * @return y coordinate
     */
    protected float calculateScreenY(final Hit hit) {
        // Determine the x and y positions to draw the hit at.
        // This code was taken from the gaug.es site
        double e = Math.sin(hit.lat * (PI / 180.0));
        e = Math.max(Math.min(e, 0.9999), -0.9999);
        double globalY = (BITMAP_ORIGIN + 0.5 * Math.log((1.0 + e) / (1.0 - e)) * NEGATIVE_PIXELS_PER_LONGITUDE_RADIAN) * 256.0;

        float y = (float) ((globalY * scale) - yCorrector);

        // Take absolute positions on actual map and scale to actual screen size since map image may have been
        // scaled
        return (float) (y * yMapScale);
    }

    private void drawPin(Hit hit, Canvas canvas) {
        // Find the color index for the given site id
        int key = resourceProvider.getKey(hit.siteId);
        if (key == -1)
            return;

        Bitmap pin = resourceProvider.getPin(key);
        RectF destination = new RectF();
        destination.top = calculateScreenY(hit) - pinHeight / 2;
        destination.left = calculateScreenX(hit) - pinWidth / 2;
        destination.right = destination.left + pinWidth;
        destination.bottom = destination.top + pinHeight;
        canvas.drawBitmap(pin, resourceProvider.getPinBounds(), destination, mapPaint);
    }

    /**
     * Add hit to view
     *
     * @param newHit
     */
    public void addHit(Hit newHit) {
        if (!running)
            return;

        hits.add(newHit);
        while (hits.size() >= MAX_HITS)
            hits.poll();

        int key = resourceProvider.getKey(newHit.siteId);
        if (key == -1)
            return;

        ObjectAnimator animator = ObjectAnimator.ofInt(new RingAnimation(calculateScreenX(newHit),
                calculateScreenY(newHit), resourceProvider.getRing(key)), "state", 0, RING_SIZES.length);
        animator.setDuration(500);
        animator.addListener(new AnimatorListenerAdapter() {

            public void onAnimationEnd(Animator animation) {
                rings.remove(animation);
            }
        });
        animator.addUpdateListener(new AnimatorUpdateListener() {

            public void onAnimationUpdate(ValueAnimator animation) {
                postInvalidate();
            }
        });
        animator.start();

        rings.add(animator);
        postInvalidate();
    }

    /**
     * Pause the animated view
     */
    public void pause() {
        running = false;
        for (ObjectAnimator animator : rings)
            animator.end();
        rings.clear();
    }

    /**
     * Resume the animated view
     */
    public void resume() {
        running = true;
    }
}

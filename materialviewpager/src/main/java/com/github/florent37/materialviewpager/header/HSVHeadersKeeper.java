package com.github.florent37.materialviewpager.header;

import android.content.Context;
import android.graphics.Color;

import com.github.florent37.materialviewpager.Utils;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by Arthur Korchagin on 14.01.16
 */
public class HSVHeadersKeeper extends HeadersKeeper {
    private final LinkedList<float[]> mHSVValues;
    private final LinkedList<float[]> mHSVDiffs;

    public HSVHeadersKeeper(Context context, Collection<HeaderDesign> headerDesigns) {
        super(headerDesigns);

        mHSVValues = new LinkedList<>();
        mHSVDiffs = new LinkedList<>();

        for (HeaderDesign design : mHeaders) {
            mHSVValues.add(Utils.buildHSV(context.getResources().getColor(design.getColorRes())));
        }

        for (float[] fromColor : mHSVValues) {
            int index = mHSVValues.indexOf(fromColor);
            if (mHSVValues.size() > (index + 1)) {
                float[] toColor = mHSVValues.get(index + 1);

                final float[] diff = new float[3];

                diff[0] = toColor[0] - fromColor[0];
                diff[1] = toColor[1] - fromColor[1];
                diff[2] = toColor[2] - fromColor[2];

                mHSVDiffs.add(diff);
            } else {
                mHSVDiffs.add(new float[3]);
            }
        }
    }

    @Override
    public int getColorWithOffset(int position, float offset) {

        int fromPos = (int) Math.floor((double) (position + offset));

        float[] fromColor = mHSVValues.get(fromPos);
        float[] diff = mHSVDiffs.get(fromPos);

        final float[] hsv = new float[]{
                fromColor[0] + diff[0] * offset,
                fromColor[1] + diff[1] * offset,
                fromColor[2] + diff[2] * offset
        };

        return Color.HSVToColor(hsv);

    }
}

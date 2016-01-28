package com.github.florent37.materialviewpager.header;

import android.content.Context;

import com.github.florent37.materialviewpager.Utils;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by Arthur Korchagin on 14.01.16
 */
public class RGBHeadersKeeper extends HeadersKeeper {
    private final LinkedList<float[]> mRGBValues;
    private final LinkedList<float[]> mRGBDiffs;

    public RGBHeadersKeeper(Context context, Collection<HeaderDesign> headerDesigns) {
        super(headerDesigns);

        mRGBValues = new LinkedList<>();
        mRGBDiffs = new LinkedList<>();

        for (HeaderDesign design : mHeaders) {
            mRGBValues.add(Utils.buildRGB(design.getColor()));
        }

        for (float[] fromColor : mRGBValues) {
            int index = mRGBValues.indexOf(fromColor);
            if (mRGBValues.size() > (index + 1)) {
                float[] toColor = mRGBValues.get(index + 1);

                final float[] diff = new float[3];

                diff[0] = toColor[0] - fromColor[0];
                diff[1] = toColor[1] - fromColor[1];
                diff[2] = toColor[2] - fromColor[2];

                mRGBDiffs.add(diff);
            } else {
                mRGBDiffs.add(new float[3]);
            }
        }
    }

    @Override
    public int getColorWithOffset(int position, float offset) {

        int fromPos = (int) Math.floor((double) (position + offset));

        float[] fromColor = mRGBValues.get(fromPos);
        float[] diff = mRGBDiffs.get(fromPos);

        final int[] rgb = new int[]{
                Math.round(fromColor[0] + diff[0] * offset),
                Math.round(fromColor[1] + diff[1] * offset),
                Math.round(fromColor[2] + diff[2] * offset)
        };

        return Utils.rgbToColor(rgb);

    }
}

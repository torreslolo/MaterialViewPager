package com.github.florent37.materialviewpager.header;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.github.florent37.materialviewpager.R;
import com.squareup.picasso.Picasso;

public class ImagesPagerAdapter extends PagerAdapter {

    private final HeadersKeeper mHeadersKeeper;
    Context mContext;
    LayoutInflater mLayoutInflater;

    public ImagesPagerAdapter(Context context, HeadersKeeper headersKeeper) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mHeadersKeeper = headersKeeper;
    }

    @Override
    public int getCount() {
        return mHeadersKeeper.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.header_pager_image, container, false);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.iv_image);
//        imageView.setImageResource(mImageList.get(position));
        container.addView(itemView);

        Picasso.with(container.getContext())
                .load(mHeadersKeeper.getHeaderDesign(position).getImageUrl())
                .into(imageView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((FrameLayout) object);
    }
}
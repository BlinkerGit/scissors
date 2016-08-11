/*
 * Copyright (C) 2015 Lyft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lyft.android.scissors;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

/**
 * A {@link BitmapLoader} with transformation for {@link Glide} image library.
 *
 * @see GlideBitmapLoader#createUsing(CropView)
 * @see GlideBitmapLoader#createUsing(CropView, RequestManager, BitmapPool)
 */
public class GlideBitmapLoader implements BitmapLoader {

    private final RequestManager requestManager;
    private final GlideFillViewportTransformation transformation;
    private final CropView.OnImageLoadListener listener;

    public GlideBitmapLoader(@NonNull RequestManager requestManager, @NonNull GlideFillViewportTransformation transformation, @Nullable CropView.OnImageLoadListener listener) {
        this.requestManager = requestManager;
        this.transformation = transformation;
        this.listener = listener;
    }

    @Override
    public void load(@Nullable Object model, @NonNull ImageView imageView) {
        load(model, imageView, 1f);
    }

    private void load(@Nullable final Object model, @NonNull final ImageView imageView, final float scale) {
        requestManager.load(model)
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.RESULT)
            .imageDecoder(new OOMReadyStreamBitmapDecoder(imageView.getContext()))
            .sizeMultiplier(scale)
            .transform(transformation)
            .into(new BitmapImageViewTarget(imageView) {
                @Override
                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                    if (e.getCause() instanceof OutOfMemoryError) {
                        // Don't drop below 70% quality
                        if (scale * .9f > .7) {
                            Log.e("GlideBitmapLoader", "Exception occurred, scaling back image to: " + (scale * 90) + "%");
                            load(model, imageView, scale * .9f);
                        } else {
                            Log.e("GlideBitmapLoader", "Loading image failed permanently. Please free up memory and try again.");
                            if (listener != null) {
                                listener.onLoadFail();
                            }
                            throw new RuntimeException(e);
                        }
                    }
                    super.onLoadFailed(e, errorDrawable);
                }

                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    super.onResourceReady(resource, glideAnimation);
                    if (listener != null) {
                        listener.onLoadSuccess();
                    }
                }
            });
    }

    public static BitmapLoader createUsing(@NonNull CropView cropView, CropView.OnImageLoadListener listener) {
        return createUsing(cropView, Glide.with(cropView.getContext()), Glide.get(cropView.getContext()).getBitmapPool(), listener);
    }

    public static BitmapLoader createUsing(@NonNull CropView cropView, @NonNull RequestManager requestManager,
                                           @NonNull BitmapPool bitmapPool, @Nullable CropView.OnImageLoadListener listener) {
        return new GlideBitmapLoader(requestManager,
                GlideFillViewportTransformation.createUsing(bitmapPool, cropView.getViewportWidth(), cropView.getViewportHeight()), listener);
    }
}

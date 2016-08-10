package com.lyft.android.scissors;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;

import java.io.InputStream;

public class OOMReadyStreamBitmapDecoder extends StreamBitmapDecoder {
  public OOMReadyStreamBitmapDecoder(Context context) {
    super(context);
  } // or any other ctor you want from super
  @Override public Resource<Bitmap> decode(InputStream source, int width, int height) throws OutOfMemoryError {
    return super.decode(source, width, height);
  }
}

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
package com.lyft.android.scissorssample;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.lyft.android.scissors.CropView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static android.graphics.Bitmap.CompressFormat.JPEG;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

public class MainActivity extends Activity {

  private static final float[] ASPECT_RATIOS = {0f, 1f, 6f / 4f, 16f / 9f};

  private static final String[] ASPECT_LABELS = {"\u00D8", "1:1", "6:4", "16:9"};

  CropView cropView;

  List<View> buttons;

  View pickButton;

  CompositeSubscription subscriptions = new CompositeSubscription();

  private int selectedRatio = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    setContentView(R.layout.activity_main);

    pickButton = findViewById(R.id.pick_fab);
    cropView = findViewById(R.id.crop_view);
    buttons = new ArrayList<>(3);
    FloatingActionButton crop = findViewById(R.id.crop_fab);
    buttons.add(crop);
    FloatingActionButton mini = findViewById(R.id.pick_mini_fab);
    buttons.add(mini);
    FloatingActionButton ratio = findViewById(R.id.ratio_fab);
    buttons.add(ratio);

    crop.setOnClickListener(onCropClicked);
    ratio.setOnClickListener(onRatioClicked);
    cropView.setOnTouchListener(onTouchCropView);

    findViewById(R.id.pick_fab).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Log.d("LSNJEL", "LSENFSOIUBEF");
        cropView.extensions()
            .pickUsing(MainActivity.this, RequestCodes.PICK_IMAGE_FROM_GALLERY);
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == RequestCodes.PICK_IMAGE_FROM_GALLERY
        && resultCode == Activity.RESULT_OK) {
      Uri galleryPictureUri = data.getData();

      cropView.extensions()
          .load(galleryPictureUri);

      updateButtons();
    }
  }

  public View.OnClickListener onCropClicked = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      final File croppedFile = new File(getCacheDir(), "cropped.jpg");

      Observable<Void> onSave = Observable.from(cropView.extensions()
          .crop()
          .quality(100)
          .format(JPEG)
          //.originalSize()
          //.outputSize(320, 320)
          //.sourceBitmap(bitmap)
          .offset(0, 200)
          .into(croppedFile))
          .subscribeOn(io())
          .observeOn(mainThread());

      subscriptions.add(onSave
          .subscribe(new Action1<Void>() {
            @Override
            public void call(Void nothing) {
              CropResultActivity.startUsing(croppedFile, MainActivity.this);
            }
          }));
    }
  };

  public View.OnClickListener onRatioClicked = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      final float oldRatio = cropView.getImageRatio();
      selectedRatio = (selectedRatio + 1) % ASPECT_RATIOS.length;

      // Since the animation needs to interpolate to the native
      // ratio, we need to get that instead of using 0
      float newRatio = ASPECT_RATIOS[selectedRatio];
      if (Float.compare(0, newRatio) == 0) {
        newRatio = cropView.getImageRatio();
      }

      ObjectAnimator viewportRatioAnimator = ObjectAnimator.ofFloat(cropView, "viewportRatio", oldRatio, newRatio)
          .setDuration(420);
      viewportRatioAnimator.start();

      Toast.makeText(MainActivity.this, ASPECT_LABELS[selectedRatio], Toast.LENGTH_SHORT).show();
    }
  };

  @Override
  protected void onDestroy() {
    super.onDestroy();

    subscriptions.unsubscribe();
  }

  public View.OnTouchListener onTouchCropView = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent event) {
      if (event.getPointerCount() > 1 || cropView.getImageBitmap() == null) {
        return true;
      }

      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_MOVE:
          for (View button: buttons) {
            button.setVisibility(View.INVISIBLE);
          }
          break;
        default:
          for (View button: buttons) {
            button.setVisibility(View.VISIBLE);
          }
          break;
      }
      return true;
    }
  };

  private void updateButtons() {
    for (View button: buttons) {
      button.setVisibility(View.VISIBLE);
    }
    pickButton.setVisibility(View.GONE);
  }
}

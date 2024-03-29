/*
 * Copyright (C) 2008 ZXing authors
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

package com.sophiemarceauqu.lib_qrcode.zxing.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import com.sophiemarceauqu.lib_qrcode.zxing.app.PreferencesActivity;
import com.sophiemarceauqu.lib_qrcode.zxing.decode.PlanarYUVLuminanceSource;
import com.sophiemarceauqu.lib_qrcode.zxing.util.Util;
import java.io.IOException;
import java.util.List;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();

  /**
   * 定义扫描框宽高
   */
  private static final int MIN_FRAME_WIDTH = 194;
  private static final int MIN_FRAME_HEIGHT = 194;

  private static CameraManager cameraManager;

  static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT

  static {
    int sdkInt;
    try {
      sdkInt = Build.VERSION.SDK_INT;
    } catch (NumberFormatException nfe) {
      // Just to be safe
      sdkInt = 10000;
    }
    SDK_INT = sdkInt;
  }

  private final Context context;
  private final CameraConfigurationManager configManager;
  private Camera camera;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private boolean initialized;
  private boolean previewing;
  private final boolean useOneShotPreviewCallback;
  /**
   * Preview frames are delivered here, which we pass on to the registered
   * handler. Make sure to clear the handler so it will only receive one
   * message.
   */
  private final PreviewCallback previewCallback;
  /**
   * Autofocus callbacks arrive here, and are dispatched to the Handler which
   * requested them.
   */
  private final AutoFocusCallback autoFocusCallback;

  /**
   * Initializes this static object with the Context of the calling Activity.
   *
   * @param context The Activity which wants to use the camera.
   */
  public static void init(Context context) {
    if (cameraManager == null) {
      cameraManager = new CameraManager(context);
    }
  }

  /**
   * Gets the CameraManager singleton instance.
   *
   * @return A reference to the CameraManager singleton.
   */
  public static CameraManager get() {
    return cameraManager;
  }

  private CameraManager(Context context) {
    this.context = context;
    this.configManager = new CameraConfigurationManager();

    // Camera.setOneShotPreviewCallback() has a race condition in Cupcake,
    // so we use the older
    // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later,
    // we need to use
    // the more efficient one shot callback, as the older one can swamp the
    // system and cause it
    // to run out of memory. We can't use SDK_INT because it was introduced
    // in the Donut SDK.
    // useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) >
    // Build.VERSION_CODES.CUPCAKE;
    useOneShotPreviewCallback = Build.VERSION.SDK_INT > 3; // 3 = Cupcake

    previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
    autoFocusCallback = new AutoFocusCallback();
  }

  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames
   * into.
   * @throws IOException Indicates the camera driver failed to open.
   */
  public void openDriver(SurfaceHolder holder) throws IOException {
    if (camera == null) {
      camera = Camera.open();
      if (camera == null) {
        throw new IOException();
      }
      camera.setPreviewDisplay(holder);

      if (!initialized) {
        initialized = true;
        configManager.initFromCameraParameters(camera);
      }
      configManager.setDesiredCameraParameters(camera);

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      if (prefs.getBoolean(PreferencesActivity.KEY_FRONT_LIGHT, false)) {
        FlashlightManager.enableFlashlight();
      }
    }
  }

  /**
   * Closes the camera driver if still in use.
   */
  public void closeDriver() {
    if (camera != null) {
      FlashlightManager.disableFlashlight();
      camera.release();
      camera = null;
    }
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public void startPreview() {
    if (camera != null && !previewing) {
      camera.startPreview();
      previewing = true;
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public void stopPreview() {
    if (camera != null && previewing) {
      if (!useOneShotPreviewCallback) {
        camera.setPreviewCallback(null);
      }
      camera.stopPreview();
      previewCallback.setHandler(null, 0);
      autoFocusCallback.setHandler(null, 0);
      previewing = false;
    }
  }

  /**
   * A single preview frame will be returned to the handler supplied. The data
   * will arrive as byte[] in the message.obj field, with width and height
   * encoded as message.arg1 and message.arg2, respectively.
   *
   * @param handler The handler to send the message to.
   * @param message The what field of the message to be sent.
   */
  public void requestPreviewFrame(Handler handler, int message) {
    if (camera != null && previewing) {
      previewCallback.setHandler(handler, message);
      if (useOneShotPreviewCallback) {
        camera.setOneShotPreviewCallback(previewCallback);
      } else {
        camera.setPreviewCallback(previewCallback);
      }
    }
  }

  /**
   * Asks the camera hardware to perform an autofocus.
   *
   * @param handler The Handler to notify when the autofocus completes.
   * @param message The message to deliver.
   */
  public void requestAutoFocus(Handler handler, int message) {
    if (camera != null && previewing) {
      autoFocusCallback.setHandler(handler, message);
      // Log.d(TAG, "Requesting auto-focus callback");
      camera.autoFocus(autoFocusCallback);
    }
  }

  /**
   * Calculates the framing rect which the UI should draw to show the user
   * where to place the barcode. This target helps with alignment as well as
   * forces the user to hold the device far enough away to ensure the image
   * will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public Rect getFramingRect() {
    Point screenResolution = configManager.getScreenResolution();
    if (framingRect == null) {
      if (camera == null) {
        return null;
      }
      int width = Util.dip2px(context, MIN_FRAME_WIDTH);
      int height = Util.dip2px(context, MIN_FRAME_HEIGHT);

      int leftOffset = (screenResolution.x - width) / 2;
      int topOffset = Util.dip2px(context, 150);
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }
    return framingRect;
  }

  /**
   * Like {@link #getFramingRect} but coordinates are in terms of the preview
   * frame, not UI / screen.
   */
  public Rect getFramingRectInPreview() {
    if (framingRectInPreview == null) {
      Rect rect = new Rect(getFramingRect());
      Point cameraResolution = configManager.getCameraResolution();
      Point screenResolution = configManager.getScreenResolution();
      rect.left = rect.left * cameraResolution.y / screenResolution.x;
      rect.right = rect.right * cameraResolution.y / screenResolution.x;
      rect.top = rect.top * cameraResolution.x / screenResolution.y;
      rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;

      framingRectInPreview = rect;
    }
    return framingRectInPreview;
  }

  /**
   * Converts the result points from still resolution coordinates to screen
   * coordinates.
   *
   * @param points
   *            The points returned by the Reader subclass through
   *            Result.getResultPoints().
   * @return An array of Points scaled to the size of the framing rect and
   *         offset appropriately so they can be drawn in screen coordinates.
   */

  /**
   * A factory method to build the appropriate LuminanceSource object based on
   * the format of the preview buffers, as described by Camera.Parameters.
   *
   * @param data A preview frame.
   * @param width The width of the image.
   * @param height The height of the image.
   * @return A PlanarYUVLuminanceSource instance.
   */
  @SuppressWarnings("deprecation") public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
      int width, int height) {
    Rect rect = getFramingRectInPreview();
    int previewFormat = configManager.getPreviewFormat();
    String previewFormatString = configManager.getPreviewFormatString();
    switch (previewFormat) {
      // This is the standard Android format which all devices are REQUIRED to
      // support.
      // In theory, it's the only one we should ever care about.
      case PixelFormat.YCbCr_420_SP:
        // This format has never been seen in the wild, but is compatible as
        // we only care
        // about the Y channel, so allow it.
      case PixelFormat.YCbCr_422_SP:
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(),
            rect.height());
      default:
        // The Samsung Moment incorrectly uses this variant instead of the
        // 'sp' version.
        // Fortunately, it too has all the Y data up front, so we can read
        // it.
        if ("yuv420p".equals(previewFormatString)) {
          return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
              rect.width(), rect.height());
        }
    }
    throw new IllegalArgumentException(
        "Unsupported picture format: " + previewFormat + '/' + previewFormatString);
  }

  /**
   * 通过设置Camera打开闪光灯
   */
  public void turnLightOn() {
    if (camera == null) {
      return;
    }
    Parameters parameters = camera.getParameters();
    if (parameters == null) {
      return;
    }

    List<String> flashModes = parameters.getSupportedFlashModes();
    if (flashModes == null) {
      return;
    }
    String flashMode = parameters.getFlashMode();
    Log.i(TAG, "Flash mode: " + flashMode);
    Log.i(TAG, "Flash modes: " + flashModes);
    // 闪光灯关闭状态
    if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
      // Turn on the flash
      if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
        parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
        camera.setParameters(parameters);
        camera.startPreview();
      } else {
      }
    }
  }

  /**
   * 通过设置Camera关闭闪光灯
   */
  public void turnLightOff() {
    if (camera == null) {
      return;
    }
    Parameters parameters = camera.getParameters();
    if (parameters == null) {
      return;
    }
    List<String> flashModes = parameters.getSupportedFlashModes();
    String flashMode = parameters.getFlashMode();
    // Check if camera flash exists
    if (flashModes == null) {
      return;
    }
    // 闪光灯打开状态
    if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
      // Turn off the flash
      if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
        parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        camera.setParameters(parameters);
      } else {
        Log.e(TAG, "FLASH_MODE_OFF not supported");
      }
    }
  }
}

### Importing OpenCV Module into Android Studio Project

1. Download OpenCV Android and extract it from [here](https://opencv.org/releases/).
2. In Android Studio, go to "File" -> "New" -> "Import Module".
3. Navigate to the OpenCV-Mobile SDK folder and select the "sdk" folder. Click "Finish" to import the OpenCV module into your Android project.
or simply drop the sdk folder in the directory of the repo
### Modifying CameraBridgeViewBase.Java

In the `CameraBridgeViewBase.java` file, find the `deliverAndDrawFrame` method and replace it with the following code:
```java

protected void deliverAndDrawFrame(CvCameraViewFrame frame) {
        Mat modified;

        if (mListener != null) {
            modified = mListener.onCameraFrame(frame);
        } else {
            modified = frame.rgba();
        }

        boolean bmpValid = true;
        if (modified != null) {
            try {
                Utils.matToBitmap(modified, mCacheBitmap);
            } catch(Exception e) {
                Log.e(TAG, "Mat type: " + modified);
                Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                bmpValid = false;
            }
        }

        if (bmpValid && mCacheBitmap != null) {
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "mStretch value: " + mScale);

                float halfCanvasWidth = canvas.getWidth() * 0.5f;
                float halfCanvasHeight = canvas.getHeight() * 0.5f;
                float deltaX = halfCanvasWidth - mCacheBitmap.getWidth() * 0.5f;
                float deltaY = halfCanvasHeight - mCacheBitmap.getHeight() * 0.5f;
                float scale = canvas.getWidth() / (float) mCacheBitmap.getHeight();


                Matrix matrix = new Matrix();
                matrix.preTranslate(deltaX, deltaY);

                matrix.postRotate(90f, halfCanvasWidth, halfCanvasHeight);

                matrix.postScale(scale, scale, halfCanvasWidth, halfCanvasHeight);

                canvas.drawBitmap(mCacheBitmap, matrix, null);

                if (mFpsMeter != null) {
                    mFpsMeter.measure();
                    mFpsMeter.draw(canvas, 20, 30);
                }
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
	```

package com.project.dehazing;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DarkChannelPrior {

    public static Mat enhance(Mat image, double krnlRatio, double minAtmosLight, double eps) {
        image.convertTo(image, CvType.CV_32F);
        // extract each color channel
        List<Mat> rgb = new ArrayList<>();
        Core.split(image, rgb);
        Mat rChannel = rgb.get(0);
        Mat gChannel = rgb.get(1);
        Mat bChannel = rgb.get(2);

        // Calculate the dark channel
        Mat minRGB = new Mat();
        Core.min(rChannel, gChannel, minRGB);
        Core.min(minRGB, bChannel, minRGB);

        // Create a kernel for erosion
        int kernelSize = (int) (Math.min(rChannel.rows(), rChannel.cols()) * krnlRatio);
        kernelSize = kernelSize % 2 == 1 ? kernelSize : kernelSize + 1; // Ensure kernel size is odd
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));

        // Erode the dark channel using the kernel
        Imgproc.erode(minRGB, minRGB, kernel, new Point(-1, -1), 1);

        // get coarse transmission map
        Mat t = minRGB.clone();
        Core.subtract(t, new Scalar(255.0), t);
        Core.multiply(t, new Scalar(-1.0), t);
        Core.divide(t, new Scalar(255.0), t);


        // obtain gray scale image
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
        Core.divide(gray, new Scalar(255.0), gray);

        // get refined transmission map using gaussian filter
        //https://docs.opencv.org/2.4/modules/imgproc/doc/filtering.html#gaussianblur
        Mat t_ = new Mat();
        org.opencv.core.Size s = new Size(15,15);
        Imgproc.GaussianBlur(gray, t_, s, 15);

        // get minimum atmospheric light
        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(minRGB);
        minAtmosLight = Math.min(minAtmosLight, minMaxLocResult.maxVal);

        // dehaze each color channel
        List<Mat> channels = Arrays.asList(rChannel, gChannel, bChannel);
        double finalMinAtmosLight = minAtmosLight;
        channels.parallelStream().forEach(channel -> dehazeChannel(channel, t, finalMinAtmosLight));

        // merge three color channels to a image
        Mat outval = new Mat();
        Core.merge(new ArrayList<>(Arrays.asList(rChannel, gChannel, bChannel)), outval);
        outval.convertTo(outval, CvType.CV_8UC1);
        return outval;
    }

    private static void dehazeChannel(Mat channel, Mat t, double minAtmosLight) {
          Mat t_ = new Mat();
          Core.subtract(t, new Scalar(1.0), t_);
          Core.multiply(t_, new Scalar(-1.0 * minAtmosLight), t_);
          Core.subtract(channel, t_, channel);
          Core.divide(channel, t, channel);
    }
}
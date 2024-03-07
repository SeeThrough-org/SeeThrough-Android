package com.project.dehazing;

import android.util.Log;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DarkChannelPrior {

    public static Mat enhance(Mat image, double kernelRatio, double minAtmosLight) {
        if (image == null) {
            throw new IllegalArgumentException("Input image cannot be null");
        }
        if (kernelRatio < 0 || kernelRatio > 1) {
            throw new IllegalArgumentException("Kernel ratio must be between 0 and 1");
        }
        if (minAtmosLight < 0 || minAtmosLight > 255) {
            throw new IllegalArgumentException("Minimum atmospheric light must be between 0 and 255");
        }
        image.convertTo(image, CvType.CV_32F);
        LinkedList<Mat> rgb = new LinkedList<>();
        Core.split(image, rgb);
        Mat minRGB = DarkChannel(image, kernelRatio);
        Mat t = createTransmissionMap(image, minRGB);
        transmissionMapRefine(t);
        minAtmosLight = getMinAtmosphericLight(minRGB, minAtmosLight);
        applyDehazeToChannels(rgb, t, minAtmosLight);
        Mat outval = RecoverImage(rgb);
        releaseMats(minRGB, t, rgb.get(0), rgb.get(1), rgb.get(2));

        return outval;
    }

    private static synchronized void dehazeChannel(Mat channel, Mat t, double minAtmosLight) {
        Mat t_ = new Mat();
        Core.subtract(t, new Scalar(1.0), t_);
        Core.multiply(t_, new Scalar(-1.0 * minAtmosLight), t_);
        Core.subtract(channel, t_, channel);
        Core.divide(channel, t, channel);
        t_.release();
    }

    private static void releaseMats(Mat... mats) {
        for (Mat mat : mats) {
            mat.release();
        }
    }

    private static Mat DarkChannel(Mat image, double kernelRatio) {
        LinkedList<Mat> rgb = new LinkedList<>();
        Core.split(image, rgb);
        Mat rChannel = rgb.get(0);
        Mat gChannel = rgb.get(1);
        Mat bChannel = rgb.get(2);

        Mat dc = new Mat();
        Core.min(rChannel, gChannel, dc);
        Core.min(dc, bChannel, dc);

        int rows = rChannel.rows();
        int cols = rChannel.cols();
        int kernelSize = Double.valueOf(Math.max(Math.max(rows * kernelRatio, cols * kernelRatio), 11)).intValue();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize), new Point(-1, -1));
        Imgproc.erode(dc, dc, kernel);
        kernel.release();

        return dc;
    }

    private static double getMinAtmosphericLight(Mat minRGB, double minAtmosLight) {
        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(minRGB);
        return Math.min(minAtmosLight, minMaxLocResult.maxVal);
    }

    private static Mat createTransmissionMap(Mat image, Mat minRGB) {
        Mat t = new Mat();
        Core.subtract(minRGB, new Scalar(255.0), t);
        Core.multiply(t, new Scalar(-1.0), t);
        Core.divide(t, new Scalar(255.0), t);

        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);
        Core.divide(gray, new Scalar(255.0), gray);

        return t; // or return both t and gray if needed
    }

    private static Mat transmissionMapRefine(Mat t) {
        int blurKernelSize = 15;
        int sigma = 5 * blurKernelSize;
        Imgproc.GaussianBlur(t, t, new Size(blurKernelSize, blurKernelSize), sigma);
        return t;
    }

    private static void applyDehazeToChannels(List<Mat> rgb, Mat t, double minAtmosLight) {
        List<Mat> channels = Arrays.asList(rgb.get(0), rgb.get(1), rgb.get(2));
        double finalMinAtmosLight = minAtmosLight;
        channels.parallelStream().forEach(channel -> dehazeChannel(channel, t, finalMinAtmosLight));
    }

    private static Mat RecoverImage(List<Mat> channels) {
        Mat outval = new Mat();
        Core.merge(channels, outval);
        outval.convertTo(outval, CvType.CV_8UC1);
        return outval;
    }
}

package com.seethrough.dehazing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DarkChannelPrior {
    private static final int DARK_CHANNEL_SIZE = 12;
    private static final int TRANSMISSION_MAP_SIZE = 3;
    private static final double OMEGA = 0.90;
    private static final double MIN_TRANSMISSION = 0.1;
    private static final double MAX_TRANSMISSION = 1.0;
    private static final double ATMOSPHERIC_LIGHT_PERCENTILE = 0.0001;
    public static Mat enhance(Mat image) {
        if (image == null) {
            throw new IllegalArgumentException("Input image cannot be null");
        }
        image.convertTo(image, CvType.CV_32F);
        Mat DarkChannel = darkChannel(image, DARK_CHANNEL_SIZE);
        double atmosphericLight = estimateAtmosphericLight(DarkChannel);
        Mat transmissionMap = estimateTransmissionMap(image, atmosphericLight);
        Mat refinedTransmissionMap = transmissionMapRefine(transmissionMap);
        Mat enhancedImage = recoverImage(image, refinedTransmissionMap, atmosphericLight);

        // Convert back to norm image
        enhancedImage.convertTo(enhancedImage, CvType.CV_8UC3);

        // Clean up
        DarkChannel.release();
        transmissionMap.release();

        return enhancedImage;
    }

    private static Mat darkChannel(Mat image, int size) {
        List<Mat> channels = new ArrayList<>();
        Core.split(image, channels);

        Mat darkChannel = new Mat();
        Core.min(channels.get(0), channels.get(1), darkChannel);
        Core.min(darkChannel, channels.get(2), darkChannel);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(size, size));
        Imgproc.erode(darkChannel, darkChannel, kernel);

        kernel.release();
        channels.forEach(Mat::release);

        return darkChannel;
    }

    private static double estimateAtmosphericLight(Mat darkChannel) {
        Mat sortedMinRGB = new Mat();
        Core.sort(darkChannel.reshape(1, 1), sortedMinRGB, Core.SORT_EVERY_ROW + Core.SORT_ASCENDING);
        int topN = Math.max((int)(sortedMinRGB.total() * ATMOSPHERIC_LIGHT_PERCENTILE), 1); // Top 0.1% brightest pixels, at least 1
        double sum = 0.0;
        for (int i = sortedMinRGB.cols() - topN; i < sortedMinRGB.cols(); i++) {
            sum += sortedMinRGB.get(0, i)[0];
        }
        double avgAtmosLight = sum / topN;
        sortedMinRGB.release();
        return avgAtmosLight;
    }

    private static Mat estimateTransmissionMap(Mat image, double A) {

        Mat im3 = new Mat();
        Core.divide(image, new Scalar(A, A, A, 0), im3); // Normalize each channel by A
        Mat darkChannel = darkChannel(im3, TRANSMISSION_MAP_SIZE);
        Mat transmission = new Mat();
        Core.multiply(darkChannel, new Scalar(-OMEGA), transmission);
        Core.add(transmission, new Scalar(MAX_TRANSMISSION), transmission);

        // Ensure transmission values are within a reasonable range
        Core.max(transmission, new Scalar(MIN_TRANSMISSION), transmission); // Avoid too low values that could cause black pixels
        Core.min(transmission, new Scalar(MAX_TRANSMISSION), transmission); // Ensure no value exceeds 1.0

        //clean up
        darkChannel.release();
        im3.release();

        return transmission;
    }
    private static Mat transmissionMapRefine(Mat transmission) {
        Mat refinedTransmission = new Mat();
        Imgproc.GaussianBlur(transmission, refinedTransmission, new Size(TRANSMISSION_MAP_SIZE * 2 + 1, TRANSMISSION_MAP_SIZE * 2 + 1), 2);
        transmission.release();
        return refinedTransmission;
    }

    private static Mat recoverImage(Mat image, Mat t, double AtmosphericLight) {
        double tx = 0.1;
        Mat maxT = new Mat();
        Core.max(t, new Scalar(tx), maxT);

        List<Mat> channels = new ArrayList<>();
        Core.split(image, channels);

        List<Mat> recoveredChannels = new ArrayList<>();

        for (Mat channel : channels) {
            Mat recoveredChannel = new Mat();
            Core.subtract(channel, new Scalar(AtmosphericLight), recoveredChannel);
            Core.divide(recoveredChannel, maxT, recoveredChannel);
            Core.add(recoveredChannel, new Scalar(AtmosphericLight), recoveredChannel);
            recoveredChannels.add(recoveredChannel);
        }

        Mat result = new Mat();
        Core.merge(recoveredChannels, result);

        // Clean up
        maxT.release();
        for (Mat channel : channels) {
            channel.release();
        }
        for (Mat channel : recoveredChannels) {
            channel.release();
        }

        return result;
    }
}
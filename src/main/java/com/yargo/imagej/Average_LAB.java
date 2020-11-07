package com.yargo.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.*;

public class Average_LAB implements PlugInFilter {
    /* Calculates the average LAB for a given ROI */
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB;
    }

    public void run(ImageProcessor ip) {
        Roi image_roi = imp.getRoi();
        Point[] points = image_roi.getContainedPoints();
        int v;
        double[] tempRGB = new double[3];
        double[] tempLAB;
        double[] sumLAB = {0, 0, 0};
        for (Point point : points) {
            v = ip.get(point.x, point.y);
            ColorTools.extractRGB(v, tempRGB);
            tempLAB = ColorTools.convertRGBtoLAB(tempRGB);
            sumLAB[0] += tempLAB[0];
            sumLAB[1] += tempLAB[1];
            sumLAB[2] += tempLAB[2];
        }
        double n = points.length;
        sumLAB[0] /= n;
        sumLAB[1] /= n;
        sumLAB[2] /= n;
        IJ.log("Média LAB\n" + sumLAB[0] + "\t" + sumLAB[1] + "\t" + sumLAB[2]);
    }
}

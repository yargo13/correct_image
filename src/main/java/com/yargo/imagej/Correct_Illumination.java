/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.yargo.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class Correct_Illumination implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB;
    }

    public void run(ImageProcessor ip) {
        FloatProcessor LFloat = ip.toFloat(0, null);
        FloatProcessor aFloat = ip.toFloat(1, null);
        FloatProcessor bFloat = ip.toFloat(2, null);
        ImageProcessor ipFinal = ip.duplicate();

        int w = ip.getWidth();
        int h = ip.getHeight();
        int value;
        double[] RGBTemp = new double[3];
        double[] LABTemp = new double[3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                value = ip.get(x, y);
                ColorTools.extractRGB(value, RGBTemp);
                LABTemp = ColorTools.convertRGBtoLAB(RGBTemp);
                LFloat.setf(x, y, (float) LABTemp[0]);
                aFloat.setf(x, y, (float) LABTemp[1]);
                bFloat.setf(x, y, (float) LABTemp[2]);
            }
        }
        FloatProcessor LBlurred = (FloatProcessor) LFloat.duplicate();
        LBlurred.blurGaussian(Math.max(w / 2, h / 2));

        ImagePlus impBlurred = new ImagePlus("blurred", LBlurred);
        impBlurred.show();

        double sumBefore = 0;
        double sumAfter = 0;
        float v;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                v = LFloat.getf(x, y);
                sumBefore += v;
                v /= LBlurred.getf(x, y);
                LFloat.setf(x, y, v);
                sumAfter += v;
            }
        }
        sumBefore /= (h * w);
        sumAfter /= (h * w);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                LABTemp[0] = LFloat.getf(x, y) * sumBefore / sumAfter;
                LABTemp[1] = aFloat.getf(x, y);
                LABTemp[2] = bFloat.getf(x, y);
                ipFinal.putPixel(x, y, ColorTools.convertLABtoRGB(LABTemp));
            }
        }

        ImagePlus impFinal = new ImagePlus(imp.getShortTitle() + "_Iluminacao", ipFinal);
        impFinal.show();
    }

}

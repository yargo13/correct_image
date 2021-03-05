/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.mycompany.imagej;
/* o seu caralho de merda funfa naversao nova AGORA 2*/
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * A template for processing each pixel of either
 * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
 *
 * @author Johannes Schindelin
 */
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

public class Correct_Ilumination implements PlugInFilter {
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
				Correct_Image.extractRGB(value, RGBTemp);
				LABTemp = Correct_Image.convertRGBtoLAB(RGBTemp);
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
				ipFinal.putPixel(x, y, Correct_Image.convertLABtoRGB(LABTemp));
			}
		}

		ImagePlus impFinal = new ImagePlus(imp.getShortTitle() + "_Iluminacao", ipFinal);
		impFinal.show();
        /*
    	FloatProcessor redFloat = ip.toFloat(0, null);
    	FloatProcessor greenFloat = ip.toFloat(1, null);
    	FloatProcessor blueFloat = ip.toFloat(2, null);

    	FloatProcessor redBlurred = (FloatProcessor) redFloat.duplicate();
    	FloatProcessor greenBlurred = (FloatProcessor) greenFloat.duplicate();
    	FloatProcessor blueBlurred = (FloatProcessor) blueFloat.duplicate();

    	int w = ip.getWidth();
        int h = ip.getHeight();
    	redBlurred.blurGaussian(Math.max(w/2,h/2));
    	greenBlurred.blurGaussian(Math.max(w/2,h/2));
    	blueBlurred.blurGaussian(Math.max(w/2,h/2));

        for (int y=0; y<h; y++){
            for (int x=0; x<w; x++){
                redFloat.setf(x, y, redFloat.getf(x,y)/redBlurred.getf(x, y));
                greenFloat.setf(x, y, greenFloat.getf(x,y)/greenBlurred.getf(x, y));
                blueFloat.setf(x, y, blueFloat.getf(x,y)/blueBlurred.getf(x, y));
            }
        }

        ImagePlus impRed = new ImagePlus("Teste (red)", redFloat);
        ImagePlus impGreen = new ImagePlus("Teste (green)",greenFloat);
        ImagePlus impBlue = new ImagePlus("Teste (blue)",blueFloat);

        impRed.setDisplayRange(redFloat.getMin(), redFloat.getMin());
        impGreen.setDisplayRange(greenFloat.getMin(), greenFloat.getMin());
        impBlue.setDisplayRange(blueFloat.getMin(), blueFloat.getMin());

        ImagePlus[] impChannels = {impRed, impGreen, impBlue};
        ImagePlus impCorrected = RGBStackMerge.mergeChannels(impChannels, false).flatten();
        impCorrected.setTitle(imp.getShortTitle()+"_Iluminacao");
        impCorrected.show();
		*/
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Correct_Ilumination.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}

package com.yargo.imagej;

import edu.emory.mathcs.restoretools.Enums;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.mrnsd.MRNSDDoubleIterativeDeconvolver2D;
import edu.emory.mathcs.restoretools.iterative.mrnsd.MRNSDFloatIterativeDeconvolver2D;
import edu.emory.mathcs.restoretools.iterative.mrnsd.MRNSDOptions;
import edu.emory.mathcs.utils.ConcurrencyUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class Deconvolution {
    public static ImagePlus deconvolveMRNSD(ImageProcessor blurredImage, ImagePlus PSF, String preconditionerStr, String preconditionerTolStr, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String stoppingTolStr, String thresholdStr,
                                            String logConvergenceStr, String maxItersStr, String nOfThreadsStr, String showIterationsStr) {
        double stoppingTol;
        double preconditionerTol;
        boolean showIterations, logConvergence;
        double threshold;
        int maxIters;
        int nOfThreads;
        IterativeEnums.PreconditionerType preconditioner = null;
        IterativeEnums.BoundaryType boundary = null;
        IterativeEnums.ResizingType resizing = null;
        Enums.OutputType output = null;
        Enums.PrecisionType precision = null;
        ImagePlus imX = null;
        ImagePlus imB =  new ImagePlus("Blurred Model",blurredImage);
        ImagePlus[][] imPSF = new ImagePlus[1][1];
        imPSF[0][0] = PSF;
        int h = imB.getHeight();
        int w = imB.getWidth();
        ImageProcessor ipPSF = imPSF[0][0].getProcessor();
        ipPSF = ipPSF.resize(w, h, false);
        imPSF[0][0] = new ImagePlus("Blurred Model", ipPSF);
        maxIters = Integer.parseInt(maxItersStr);
        for (IterativeEnums.PreconditionerType elem : IterativeEnums.PreconditionerType.values()) {
            if (elem.toString().equals(preconditionerStr)) {
                preconditioner = elem;
                break;
            }
        }
        for (IterativeEnums.BoundaryType elem : IterativeEnums.BoundaryType.values()) {
            if (elem.toString().equals(boundaryStr)) {
                boundary = elem;
                break;
            }
        }
        for (IterativeEnums.ResizingType elem : IterativeEnums.ResizingType.values()) {
            if (elem.toString().equals(resizingStr)) {
                resizing = elem;
                break;
            }
        }
        for (Enums.OutputType elem : Enums.OutputType.values()) {
            if (elem.toString().equals(outputStr)) {
                output = elem;
                break;
            }
        }
        for (Enums.PrecisionType elem : Enums.PrecisionType.values()) {
            if (elem.toString().equals(precisionStr)) {
                precision = elem;
                break;
            }
        }
        preconditionerTol = Double.parseDouble(preconditionerTolStr);
        stoppingTol = Double.parseDouble(stoppingTolStr);
        threshold = Double.parseDouble(thresholdStr);
        logConvergence = Boolean.parseBoolean(logConvergenceStr);
        nOfThreads = Integer.parseInt(nOfThreadsStr);
        showIterations = Boolean.parseBoolean(showIterationsStr);
        ConcurrencyUtils.setNumberOfThreads(nOfThreads);
        MRNSDOptions options = new MRNSDOptions((stoppingTol == -1) ? true : false, stoppingTol, (threshold == -1) ? false : true, threshold, logConvergence);
        switch (precision) {
            case DOUBLE:
                MRNSDDoubleIterativeDeconvolver2D dmrnsd = new MRNSDDoubleIterativeDeconvolver2D(imB, imPSF, preconditioner, preconditionerTol, boundary, resizing, output, maxIters, showIterations, options);
                imX = dmrnsd.deconvolve();
                break;
            case SINGLE:
                MRNSDFloatIterativeDeconvolver2D fmrnsd = new MRNSDFloatIterativeDeconvolver2D(imB, imPSF, preconditioner, (float) preconditionerTol, boundary, resizing, output, maxIters, showIterations, options);
                imX = fmrnsd.deconvolve();
                break;
        }

        //IJ.save(imX, pathToDeblurredImage);
        //imX.show();
        return imX;
    }

    public static ImagePlus deconvolveColorMRNSD(ImageProcessor ipB, ImagePlus PSF, String preconditionerStr, String preconditionerTolStr, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String stoppingTolStr, String thresholdStr,
                                            String logConvergenceStr, String maxItersStr, String nOfThreadsStr, String showIterationsStr) {
        double stoppingTol;
        double preconditionerTol;
        boolean showIterations, logConvergence;
        double threshold;
        int maxIters;
        int nOfThreads;
        IterativeEnums.PreconditionerType preconditioner = null;
        IterativeEnums.BoundaryType boundary = null;
        IterativeEnums.ResizingType resizing = null;
        Enums.OutputType output = null;
        ColorProcessor ipColor = ipB.convertToColorProcessor();
        ImagePlus[][] imPSF = new ImagePlus[1][1];
        imPSF[0][0] = PSF;
        maxIters = Integer.parseInt(maxItersStr);
        for (IterativeEnums.PreconditionerType elem : IterativeEnums.PreconditionerType.values()) {
            if (elem.toString().equals(preconditionerStr)) {
                preconditioner = elem;
                break;
            }
        }
        for (IterativeEnums.BoundaryType elem : IterativeEnums.BoundaryType.values()) {
            if (elem.toString().equals(boundaryStr)) {
                boundary = elem;
                break;
            }
        }
        for (IterativeEnums.ResizingType elem : IterativeEnums.ResizingType.values()) {
            if (elem.toString().equals(resizingStr)) {
                resizing = elem;
                break;
            }
        }
        for (Enums.OutputType elem : Enums.OutputType.values()) {
            if (elem.toString().equals(outputStr)) {
                output = elem;
                break;
            }
        }
        preconditionerTol = Double.parseDouble(preconditionerTolStr);
        stoppingTol = Double.parseDouble(stoppingTolStr);
        threshold = Double.parseDouble(thresholdStr);
        logConvergence = Boolean.parseBoolean(logConvergenceStr);
        nOfThreads = Integer.parseInt(nOfThreadsStr);
        showIterations = Boolean.parseBoolean(showIterationsStr);
        ConcurrencyUtils.setNumberOfThreads(nOfThreads);
        MRNSDOptions options = new MRNSDOptions((stoppingTol == -1) ? true : false, stoppingTol, (threshold == -1) ? false : true, threshold, logConvergence);
        ByteProcessor[] channelsByte = {new ByteProcessor(ipColor.getWidth(), ipColor.getHeight()), new ByteProcessor(ipColor.getWidth(), ipColor.getHeight()), new ByteProcessor(ipColor.getWidth(), ipColor.getHeight())};
        ImagePlus[] impChannels = new ImagePlus[3];
        ImagePlus[] impDeblurred = new ImagePlus[3];
        for(int i=0; i<3;i++){
            ipColor.getChannel(i+1, channelsByte[i]);
            impChannels[i] = new ImagePlus(" "+i,channelsByte[i]);
            MRNSDDoubleIterativeDeconvolver2D dmrnsd = new MRNSDDoubleIterativeDeconvolver2D(impChannels[i], imPSF, preconditioner, preconditionerTol, boundary, resizing, output, maxIters, showIterations, options);
            impDeblurred[i] = dmrnsd.deconvolve();
        }
        //IJ.save(imX, pathToDeblurredImage);
        ColorProcessor ipColorDeblurred = (ColorProcessor)ipColor.duplicate();
        for(int i=0;i<3;i++){
            ipColorDeblurred.setChannel(i+1, impDeblurred[i].getProcessor().convertToByteProcessor());
        }
        ImagePlus impColorDeblurred = new ImagePlus("Image Deblurred", ipColorDeblurred);
        //impColorDeblurred.show();
        //IJ.save(imX, pathToDeblurredImage);
        return impColorDeblurred;
    }
}

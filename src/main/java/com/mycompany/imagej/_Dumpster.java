package com.mycompany.imagej;

import Jama.Matrix;
import edu.emory.mathcs.restoretools.Enums.OutputType;
import edu.emory.mathcs.restoretools.Enums.PrecisionType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.ResizingType;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLDoubleIterativeDeconvolver2D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver2D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import edu.emory.mathcs.utils.ConcurrencyUtils;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

public class _Dumpster implements PlugInFilter {
    ImagePlus imp;
    public static long startTime;
    public static int numberOfPatches = 16;

    public static void tic() {
        startTime = System.currentTimeMillis();
    }

    public static void toc() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        IJ.log(String.format("Elapsed time: %d.%03dsec",
                elapsedTime / 1000, elapsedTime % 1000));
    }

    public static ImageProcessor localThreshold(ImageProcessor ip){
        int h = ip.getHeight();
        int w = ip.getWidth();
        int sum;
        int kernel = 6;
        float average;
        for(int i=0;i<w-kernel;i+=kernel){
            for(int j=0;j<h-kernel;j+=kernel){
                sum = 0;
                for(int r=0;r<kernel;r++){
                    for(int c=0;c<kernel;c++){
                        sum += ip.get(i+r, j+c);
                    }
                }
                average = sum/(kernel*kernel);
                if(average>50){
                    for(int r=0;r<kernel;r++){
                        for(int c=0;c<kernel;c++){
                            ip.putPixel(i+r, j+c, 255);
                        }
                    }
                }
                else{
                    for(int r=0;r<kernel;r++){
                        for(int c=0;c<kernel;c++){
                            ip.putPixel(i+r, j+c, 0);
                        }
                    }
                }
            }
        }
        return ip;
    }

    public static ImageProcessor separateCard(PolygonRoi polygon, ImageProcessor ip){
        ImageProcessor improc = ip.duplicate();
        improc.fillOutside(polygon);
        improc.setRoi(polygon);
        improc = improc.crop();
        return improc;
    }

    public static double[][] calculateCorrectionThin(double[][] averageValues, double[][] tableValues){
        double[][] rXp = new double[16][16];
        double[][] rYp = new double[16][16];
        double[][] rZp = new double[16][16];
        double[][] wL = new double[20][20];
        double[][] wY = new double[20][3];
        double wR;
        for(int i=0;i<16;i++){
            for(int j=0;j<16;j++){
                rXp[j][i] = averageValues[j][0];
                rYp[j][i] = averageValues[j][1];
                rZp[j][i] = averageValues[j][2];
            }
        }
        //Criacao da matrix wL e wY
        for(int i=0;i<16;i++){
            for(int j=0;j<16;j++){
                wR = (rXp[i][j]-rXp[j][i])*(rXp[i][j]-rXp[j][i])+(rYp[i][j]-rYp[j][i])*(rYp[i][j]-rYp[j][i])+(rZp[i][j]-rZp[j][i])*(rZp[i][j]-rZp[j][i]);
                wL[i][j] = 2*wR*Math.log(Math.sqrt(wR)+1E-20);
            }
        }
        for(int i=0;i<16;i++){
            for(int j=17;j<20;j++){
                wL[i][j] = averageValues[i][j-17];
                wL[j][i] = averageValues[i][j-17];
            }
            wL[16][i] = 1;
            wL[i][16] = 1;
            wY[i][0] = tableValues[i][0];
            wY[i][1] = tableValues[i][1];
            wY[i][2] = tableValues[i][2];
        }
        for(int i=16;i<20;i++){
            for(int j=16;j<20;j++){
                wL[i][j] = 0;
            }
            wY[i][0] = 0;
            wY[i][1] = 0;
            wY[i][2] = 0;
        }
        Matrix mwY = new Matrix(wY);
        Matrix mwL = new Matrix(wL);
        Matrix mWw = mwL.solve(mwY);
        double[][] correction = mWw.getArray();
        return correction;
    }

    public static void calculatePixelAfterCorrectionThin(double[][] correction, double[][] averageValues, double[] tempRGB, int[] finalRGB){
        double[] rX = new double[16];
        double[] rY = new double[16];
        double[] rZ = new double[16];
        for (int i=0;i<16;i++) {
            rX[i] = tempRGB[0];
            rY[i] = tempRGB[1];
            rZ[i] = tempRGB[2];
        }

        double[] rXp = new double[16];
        double[] rYp = new double[16];
        double[] rZp = new double[16];
        for (int i=0;i<16;i++) {
            rXp[i] = averageValues[i][0];
            rYp[i] = averageValues[i][1];
            rZp[i] = averageValues[i][2];
        }

        double[] wL = new double[20];
        wL[16] = 1;
        wL[17] = tempRGB[0];
        wL[18] = tempRGB[1];
        wL[19] = tempRGB[2];

        double wR, wK;
        for (int i=0;i<16;i++) {
            wR = Math.sqrt(
                    (rXp[i]-rX[i])*(rXp[i]-rX[i])+
                            (rYp[i]-rY[i])*(rYp[i]-rY[i])+
                            (rZp[i]-rZ[i])*(rZp[i]-rZ[i])
            );
            wK = 2*wR*wR*Math.log(wR+1e-20);
            wL[i] = wK;
        }
        double somaR = 0;
        double somaG = 0;
        double somaB = 0;
        for (int i=0;i<20;i++) {
            somaR += wL[i]*correction[i][0];
            somaG += wL[i]*correction[i][1];
            somaB += wL[i]*correction[i][2];
        }
        if (somaR > 255) somaR = 255;
        if (somaG > 255) somaG = 255;
        if (somaB > 255) somaB = 255;
        if (somaR < 0) somaR = 0;
        if (somaG < 0) somaG = 0;
        if (somaB < 0) somaB = 0;
        finalRGB[0] = (int)(somaR);
        finalRGB[1] = (int)(somaG);
        finalRGB[2] = (int)(somaB);
    }

    public static ImageProcessor applyCorrectionThin(double[][] correction, double[][] averageValues, ImageProcessor ip){
        ImageProcessor thin = ip.duplicate();
        int h = ip.getHeight();
        int w = ip.getWidth();
        int v;
        double[] tempRGB = new double[3];
        int[] finalRGB = new int[3];
        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                v = ip.get(i, j);
                Correct_Image.extractRGB(v, tempRGB);
                calculatePixelAfterCorrectionThin(correction,averageValues,tempRGB,finalRGB);
                thin.putPixel(i, j, finalRGB);
            }
        }
        return thin;
    }

    public static ImagePlus deconvolveWPL(ImageProcessor ipB, ImagePlus imPSF, String pathToDeblurredImage, String boundaryStr, String resizingStr, String outputStr, String precisionStr, String thresholdStr, String maxItersStr, String nOfThreadsStr, String showIterationsStr, String gammaStr,
                                          String filterXYStr, String normalizeStr, String logMeanStr, String antiRingStr, String changeThreshPercentStr, String dbStr, String detectDivergenceStr) {
        boolean showIterations, normalize, logMean, antiRing, db, detectDivergence;
        double threshold, gamma, filterXY, changeThreshPercent;
        int maxIters;
        int nOfThreads;
        BoundaryType boundary = null;
        ResizingType resizing = null;
        OutputType output = null;
        PrecisionType precision = null;
        ColorProcessor ipColor = ipB.convertToColorProcessor();
        maxIters = Integer.parseInt(maxItersStr);
        for (BoundaryType elem : BoundaryType.values()) {
            if (elem.toString().equals(boundaryStr)) {
                boundary = elem;
                break;
            }
        }
        for (ResizingType elem : ResizingType.values()) {
            if (elem.toString().equals(resizingStr)) {
                resizing = elem;
                break;
            }
        }
        for (OutputType elem : OutputType.values()) {
            if (elem.toString().equals(outputStr)) {
                output = elem;
                break;
            }
        }
        for (PrecisionType elem : PrecisionType.values()) {
            if (elem.toString().equals(precisionStr)) {
                precision = elem;
                break;
            }
        }
        threshold = Double.parseDouble(thresholdStr);
        nOfThreads = Integer.parseInt(nOfThreadsStr);
        showIterations = Boolean.parseBoolean(showIterationsStr);
        gamma = Double.parseDouble(gammaStr);
        filterXY = Double.parseDouble(filterXYStr);
        normalize = Boolean.parseBoolean(normalizeStr);
        logMean = Boolean.parseBoolean(logMeanStr);
        antiRing = Boolean.parseBoolean(antiRingStr);
        db = Boolean.parseBoolean(dbStr);
        detectDivergence = Boolean.parseBoolean(detectDivergenceStr);
        changeThreshPercent = Double.parseDouble(changeThreshPercentStr);
        ConcurrencyUtils.setNumberOfThreads(nOfThreads);
        WPLOptions options = new WPLOptions(gamma, filterXY, 0, normalize, logMean, antiRing, changeThreshPercent, db, detectDivergence, (threshold == -1) ? false : true, threshold);
        ByteProcessor[] channelsByte = {new ByteProcessor(ipColor.getWidth(), ipColor.getHeight()), new ByteProcessor(ipColor.getWidth(), ipColor.getHeight()), new ByteProcessor(ipColor.getWidth(), ipColor.getHeight())};
        ImagePlus[] impChannels = new ImagePlus[3];
        ImagePlus[] impDeblurred = new ImagePlus[3];
        for(int i=0; i<3;i++){
            ipColor.getChannel(i+1, channelsByte[i]);
            impChannels[i] = new ImagePlus(" "+i,channelsByte[i]);
            switch (precision) {
                case DOUBLE:
                    WPLDoubleIterativeDeconvolver2D dwpl = new WPLDoubleIterativeDeconvolver2D(impChannels[i], imPSF, boundary, resizing, output, maxIters, showIterations, options);
                    impDeblurred[i] = dwpl.deconvolve();
                    break;
                case SINGLE:
                    WPLFloatIterativeDeconvolver2D fwpl = new WPLFloatIterativeDeconvolver2D(impChannels[i], imPSF, boundary, resizing, output, maxIters, showIterations, options);
                    impDeblurred[i] = fwpl.deconvolve();
                    break;
            }
        }
        ColorProcessor ipColorDeblurred = (ColorProcessor)ipColor.duplicate();
        for(int i=0;i<3;i++){
            ipColorDeblurred.setChannel(i+1, impDeblurred[i].getProcessor().convertToByteProcessor());
        }
        ImagePlus impColorDeblurred = new ImagePlus("Image Deblurred", ipColorDeblurred);
        impColorDeblurred.show();
        //IJ.save(imX, pathToDeblurredImage);
        return impColorDeblurred;
    }

    public static void generateIndicesDID(double[][] indices, int index, double value){
        double[] tempIndices = new double[2];
        tempIndices[0] = 1;
        tempIndices[1] = value;
        //tempIndices[2] = value*value;
        //tempIndices[3] = value*value*value;
        indices[index] = tempIndices;
    }

    public static double[][] calculateCorrectionDID(double[][] averageValues, double[][] tableValues){
        double[][] correction = new double[3][numberOfPatches];
        double[][] indicesR = new double[numberOfPatches][2];
        double[][] indicesG = new double[numberOfPatches][2];
        double[][] indicesB = new double[numberOfPatches][2];
        double[][] tableValuesR = new double[numberOfPatches][1];
        double[][] tableValuesG = new double[numberOfPatches][1];
        double[][] tableValuesB = new double[numberOfPatches][1];
        for (int j=0; j<(averageValues.length);j++){
            tableValuesR[j][0] = tableValues[j][0]/255;
            tableValuesG[j][0] = tableValues[j][1]/255;
            tableValuesB[j][0] = tableValues[j][2]/255;
            generateIndicesDID(indicesR, j, averageValues[j][0]/255);
            generateIndicesDID(indicesG, j, averageValues[j][1]/255);
            generateIndicesDID(indicesB, j, averageValues[j][2]/255);
        }
        Matrix indicesRMatrix = new Matrix(indicesR);
        Matrix tableRMatrix = new Matrix(tableValuesR);
        Matrix correctR = indicesRMatrix.solve(tableRMatrix);
        Matrix indicesGMatrix = new Matrix(indicesG);
        Matrix tableGMatrix = new Matrix(tableValuesG);
        Matrix correctG = indicesGMatrix.solve(tableGMatrix);
        Matrix indicesBMatrix = new Matrix(indicesB);
        Matrix tableBMatrix = new Matrix(tableValuesB);
        Matrix correctB = indicesBMatrix.solve(tableBMatrix);
        correction[0] = correctR.getColumnPackedCopy();
        correction[1] = correctG.getColumnPackedCopy();
        correction[2] = correctB.getColumnPackedCopy();


        Matrix correctM = new Matrix(correction);
        correctM.print(2, 4);

        return correction;
    }

    public static ImageProcessor applyCorrectionDID(double[][] correction, ImageProcessor ip){
        ImageProcessor did = ip.duplicate();
        int h = ip.getHeight();
        int w = ip.getWidth();
        int v;
        double[] tempRGB = new double[3];
        //double[] tempLAB = new double[3];
        //double[] finalLAB = new double[3];
        //double[] finalRGBConvert = new double[3];
        int[] finalRGB = new int[3];
        double[][] indices = new double[3][4];
        int multiplier;
        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                v = ip.get(i, j);
                Correct_Image.extractRGB(v, tempRGB);
                //tempLAB = convertRGBtoLAB(tempRGB);
                for(int k=0;k<3;k++){
                    multiplier = 255;
                    generateIndicesDID(indices, k, (tempRGB[k])/multiplier);
                    //finalLAB[k] = multiplier*(indices[k][0]*correction[k][0] + indices[k][1]*correction[k][1] + indices[k][2]*correction[k][2] + indices[k][3]*correction[k][3]);
                    finalRGB[k] = (int)(multiplier*(indices[k][0]*correction[k][0] + indices[k][1]*correction[k][1]));
                }
                //finalRGBConvert = convertLABtoRGB(finalLAB);
                for(int k=0;k<3;k++){
                    if(finalRGB[k]>255) finalRGB[k] = 255;
                    if(finalRGB[k]<0) finalRGB[k] = 0;
                }
                did.putPixel(i, j, finalRGB);
            }
        }
        return did;
    }

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB;
    }

    public void run(ImageProcessor ip) {
        tic();
        toc();
    }
}
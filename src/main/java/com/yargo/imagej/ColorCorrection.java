package com.yargo.imagej;

import Jama.Matrix;
import ij.IJ;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;

import java.awt.*;

public class ColorCorrection {

    public static ImageProcessor calculateAndApplyCorrection(ImageProcessor input, double[][] averageValues, double[][] tableValues, String algorithm) {
        if (algorithm.equals("Plane")) {
            double[][] correctionPlane = ColorCorrection.calculateCorrectionPlane(averageValues, tableValues);
            return applyCorrectionPlane(correctionPlane, input);
        } else {
            double[][] correctionTPS = ColorCorrection.calculateCorrectionTPS(averageValues, tableValues);
            return applyCorrectionTPS(correctionTPS, averageValues, input);
        }
    }

    public static double[][] calculateCorrectionPlane(double[][] averageValues, double[][] tableValues) {
        double[][] M = new double[4][Correct_Image.NUMBER_OF_PATCHES];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < (averageValues.length); j++) {
                if (i != 3) M[i][j] = averageValues[j][i];
                else M[i][j] = 1;
            }
        }
        Matrix TMatrix = new Matrix(tableValues);
        TMatrix = TMatrix.transpose();
        Matrix MMatrix = new Matrix(M);
        Matrix correctionMatrix = MMatrix.solveTranspose(TMatrix);

        System.out.print("Correction plane");
        TMatrix.print(2, 4);
        MMatrix.print(2, 4);
        correctionMatrix.print(2, 4);
        return correctionMatrix.getArray();
    }

    public static ImageProcessor applyCorrectionPlane(double[][] correction, ImageProcessor ip) {
        ImageProcessor plane = ip.duplicate();
        int h = ip.getHeight();
        int w = ip.getWidth();
        int v;
        double[] tempRGB = new double[3];
        int[] finalRGB = new int[3];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                v = ip.get(i, j);
                ColorTools.extractRGB(v, tempRGB);

                for (int k = 0; k < 3; k++) {
                    finalRGB[k] = (int) (correction[0][k] * tempRGB[0] + correction[1][k] * tempRGB[1] + correction[2][k] * tempRGB[2] + correction[3][k]);
                }
                for (int k = 0; k < 3; k++) {
                    if (finalRGB[k] > 255) finalRGB[k] = 255;
                    if (finalRGB[k] < 0) finalRGB[k] = 0;
                }
                plane.putPixel(i, j, finalRGB);
            }
        }
        return plane;
    }

    public static double[][] calculateCorrectionTPS(double[][] averageValues, double[][] tableValues) {
        double[][] rXp = new double[16][16];
        double[][] rYp = new double[16][16];
        double[][] rZp = new double[16][16];
        double[][] wL = new double[20][20];
        double[][] wY = new double[20][3];
        double wR;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                rXp[j][i] = averageValues[j][0];
                rYp[j][i] = averageValues[j][1];
                rZp[j][i] = averageValues[j][2];
            }
        }
        //Criacao da matrix wL e wY
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                wR = (rXp[i][j] - rXp[j][i]) * (rXp[i][j] - rXp[j][i]) + (rYp[i][j] - rYp[j][i]) * (rYp[i][j] - rYp[j][i]) + (rZp[i][j] - rZp[j][i]) * (rZp[i][j] - rZp[j][i]);
                wL[i][j] = 2 * wR * Math.log(Math.sqrt(wR) + 1E-20);
            }
        }
        for (int i = 0; i < 16; i++) {
            for (int j = 17; j < 20; j++) {
                wL[i][j] = averageValues[i][j - 17];
                wL[j][i] = averageValues[i][j - 17];
            }
            wL[16][i] = 1;
            wL[i][16] = 1;
            wY[i][0] = tableValues[i][0];
            wY[i][1] = tableValues[i][1];
            wY[i][2] = tableValues[i][2];
        }
        for (int i = 16; i < 20; i++) {
            for (int j = 16; j < 20; j++) {
                wL[i][j] = 0;
            }
            wY[i][0] = 0;
            wY[i][1] = 0;
            wY[i][2] = 0;
        }
        Matrix mwY = new Matrix(wY);
        Matrix mwL = new Matrix(wL);
        Matrix mWw = mwL.solve(mwY);
        return mWw.getArray();
    }

    public static ImageProcessor applyCorrectionTPS(double[][] correction, double[][] averageValues, ImageProcessor ip){
        ImageProcessor ipThin = ip.duplicate();
        int h = ip.getHeight();
        int w = ip.getWidth();
        int v;
        double[] tempRGB = new double[3];
        int[] finalRGB = new int[3];
        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                v = ip.get(i, j);
                ColorTools.extractRGB(v, tempRGB);
                calculatePixelAfterCorrectionTPS(correction,averageValues,tempRGB,finalRGB);
                ipThin.putPixel(i, j, finalRGB);
            }
        }
        return ipThin;
    }

    public static void calculatePixelAfterCorrectionTPS(double[][] correction, double[][] averageValues, double[] tempRGB, int[] finalRGB){
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

    public static PointRoi findCenterPointsColors(Rectangle cardOuterRectangle) {
        double x, y;
        int[] xp = new int[Correct_Image.NUMBER_OF_PATCHES + 1];
        int[] yp = new int[Correct_Image.NUMBER_OF_PATCHES + 1];
        int aux = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                x = cardOuterRectangle.getCenterX()
                        - (7.0 / 2 * Correct_Image.SIDE_PATCH + 7.0 / 2 * Correct_Image.INTER_DISTANCE) / Correct_Image.width * Correct_Image.majorSide
                        + j * (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) / Correct_Image.width * Correct_Image.majorSide;
                if (Correct_Image.isSuperior)
                    y = cardOuterRectangle.getCenterY()
                            - (3.0 / 2 * Correct_Image.SIDE_PATCH + 3.0 / 2 * Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide
                            + i * (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide;
                else
                    y = cardOuterRectangle.getCenterY()
                            + (1.0 / 2 * Correct_Image.SIDE_PATCH + 1.0 / 2 * Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide
                            + i * (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide;
                xp[aux] = (int) x;
                yp[aux] = (int) y;
                aux++;
            }
        }
        xp[Correct_Image.NUMBER_OF_PATCHES] = (int) cardOuterRectangle.getCenterX();
        yp[Correct_Image.NUMBER_OF_PATCHES] = (int) cardOuterRectangle.getCenterY();
        return new PointRoi(xp, yp, Correct_Image.NUMBER_OF_PATCHES + 1);
    }

    public static double[][] averageRGB(ImageProcessor input, Rectangle cardOuterRectangle, boolean isSuperior) {
        double[] rgbSum = new double[3];
        double[][] averageValues = new double[Correct_Image.NUMBER_OF_PATCHES][3];
        double[] rgbTemp = new double[3];
        int cont;
        int value;
        double x, y;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                x = cardOuterRectangle.getCenterX()
                        - (7.0 / 2 * Correct_Image.SIDE_PATCH + 7.0 / 2 * Correct_Image.INTER_DISTANCE) / Correct_Image.width * Correct_Image.majorSide
                        + j * (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) / Correct_Image.width * Correct_Image.majorSide;
                if (isSuperior)
                    y = cardOuterRectangle.getCenterY()
                            - (3.0 / 2 * Correct_Image.SIDE_PATCH + 3.0 / 2 * Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide
                            + i * (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide;
                else
                    y = cardOuterRectangle.getCenterY()
                            + (1.0 / 2 * Correct_Image.SIDE_PATCH + 1.0 / 2 * Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide
                            + i * (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) / Correct_Image.height * Correct_Image.minorSide;
                cont = 0;
                rgbSum[0] = 0;
                rgbSum[1] = 0;
                rgbSum[2] = 0;
                for (int row = (int) (x - (Correct_Image.SIDE_PATCH / 4 * Correct_Image.majorSide / Correct_Image.width)); row < (int) (x + (Correct_Image.SIDE_PATCH / 4 * Correct_Image.majorSide / Correct_Image.width)); row++) {
                    for (int col = (int) (y - (Correct_Image.SIDE_PATCH / 4 * Correct_Image.minorSide / Correct_Image.height)); col < (int) (y + (Correct_Image.SIDE_PATCH / 4 * Correct_Image.minorSide / Correct_Image.height)); col++) {
                        value = input.get(row, col);
                        ColorTools.extractRGB(value, rgbTemp);
                        rgbSum[0] += rgbTemp[0];
                        rgbSum[1] += rgbTemp[1];
                        rgbSum[2] += rgbTemp[2];
                        cont += 1;
                    }
                }
                rgbSum[0] /= cont;
                rgbSum[1] /= cont;
                rgbSum[2] /= cont;
                averageValues[8 * i + j][0] = rgbSum[0];
                averageValues[8 * i + j][1] = rgbSum[1];
                averageValues[8 * i + j][2] = rgbSum[2];
            }
        }

        //Verifica cor do patch na primeira linha e primeira coluna e na ultima
        //linha e ultima coluna, vendo qual é maior
        if (!isSuperior) {
            double[][] rearrangedValues = new double[Correct_Image.NUMBER_OF_PATCHES][3];
            for (int i = 0; i < Correct_Image.NUMBER_OF_PATCHES; i++) {
                rearrangedValues[i] = averageValues[Correct_Image.NUMBER_OF_PATCHES - i - 1];
            }
            averageValues = rearrangedValues.clone();
        }

        Matrix correctM = new Matrix(averageValues);
        correctM.print(2, 4);

        return averageValues;
    }

    public static double meanColorDifference(double[][] averageValues, double[][] tableValues) {
        double sum = 0, partialSum;
        for (int i = 0; i < averageValues.length; i++) {
            partialSum = 0;
            for (int j = 0; j < 3; j++) {
                partialSum += (averageValues[i][j] - tableValues[i][j]) * (averageValues[i][j] - tableValues[i][j]);
            }
            sum += Math.sqrt(partialSum);
        }
        return sum / averageValues.length;
    }


    public static void printStdErrorMinMax(double[][] averageValues, double[][] tableValues) {
        double sum = 0, partialSum, stdError, minDif = 999, patchMin = 0, maxDif = -1, patchMax = 0;
        double meanDifference = meanColorDifference(averageValues, tableValues);
        IJ.log("Values Color");
        for (int i = 0; i < averageValues.length; i++) {
            partialSum = 0;
            for (int j = 0; j < 3; j++) {
                partialSum += (averageValues[i][j] - tableValues[i][j]) * (averageValues[i][j] - tableValues[i][j]);
            }
            sum += (Math.sqrt(partialSum) - meanDifference) * (Math.sqrt(partialSum) - meanDifference);
            //IJ.log(""+Math.sqrt(partialSum));
            if (Math.abs(Math.sqrt(partialSum)) > maxDif) {
                maxDif = Math.sqrt(partialSum);
                patchMax = i;
            }
            if (Math.abs(Math.sqrt(partialSum)) < minDif) {
                minDif = Math.sqrt(partialSum);
                patchMin = i;
            }
        }
        stdError = Math.sqrt(sum / averageValues.length);
        IJ.log("Desvio Padrao: " + stdError);
        IJ.log("Diferenca Minima: " + minDif + " no patch: " + patchMin);
        IJ.log("Diferenca Maxima: " + maxDif + " no patch: " + patchMax + "\n");
    }
}

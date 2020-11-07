package com.yargo.imagej;

import Jama.Matrix;
import ij.process.ImageProcessor;

public class ColorCorrection {

    public static double[][] calculateCorrectionPlane(double[][] averageValues, double[][] tableValues) {
        double[][] M = new double[4][Correct_Image.numberOfPatches];
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

    public static double[][] calculateCorrectionThin(double[][] averageValues, double[][] tableValues) {
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
                ColorTools.extractRGB(v, tempRGB);
                calculatePixelAfterCorrectionThin(correction,averageValues,tempRGB,finalRGB);
                thin.putPixel(i, j, finalRGB);
            }
        }
        return thin;
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
}

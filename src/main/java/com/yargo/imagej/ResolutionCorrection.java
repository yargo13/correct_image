package com.yargo.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Wand;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.InputStream;

public class ResolutionCorrection {

    public static ImagePlus calibrate(ImagePlus impSource, ImageProcessor model) {
        double[] resultsBefore = ResolutionCorrection.analyzeModel(model, false);

        ImagePlus imPSF;
        InputStream is = ResolutionCorrection.class.getResourceAsStream("/Modelo.tif");
        Opener opener = new Opener();

        ImagePlus modelTIF = opener.openTiff(is, "Modelo");

        double[] resultsModel = {1, 1, 0};
        double[] resultsTemp;
        int iter = 0, sequence = 0, iterationsFinal = 0;
        if (resultsBefore[2] > 0.62) {
            resultsModel = resultsBefore.clone();
            iter = 0;
        }
        while (iter < 100) {
            ImagePlus imX1 = Deconvolution.deconvolveMRNSD(model, modelTIF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                    "false", Integer.toString(iter), "4", "false");
            imPSF = ResolutionCorrection.extractPSF(imX1);
            ImagePlus impModelDeconv = Deconvolution.deconvolveMRNSD(model, imPSF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                    "false", "1", "4", "false");
            ImageProcessor ipModel = impModelDeconv.getChannelProcessor();
            ipModel = ipModel.convertToByte(false);
            resultsTemp = ResolutionCorrection.analyzeModel(ipModel, false);

            if (resultsTemp[0] * resultsTemp[1] < resultsModel[0] * resultsModel[1]) {
                resultsModel[0] = resultsTemp[0];
                resultsModel[1] = resultsTemp[1];
                resultsModel[2] = resultsTemp[2];
                sequence = 0;
                iterationsFinal = iter;
            } else sequence += 1;
            if (sequence >= 15) {
                IJ.log("Numero de Iteracoes " + iterationsFinal);
                break;
            }
            iter++;
        }
        if (iterationsFinal > 0) {
            ImagePlus imX1 = Deconvolution.deconvolveMRNSD(model, modelTIF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                    "false", Integer.toString(iterationsFinal), "4", "false");
            imPSF = ResolutionCorrection.extractPSF(imX1);
            return Deconvolution.deconvolveColorMRNSD(impSource.getProcessor(), imPSF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                    "false", "1", "4", "false");
        } return impSource;
    }

    //Analisa modelo aproximadamente quadrado e devolve no vetor results o valor de MSE, Contagem de Threshold e SSIM
    //Caso print seja selecionado, informa detalhes das medições no log
    public static double[] analyzeModel(ImageProcessor model, boolean print) {
        int h = model.getHeight();
        int w = model.getWidth();
        double[] results = new double[3];
        if (h < 3.0 / 4 * w || w < 3.0 / 4 * h) {
            IJ.log("Modelo Invalido");
            results[0] = 1;
            results[1] = 1;
            results[2] = 0;
        }
        double[] modelArray = new double[(w - 4) * (h - 4)];
        double[] baseArray = new double[(w - 4) * (h - 4)];

        InputStream is = Correct_Illumination.class.getResourceAsStream("/Modelo.tif");
        Opener opener = new Opener();
        ImagePlus imBase = opener.openTiff(is, "Modelo");
        ImageProcessor ipBase = imBase.getProcessor();
        ipBase = ipBase.resize(w, h, false);
        double min = 255, max = 0,
                difQuad = 0, avgModel = 0, avgBase = 0,
                countThreshold = 0, sumSquares = 0;

        //Threshold de 10% para classificacao correta da cor
        double threshold = 0.1;

        for (int j = 0; j < h - 4; j++) {
            for (int i = 0; i < w - 4; i++) {
                modelArray[(w - 4) * j + i] = (double) (model.get(i + 2, j + 2)) / 255;
                baseArray[(w - 4) * j + i] = (double) (ipBase.get(i + 2, j + 2)) / 255;
            }
        }

        w = w - 4;
        h = h - 4; //troca valores para facilitar calculos

        for (double v : modelArray) {
            if (v > max) max = v;
            if (v < min) min = v;
        }
        for (int i = 0; i < modelArray.length; i++) {
            modelArray[i] = (modelArray[i] - min) / (max - min);
            avgModel += modelArray[i];
            avgBase += baseArray[i];
            difQuad += (baseArray[i] - modelArray[i]) * (baseArray[i] - modelArray[i]);
            sumSquares += (baseArray[i] * baseArray[i]);
            if (Math.abs(baseArray[i] - modelArray[i]) > threshold) countThreshold += 1;
        }

        countThreshold = countThreshold / modelArray.length;
        avgModel = avgModel / modelArray.length;
        avgBase = avgBase / modelArray.length;

        double covar = 0, varModel = 0, varBase = 0, c = 0.0001;
        for (int i = 0; i < modelArray.length; i++) {
            varModel += (modelArray[i] - avgModel) * (modelArray[i] - avgModel);
            varBase += (baseArray[i] - avgBase) * (baseArray[i] - avgBase);
            covar += (modelArray[i] - avgModel) * (baseArray[i] - avgBase);
        }
        varModel = varModel / modelArray.length;
        varBase = varBase / modelArray.length;
        covar = covar / modelArray.length;

        double numeratorCoAvg, numeratorCoVar, denominatorAvg, denominatorVar;
        numeratorCoAvg = (2 * avgModel * avgBase + c);
        numeratorCoVar = (2 * covar + c);
        denominatorAvg = (avgModel * avgModel + avgBase * avgBase + c);
        denominatorVar = (varModel + varBase + c);
        double ssim = numeratorCoAvg / denominatorAvg * numeratorCoVar / denominatorVar;
        double difQuadMed = difQuad / modelArray.length;
        double NRMSE = Math.sqrt(difQuad / sumSquares);


        double minQ = 1, maxQ = 0, ssimSmall, sumSsimSmall = 0;
        int smallHeight = h / 10;
        int smallWidth = w / 10;
        int numberPixelsSmall = smallHeight * smallWidth;
        for (int lin = 0; lin < 10; lin++) {
            for (int col = 0; col < 10; col++) {
                double sumSmallModel = 0, sumSmallBase = 0, avgSmallModel, avgSmallBase, covarSmall = 0, varSmallModel = 0, varSmallBase = 0;
                for (int i = 0; i < smallWidth; i++) {
                    for (int j = 0; j < smallHeight; j++) {
                        int elementIndex = w * (smallHeight * lin + j) + (i + smallWidth * col);
                        sumSmallModel += modelArray[elementIndex]; //indice do elemento da submatriz (lin, col)
                        sumSmallBase += baseArray[elementIndex];
                    }
                }
                avgSmallModel = sumSmallModel / numberPixelsSmall;
                avgSmallBase = sumSmallBase / numberPixelsSmall;
                for (int i = 0; i < smallWidth; i++) {
                    for (int j = 0; j < smallHeight; j++) {
                        int elementIndex = w * (smallHeight * lin + j) + (i + smallWidth * col);
                        covarSmall += (modelArray[elementIndex] - avgSmallModel) * (baseArray[elementIndex] - avgSmallBase);
                        varSmallModel += (modelArray[elementIndex] - avgSmallModel) * (modelArray[elementIndex] - avgSmallModel);
                        varSmallBase += (baseArray[elementIndex] - avgSmallBase) * (baseArray[elementIndex] - avgSmallBase);
                    }
                }
                covarSmall = covarSmall / numberPixelsSmall;
                varSmallModel = varSmallModel / numberPixelsSmall;
                varSmallBase = varSmallBase / numberPixelsSmall;
                ssimSmall = (2 * avgSmallModel * avgSmallBase + c) * (2 * covarSmall + c) / ((avgSmallModel * avgSmallModel + avgSmallBase * avgSmallBase + c) * (varSmallModel + varSmallBase + c));
                sumSsimSmall += ssimSmall;
                if (ssimSmall > maxQ) maxQ = ssimSmall;
                if (ssimSmall < minQ) minQ = ssimSmall;
            }
        }

        if (print) {
            IJ.log("MSE: " + difQuadMed);
            IJ.log("NRMSE: " + NRMSE);
            IJ.log("Q: " + ssim);
            //IJ.log("N1: "+numeratorCoAvg+" D1: "+denominatorAvg+" N2: "+numeratorCoVar+" D2: "+denominatorVar);
            IJ.log("Mean Q: " + sumSsimSmall / 100);
            IJ.log("Count Threshold: " + countThreshold + " Multiply: " + difQuadMed * countThreshold);
        }
        results[0] = difQuadMed;
        results[1] = countThreshold;
        results[2] = sumSsimSmall / 100;
        return results;
    }

    public static ImageProcessor findValidModel(ImageProcessor ipSource, PointRoi cardExtremePoints) throws RuntimeException {
        ImageProcessor model = ResolutionCorrection.findModel(ipSource, cardExtremePoints, 1);
        double[] resultsBefore = ResolutionCorrection.analyzeModel(model, false);
        if (model.getHeight() < model.getWidth() * 3.0 / 4 || model.getWidth() < model.getHeight() * 3.0 / 4 || resultsBefore[0] > 0.2) {
            model = ResolutionCorrection.findModel(ipSource, cardExtremePoints, 2);
        }

        if (model.getHeight() < model.getWidth() * 3.0 / 4 || model.getWidth() < model.getHeight() * 3.0 / 4 || resultsBefore[0] > 0.2) {
            IJ.log("Nao foi possivel detectar o modelo de resolucao");
            throw new RuntimeException();
        }
        return model;
    }

    public static ImageProcessor findModel(ImageProcessor ipSource, PointRoi cardExtremePoints, int position) {
        Rectangle r = cardExtremePoints.getBounds();
        double xCenterSquare, yCenterSquare;
        int x = 0, y = 0, width = 0, height = 0;
        switch (position) {
            case 1:
                xCenterSquare = (r.getCenterX() - (3 * Correct_Image.SIDE_PATCH + 3 * Correct_Image.INTER_DISTANCE) * r.getWidth() / Correct_Image.width);
                if (Correct_Image.isSuperior)
                    yCenterSquare = (r.getCenterY() + (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                else
                    yCenterSquare = (r.getCenterY() - (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                x = (int) (xCenterSquare - (Correct_Image.SIDE_PATCH + 0.5 * Correct_Image.INTER_DISTANCE) * r.getWidth() / Correct_Image.width);
                y = (int) (yCenterSquare - (Correct_Image.SIDE_PATCH + 0.5 * Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                width = (int) (1 * (2 * Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getWidth() / Correct_Image.width);
                height = (int) (1 * (2 * Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                break;
            case 2:
                xCenterSquare = (r.getCenterX() + (3 * Correct_Image.SIDE_PATCH + 3 * Correct_Image.INTER_DISTANCE) * r.getWidth() / Correct_Image.width);
                if (Correct_Image.isSuperior)
                    yCenterSquare = (r.getCenterY() + (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                else
                    yCenterSquare = (r.getCenterY() - (Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                x = (int) (xCenterSquare - (Correct_Image.SIDE_PATCH + 0.5 * Correct_Image.INTER_DISTANCE) * r.getWidth() / Correct_Image.width);
                y = (int) (yCenterSquare - (Correct_Image.SIDE_PATCH + 0.5 * Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                width = (int) (1 * (2 * Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getWidth() / Correct_Image.width);
                height = (int) (1 * (2 * Correct_Image.SIDE_PATCH + Correct_Image.INTER_DISTANCE) * r.getHeight() / Correct_Image.height);
                break;
        }
        Rectangle rSquare = new Rectangle(x, y, width, height);
        ipSource.setRoi(rSquare);
        ImageProcessor ipByte = ipSource.crop();
        ipByte = ipByte.convertToByte(false);
        ImageProcessor ipThresh = ipByte.duplicate();
        int w = ipThresh.getWidth();
        int h = ipThresh.getHeight();
        int v;
        double max = -2;
        double min = 256;
        for (int i = 0; i < h * w; i++) {
            v = ipThresh.get(i);
            if (v > max) max = v;
            if (v < min) min = v;
        }
        double threshold;
        if (max > 190 && min > 90) {
            threshold = (max + min) * 0.5;
            ipThresh.threshold((int) threshold);
        } else {
            ipThresh.autoThreshold();
        }

        Wand wand = new Wand(ipThresh);
        Polygon polygon;
        r = ipThresh.getRoi();
        double area = r.getHeight() * r.getWidth();
        int xWand = (int) (r.getCenterX() + r.getWidth() * 40.5 / 100);
        int yWand = (int) (r.getCenterY());
        wand.autoOutline(xWand, yWand);
        polygon = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
        if (polygon.getBounds().getWidth() * polygon.getBounds().getHeight() > 0.85 * area) {
            xWand = (int) (r.getCenterX() + r.getWidth() * 37.0 / 100);
            yWand = (int) (r.getCenterY());
        }
        if (polygon.getBounds().getWidth() * polygon.getBounds().getHeight() < 0.70 * area) {
            xWand = (int) (r.getCenterX());
            yWand = (int) (r.getCenterY() + r.getHeight() * 40.0 / 100);
        }
        wand.autoOutline(xWand, yWand);
        polygon = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
        r = polygon.getBounds();
        r = new Rectangle((int) r.getMinX() + 1, (int) r.getMinY() + 1, r.width - 2, r.height - 2);
        ipByte.setRoi(r);
        ipByte = ipByte.crop();

        return ipByte;
    }

    public static ImagePlus extractPSF(ImagePlus impSource) {
        ImageProcessor ipPSF = impSource.getChannelProcessor();
        int x = 0, y = 0, v, max = 0;
        for (int i = 0; i < ipPSF.getWidth() / 5; i++) {
            for (int j = 0; j < ipPSF.getHeight() / 5; j++) {
                v = ipPSF.get(i, j);
                if (v > max) {
                    x = i;
                    y = j;
                    max = v;
                }
            }
        }
        Rectangle rPSF;
        int min = Math.min(x, y);
        min = Math.min(min, 7);
        rPSF = new Rectangle(x - min, y - min, 2 * min + 1, 2 * min + 1);
        ipPSF.setRoi(rPSF);
        ipPSF = ipPSF.crop();

        return new ImagePlus("PSF", ipPSF);
    }
}

package com.yargo.imagej;

import Jama.Matrix;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Undo;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.io.Opener;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;

public class Correct_Image implements PlugInFilter {
    ImagePlus imp;
    public static long startTime;
    static double widthBlackExt = 11.00;
    static double widthWhite = 10.70;
    static double widthBlackInt = 10.40;
    static double width = widthWhite;
    static double heightBlackExt = 5.50;
    static double heightWhite = 5.20;
    static double heightBlackInt = 4.90;
    static double height = heightWhite;
    static double sidePatch = 1.00;
    static double interDistance = 0.20;
    static int majorSide = 1;
    static int minorSide = 1;
    static int numberOfPatches = 16;
    static boolean superior = true;
    static boolean shouldClearModel = false;
    static HashMap<String, double[][]> tableValuesRGB = new HashMap<String, double[][]>() {
        {
            double[][] aRGB = {{137, 108, 102}, {147, 109, 98}, {166, 142, 123}, {185, 158, 156},
                    {199, 179, 163}, {198, 178, 173}, {194, 178, 169}, {203, 193, 189},
                    {156, 128, 108}, {166, 138, 116}, {184, 163, 147}, {186, 162, 146},
                    {198, 178, 172}, {203, 183, 180}, {202, 189, 189}, {205, 194, 193}};
            double[][] bRGB = {{126, 103, 94}, {138, 107, 96}, {163, 130, 109}, {181, 136, 126},
                    {179, 148, 124}, {189, 153, 137}, {184, 154, 136}, {192, 170, 152},
                    {153, 120, 102}, {161, 128, 107}, {176, 143, 122}, {180, 141, 120},
                    {190, 154, 137}, {201, 162, 147}, {197, 166, 152}, {199, 172, 155}};
            double[][] cRGB = {{103, 86, 76}, {117, 91, 79}, {163, 126, 101}, {187, 138, 128},
                    {189, 154, 126}, {198, 161, 143}, {195, 164, 142}, {204, 181, 161},
                    {141, 109, 89}, {164, 123, 97}, {181, 145, 122}, {186, 145, 121},
                    {198, 162, 144}, {205, 169, 154}, {203, 173, 159}, {207, 180, 162}};

            put("a", aRGB);
            put("b", bRGB);
            put("c", cRGB);
        }
    };
    static HashMap<String, double[][]> tableValuesLAB = new HashMap<String, double[][]>() {
        {
            double[][] aLAB = {{48.288, 10.313, 7.995}, {49.583, 13.206, 12.229}, {60.630, 6.398, 13.368}, {67.423, 9.827, 4.516},
                    {74.132, 4.490, 10.854}, {74.078, 6.324, 4.949}, {73.642, 4.473, 6.645}, {78.633, 2.963, 2.873},
                    {55.627, 8.014, 14.800}, {59.610, 7.127, 16.085}, {68.310, 5.263, 11.145}, {68.174, 6.472, 11.600},
                    {74.132, 6.152, 5.580}, {76.050, 6.494, 4.118}, {77.636, 4.454, 1.844}, {79.248, 3.730, 2.068}};
            double[][] bLAB = {{45.967, 8.100, 8.600}, {48.300, 11.300, 11.100}, {57.333, 10.700, 16.700}, {61.200, 16.567, 12.300},
                    {63.967, 9.300, 17.533}, {66.500, 11.867, 13.800}, {66.267, 9.000, 14.200}, {71.400, 6.200, 12.200},
                    {53.567, 11.100, 15.200}, {56.567, 11.067, 16.767}, {62.200, 10.500, 16.267}, {62.133, 13.067, 17.100},
                    {66.733, 12.000, 14.067}, {70.033, 13.300, 13.633}, {70.700, 10.100, 11.500}, {72.667, 7.900, 12.400}};
            double[][] cLAB = {{38.200, 6.333, 8.300}, {41.300, 9.833, 11.500}, {56.267, 12.267, 19.667}, {62.200, 18.333, 13.133},
                    {66.533, 10.300, 19.767}, {69.533, 11.900, 14.800}, {70.000, 9.367, 15.933}, {75.500, 6.100, 13.700},
                    {49.000, 11.367, 16.233}, {55.500, 13.600, 20.633}, {63.467, 11.567, 17.700}, {63.767, 13.433, 18.733},
                    {69.667, 11.900, 14.633}, {72.433, 11.867, 13.033}, {73.267, 9.400, 11.533}, {75.567, 7.767, 13.033}};
            put("a", aLAB);
            put("b", bLAB);
            put("c", cLAB);
        }
    };

    public static void tic() {
        startTime = System.currentTimeMillis();
    }

    public static void toc() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        IJ.log(String.format("Elapsed time: %d.%03dsec",
                elapsedTime / 1000, elapsedTime % 1000));
    }


    public void enlargeCanvas(ImagePlus imp, double angle) {
        imp.unlock();
        if (imp.getStackSize() == 1)
            Undo.setup(Undo.COMPOUND_FILTER, imp);
        IJ.run(imp, "Select All", "");
        IJ.run(imp, "Rotate...", "angle=" + angle);
        Roi roi = imp.getRoi();
        Rectangle r = roi.getBounds();
        if (r.width < imp.getWidth()) r.width = imp.getWidth();
        if (r.height < imp.getHeight()) r.height = imp.getHeight();
        IJ.run(imp, "Canvas Size...", "width=" + r.width + " height=" + r.height + " position=Center zero");
    }

    public static Polygon findCard(ImageProcessor ip, int irregular) {
        //Registra dimensoes do ROI
        Rectangle roi = ip.getRoi();
        int r0 = roi.y;
        int c0 = roi.x;
        int hROI = roi.height;
        int wROI = roi.width;
        ImageProcessor ipByte = ip.convertToByte(true);
        int h = ipByte.getHeight();
        int w = ipByte.getWidth();
        int v;
        ipByte.findEdges();
        ipByte.medianFilter(); //Filtro da mediana retira parte do ruido de "bordas" virtuais
        if (irregular == 3) {
            int threshold = ipByte.getAutoThreshold();
            IJ.log("" + (int) (0.65 * threshold));
            ipByte.threshold((int) (0.65 * threshold));
        } else {
            ipByte.autoThreshold();
        }
        ipByte.medianFilter();
        ImagePlus impa = new ImagePlus("Teste", ipByte); //Imagem das bordas, para verificacao de problemas
        //impa.show();
        ParticleAnalyzer pAnalyzer = new ParticleAnalyzer(36, 0, null, 50, Double.POSITIVE_INFINITY, 0, 0.6);
        pAnalyzer.setHideOutputImage(true);
        pAnalyzer.analyze(impa);
        ImagePlus impParticles = pAnalyzer.getOutputImage();
        ImageProcessor ipParticles = impParticles.getChannelProcessor();
        Wand wand = new Wand(ipParticles);
        double area = -2;

        int xi = 0;
        int yi = 0;
        Polygon polygonfinal = null;

        for (int j = r0 + 1; j < r0 + hROI; j++) {
            for (int i = c0 + 1; i < c0 + wROI; i++) {
                v = ipParticles.get(i, j);
                if (v == 0 && (polygonfinal == null || !polygonfinal.contains(i, j))) {
                    wand.autoOutline(i, j);
                    polygonfinal = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
                    Rectangle r = polygonfinal.getBounds();
                    double rarea = r.getWidth() * r.getHeight();
                    if (rarea > area) {
                        area = rarea;
                        //IJ.log(""+area+" i: "+i+" j: "+j);
                        xi = i;
                        yi = j;
                    }

                }
            }
        }
        wand.autoOutline(xi, yi);

        int[] xretang = {w, 0, 0, 0};
        int[] yretang = {0, h, 0, 0};

        for (int i = 0; i < wand.npoints; i++) {
            //0: Ponto mais a esquerda
            if (wand.xpoints[i] < xretang[0]) {
                xretang[0] = wand.xpoints[i];
                yretang[0] = wand.ypoints[i];
            }
            //1: Ponto mais em cima
            if (wand.ypoints[i] < yretang[1]) {
                xretang[1] = wand.xpoints[i];
                yretang[1] = wand.ypoints[i];
            }
            //2: Ponto mais a direita
            if (wand.xpoints[i] > xretang[2]) {
                xretang[2] = wand.xpoints[i];
                yretang[2] = wand.ypoints[i];
            }
            //3: Ponto mais embaixo
            if (wand.ypoints[i] > yretang[3]) {
                xretang[3] = wand.xpoints[i];
                yretang[3] = wand.ypoints[i];
            }
        }

        return new Polygon(xretang, yretang, 4);
    }

    public static int findIrregularRectangle(Polygon polygon) {
        /*
         * Verifica diferencas entre pontos consecutivos do retangulo tanto em x como em y
         * Se a o modulo da variacao em uma das dimensoes for pequeno apos 3 pontos, assume-se que estao proximos
         * de uma linha reta e retorna que o retangulo é irregular
         */
        Rectangle r = polygon.getBounds();

        int[] deltax = new int[4];
        int[] deltay = new int[4];
        int[] xpoints = polygon.xpoints;
        int[] ypoints = polygon.ypoints;
        if (r.getHeight() > 0.9 * r.getWidth() && r.getHeight() < 1.1 * r.getWidth()) return 3;
        for (int i = 0; i < 4; i++) {
            deltax[i] = Math.abs(xpoints[i] - xpoints[(i + 1) % 4]);
            deltay[i] = Math.abs(ypoints[i] - ypoints[(i + 1) % 4]);
        }

        for (int i = 0; i < 4; i++) {
            if (deltax[i] < r.getWidth() * 0.15 || deltay[i] < r.getHeight() * 0.15) {
                IJ.log("Delta x: " + deltax[i] + " Deltay: " + deltay[i]);
                if (deltax[0] > deltax[3]) {
                    return 1;
                } else return 2;
            }
        }

        return 0;
    }

    public static boolean checkOrientation(ImageProcessor ip, Polygon polygon) {
        boolean turn = false;
        Rectangle r = polygon.getBounds();

        if ((r.getHeight() > r.getWidth() && ip.getHeight() > ip.getWidth()) || r.getHeight() > 1.4 * r.getWidth())
            turn = true;
        return turn;
    }

    public static void getNewPoints(ImageProcessor source, Polygon polygon, int[] xnew, int[] ynew) {
        int h = source.getHeight();
        int w = source.getWidth();

        int[] x = polygon.xpoints;
        int[] y = polygon.ypoints;
        double area = 0.5 * (x[0] * y[1] + x[1] * y[2] + x[2] * y[3] + x[3] * y[0] - y[0] * x[1] - y[1] * x[2] - y[2] * x[3] - y[3] * x[0]);

        double length;
        double minLength = 99999;
        int position = 0;

        //Ve qual é o lado menor, indicando o ponto inicial do segmento
        for (int i = 0; i < 4; i++) {
            length = Math.sqrt((x[(i + 1) % 4] - x[i]) * (x[(i + 1) % 4] - x[i]) + (y[(i + 1) % 4] - y[i]) * (y[(i + 1) % 4] - y[i]));
            if (length < minLength) {
                minLength = length;
                position = i;
            }
        }
        minorSide = (int) minLength;
        majorSide = (int) (area / minLength);
        xnew[position] = x[position] + w / 20;
        ynew[position] = y[position] + h / 20;

        if (position == 0) {
            xnew[1] = xnew[0];
            xnew[2] = xnew[0] + majorSide;
            xnew[3] = xnew[0] + majorSide;

            ynew[1] = ynew[0] - minorSide;
            ynew[2] = ynew[0] - minorSide;
            ynew[3] = ynew[0];

        } else if (position == 1) {
            xnew[0] = xnew[1] - majorSide;
            xnew[2] = xnew[1];
            xnew[3] = xnew[1] - majorSide;

            ynew[0] = ynew[1];
            ynew[2] = ynew[1] + minorSide;
            ynew[3] = ynew[1] + minorSide;
        } else if (position == 2) {
            xnew[0] = xnew[2] - majorSide;
            xnew[1] = xnew[2] - majorSide;
            xnew[3] = xnew[2];

            ynew[0] = ynew[2] + minorSide;
            ynew[1] = ynew[2];
            ynew[3] = ynew[2] + minorSide;
        } else {
            xnew[0] = xnew[3];
            xnew[1] = xnew[3] + majorSide;
            xnew[2] = xnew[3] + majorSide;

            ynew[0] = ynew[3] - minorSide;
            ynew[1] = ynew[3] - minorSide;
            ynew[2] = ynew[3];
        }

    }

    public static void calibrateImage(ImagePlus imp) {
        ij.measure.Calibration calibration = imp.getCalibration();
        calibration.pixelWidth = width / majorSide;
        calibration.pixelHeight = height / minorSide;
        calibration.setUnit("cm");
    }

    public static void checkSize(ImageProcessor ip, Rectangle r) {
        ImageProcessor ipByte = ip.convertToByte(false);
        int v = ipByte.get((int) r.getMinX() + 5, (int) r.getMinY() + 5);
        int u = ipByte.get((int) (r.getMinX() + r.getWidth() * 0.20 / (widthBlackExt) + 5), (int) (r.getMinY() + r.getHeight() * 0.20 / (widthBlackExt) + 5));
        IJ.log("U: " + u + " V: " + v);
        if ((u < 50 && u >= 2 * v) || (u >= 50 && u > 1.1 * v)) {
            width = widthBlackExt;
            height = heightBlackExt;
            IJ.log("Black Ext");
        } else if ((u < 50 && u < 2 * v && u > 0.5 * v) || (u >= 50 && u > 1.1 * v && u < 0.9 * v)) {
            width = widthBlackInt;
            height = heightBlackInt;
            IJ.log("Black Int");
        } else {
            width = widthWhite;
            height = heightWhite;
            IJ.log("White");
        }

    }

    public static void checkSuperior(ImageProcessor ip, PointRoi newpoints) {
        Rectangle r = newpoints.getBounds();
        double[] rgbSum = new double[3];
        double[] rgbTemp = new double[3];
        int cont;
        int value;
        double x, y;

        //Identificar se cores estão na metade superior ou inferior através do patch em branco.
        double[] values = {0, 0};
        for (int i = -1; i < 2; i += 2) {
            x = r.getCenterX() - (3.0 / 2 * sidePatch + 3.0 / 2 * interDistance) / width * majorSide;
            y = r.getCenterY() + i * (1.0 / 2 * sidePatch + 1.0 / 2 * interDistance) / height * minorSide;
            cont = 0;
            rgbSum[0] = 0;
            rgbSum[1] = 0;
            rgbSum[2] = 0;
            for (int row = (int) (x - (sidePatch / 4 * majorSide / width)); row < (int) (x + (sidePatch / 4 * majorSide / width)); row++) {
                for (int col = (int) (y - (sidePatch / 4 * minorSide / height)); col < (int) (y + (sidePatch / 4 * minorSide / height)); col++) {
                    value = ip.get(row, col);
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
            values[(int) ((i + 1) * 0.5)] = Math.sqrt(rgbSum[0] * rgbSum[0] + rgbSum[1] * rgbSum[1] + rgbSum[2] * rgbSum[2]);

        }
        //IJ.log("Values[0] "+values[0]+ " Values[1] "+values[1]);
        superior = !(values[0] > values[1]);
    }

    public static ImageProcessor findModel(ImageProcessor ip, PointRoi points, int position) {
        Rectangle r = points.getBounds();
        double xCenterSquare, yCenterSquare;
        int x = 0, y = 0, width = 0, height = 0;
        switch (position) {
            case 1:
                xCenterSquare = (r.getCenterX() - (3 * sidePatch + 3 * interDistance) * r.getWidth() / Correct_Image.width);
                if (superior)
                    yCenterSquare = (r.getCenterY() + (sidePatch + interDistance) * r.getHeight() / Correct_Image.height);
                else
                    yCenterSquare = (r.getCenterY() - (sidePatch + interDistance) * r.getHeight() / Correct_Image.height);
                x = (int) (xCenterSquare - (sidePatch + 0.5 * interDistance) * r.getWidth() / Correct_Image.width);
                y = (int) (yCenterSquare - (sidePatch + 0.5 * interDistance) * r.getHeight() / Correct_Image.height);
                width = (int) (1 * (2 * sidePatch + interDistance) * r.getWidth() / Correct_Image.width);
                height = (int) (1 * (2 * sidePatch + interDistance) * r.getHeight() / Correct_Image.height);
                break;
            case 2:
                xCenterSquare = (r.getCenterX() + (3 * sidePatch + 3 * interDistance) * r.getWidth() / Correct_Image.width);
                if (superior)
                    yCenterSquare = (r.getCenterY() + (sidePatch + interDistance) * r.getHeight() / Correct_Image.height);
                else
                    yCenterSquare = (r.getCenterY() - (sidePatch + interDistance) * r.getHeight() / Correct_Image.height);
                x = (int) (xCenterSquare - (sidePatch + 0.5 * interDistance) * r.getWidth() / Correct_Image.width);
                y = (int) (yCenterSquare - (sidePatch + 0.5 * interDistance) * r.getHeight() / Correct_Image.height);
                width = (int) (1 * (2 * sidePatch + interDistance) * r.getWidth() / Correct_Image.width);
                height = (int) (1 * (2 * sidePatch + interDistance) * r.getHeight() / Correct_Image.height);
                break;
        }
        Rectangle rSquare = new Rectangle(x, y, width, height);
        ip.setRoi(rSquare);
        ImageProcessor ipByte = ip.crop();
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


    public static ImagePlus extractPSF(ImagePlus imX) {
        ImageProcessor ipPSF = imX.getChannelProcessor();
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

    public static double[][] averageRGB(ImageProcessor ip, PointRoi newpoints) {
        Rectangle r = newpoints.getBounds();
        double[] rgbSum = new double[3];
        double[][] averageValues = new double[numberOfPatches][3];
        double[] rgbTemp = new double[3];
        int cont;
        int value;
        double x, y;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                x = r.getCenterX() - (7.0 / 2 * sidePatch + 7.0 / 2 * interDistance) / width * majorSide + j * (sidePatch + interDistance) / width * majorSide;
                if (superior)
                    y = r.getCenterY() - (3.0 / 2 * sidePatch + 3.0 / 2 * interDistance) / height * minorSide + i * (sidePatch + interDistance) / height * minorSide;
                else
                    y = r.getCenterY() + (1.0 / 2 * sidePatch + 1.0 / 2 * interDistance) / height * minorSide + i * (sidePatch + interDistance) / height * minorSide;
                cont = 0;
                rgbSum[0] = 0;
                rgbSum[1] = 0;
                rgbSum[2] = 0;
                for (int row = (int) (x - (sidePatch / 4 * majorSide / width)); row < (int) (x + (sidePatch / 4 * majorSide / width)); row++) {
                    for (int col = (int) (y - (sidePatch / 4 * minorSide / height)); col < (int) (y + (sidePatch / 4 * minorSide / height)); col++) {
                        value = ip.get(row, col);
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
        //Verifica cor do patch na primeira linha e primeira coluna e na ultima linha e ultima coluna, vendo qual é maior

        if (!superior) {
            double[][] rearrangedValues = new double[numberOfPatches][3];
            for (int i = 0; i < numberOfPatches; i++) {
                rearrangedValues[i] = averageValues[numberOfPatches - i - 1];
            }
            averageValues = rearrangedValues.clone();
        }

        Matrix correctM = new Matrix(averageValues);
        correctM.print(2, 4);


        return averageValues;
    }

    public double meanColorDifference(double[][] averageValues, double[][] tableValues) {
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

    public void printStdErrorMinMax(double meanDifference, double[][] averageValues, double[][] tableValues) {
        double sum = 0, partialSum, stdError, minDif = 999, patchMin = 0, maxDif = -1, patchMax = 0;
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

    //Analisa modelo aproximadamente quadrado e devolve no vetor results o valor de MSE, Contagem de Threshold e SSIM
    //Caso print seja selecionado, informa detalhes das medições no log
    public void analyzeModel(ImageProcessor model, double[] results, boolean print) {
        int h = model.getHeight();
        int w = model.getWidth();
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
    }

    public ImagePlus clearModel(ImagePlus imp, ImageProcessor ip, PointRoi points) {
        ip.setRoi(points.getBounds());
        ip.fill();
        ImageProcessor ipByte = ip.convertToByte(true);
        int h = ip.getHeight();
        int w = ip.getWidth();
        int minX = w, maxX = 0, minY = h, maxY = 0;
        int u = ipByte.get(1, 1);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (ipByte.get(i, j) != u) {
                    if (i < minX) minX = i;
                    if (i > maxX) maxX = i;
                    if (j < minY) minY = j;
                    if (j > maxY) maxY = j;
                }
            }
        }

        Rectangle r = new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
        ip.setRoi(r);
        ip = ip.crop();
        ImagePlus impClear = new ImagePlus(imp.getTitle(), ip);
        impClear.show();
        return impClear;
    }

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB;
    }


    public void run(ImageProcessor ip) {
        /*
    	int[][] RGB = ColorTools.convertLABtoRGB( tableValuesLAB.get("b"));
    	double[][] doubleRGB = new double[RGB.length][3];
    	for (int i = 0; i < RGB.length; i++){
    	    for (int j = 0; j < 3; j++){
    	        doubleRGB[i][j] = RGB[i][j];
            }
        }
    	Matrix RGBM = new Matrix(doubleRGB);
    	RGBM.print(2, 4);
        return;
        */
        IJ.log("\\Clear");
        IJ.log("" + imp.getTitle());
        tic();
        String resultText = QRDecoder.decode(ip);

        GenericDialog dialog = new GenericDialog("Selecionar Calibração");
        String[] algorithmOptions = {"Plane", "DID"};
        dialog.addChoice("TableRGB - Calcular Correção", tableValuesRGB.keySet().toArray(new String[0]), "Ideal");
        dialog.addChoice("TableLAB - Calcular Erro", tableValuesLAB.keySet().toArray(new String[0]), "Ideal");
        dialog.addChoice("Correção", algorithmOptions, "Plane");
        dialog.showDialog();
        if (dialog.wasCanceled()) return;
        double[][] tableRGB = tableValuesRGB.get(dialog.getNextChoice());
        double[][] tableLAB = tableValuesLAB.get(dialog.getNextChoice());
        String algorithmChoice = dialog.getNextChoice();

        ImageProcessor preprocessedIp = ip.duplicate();
        preprocessedIp.setBackgroundValue(0);
        ImagePlus preprocessedImp = new ImagePlus("Preprocessed", preprocessedIp);

        Polygon polygon = findCard(preprocessedIp, 0);
        int irregular = findIrregularRectangle(polygon);

        if (irregular != 0) {
            enlargeCanvas(preprocessedImp, -12.5);
            preprocessedIp = preprocessedImp.getChannelProcessor();
            preprocessedIp.setBackgroundValue(0);
            if (irregular == 1) preprocessedIp.rotate(-12.5);
            else preprocessedIp.rotate(12.5);
            polygon = findCard(preprocessedIp, 0);
        }

        irregular = findIrregularRectangle(polygon);
        if (irregular == 3) {
            polygon = findCard(preprocessedIp, irregular);
        }


        boolean turn = checkOrientation(ip, polygon);
        if (turn) {
            preprocessedIp = preprocessedIp.rotateRight();
            polygon = findCard(preprocessedIp, irregular);
            preprocessedImp = new ImagePlus(imp.getTitle(), preprocessedIp);
        }

        PointRoi pointsRoi = new PointRoi(polygon.xpoints, polygon.ypoints, polygon.npoints);
        preprocessedImp.setRoi(pointsRoi);
        preprocessedImp.show();

        int[] xnew = new int[4];
        int[] ynew = new int[4];
        getNewPoints(preprocessedIp, polygon, xnew, ynew);

        PointRoi newPoints = new PointRoi(xnew, ynew, 4);
        ImagePlus impPerspective = PerspectiveTransform.transform(preprocessedImp, newPoints);
        impPerspective.show();

        ImageProcessor ipPerspective = impPerspective.getChannelProcessor();

        Rectangle r = newPoints.getBounds();
        checkSize(ipPerspective, r);
        checkSuperior(ipPerspective, newPoints);

        double[] resultsBefore = new double[3];
        ImageProcessor ipDeconvolved;
        ImageProcessor model1 = findModel(ipPerspective, newPoints, 1);
        analyzeModel(model1, resultsBefore, false);
        if (model1.getHeight() < model1.getWidth() * 3.0 / 4 || model1.getWidth() < model1.getHeight() * 3.0 / 4 || resultsBefore[0] > 0.2) {
            model1 = findModel(ipPerspective, newPoints, 2);
            analyzeModel(model1, resultsBefore, false);
        }
        if (model1.getHeight() < model1.getWidth() * 3.0 / 4 || model1.getWidth() < model1.getHeight() * 3.0 / 4 || resultsBefore[0] > 0.2) {
            IJ.log("Nao foi possivel detectar o modelo de resolucao");
            ipDeconvolved = ipPerspective;
        } else {
            ImagePlus imPSF;
            InputStream is = getClass().getResourceAsStream("/Modelo.tif");
            Opener opener = new Opener();

            ImagePlus modelTIF = opener.openTiff(is, "Modelo");

            double[] resultsModel = {1, 1, 0};
            double[] resultsTemp = new double[3];
            int i = 0, sequence = 0, iterationsFinal = 0;
            if (resultsBefore[2] > 0.62) {
                resultsModel = resultsBefore.clone();
                i = 0;
            }
            while (i < 100) {
                ImagePlus imX1 = Deconvolution.deconvolveMRNSD(model1, modelTIF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", Integer.toString(i), "4", "false");
                imPSF = extractPSF(imX1);
                ImagePlus impModelDeconv = Deconvolution.deconvolveMRNSD(model1, imPSF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", "1", "4", "false");
                ImageProcessor ipModel = impModelDeconv.getChannelProcessor();
                ipModel = ipModel.convertToByte(false);
                analyzeModel(ipModel, resultsTemp, false);

                if (resultsTemp[0] * resultsTemp[1] < resultsModel[0] * resultsModel[1]) {
                    resultsModel[0] = resultsTemp[0];
                    resultsModel[1] = resultsTemp[1];
                    resultsModel[2] = resultsTemp[2];
                    sequence = 0;
                    iterationsFinal = i;
                } else sequence += 1;
                if (sequence >= 15) {
                    IJ.log("Numero de Iteracoes " + iterationsFinal);
                    break;
                }
                i++;
            }
            if (iterationsFinal > 0) {
                ImagePlus imX1 = Deconvolution.deconvolveMRNSD(model1, modelTIF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", Integer.toString(iterationsFinal), "4", "false");
                imPSF = extractPSF(imX1);
                ImagePlus impDeconvolved = Deconvolution.deconvolveColorMRNSD(ipPerspective, imPSF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", "1", "4", "false");
                ipDeconvolved = impDeconvolved.getProcessor();
            } else ipDeconvolved = ipPerspective;
            //imPSF.show();

        }

        double[][] averageRGBPerspective = averageRGB(ipPerspective, newPoints);
        double[][] averageLABPerspective = ColorTools.convertRGBtoLAB(averageRGBPerspective);
        double meanDifference = meanColorDifference(averageLABPerspective, tableLAB);

        double x, y;
        int[] xp = new int[25];
        int[] yp = new int[25];
        int aux = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 8; j++) {
                x = r.getCenterX() - (7.0 / 2 * sidePatch + 7.0 / 2 * interDistance) / width * majorSide + j * (sidePatch + interDistance) / width * majorSide;
                if (superior)
                    y = r.getCenterY() - (3.0 / 2 * sidePatch + 3.0 / 2 * interDistance) / height * minorSide + i * (sidePatch + interDistance) / height * minorSide;
                else
                    y = r.getCenterY() + (1.0 / 2 * sidePatch + 1.0 / 2 * interDistance) / height * minorSide + i * (sidePatch + interDistance) / height * minorSide;
                xp[aux] = (int) x;
                yp[aux] = (int) y;
                aux++;
            }
        }
        xp[24] = (int) r.getCenterX();
        yp[24] = (int) r.getCenterY();
        PointRoi pr = new PointRoi(xp, yp, 25);
        impPerspective.setRoi(pr);

        ImagePlus impCorrected;
        if (meanDifference > 55) {
            IJ.log("Tabela de cores nao localizada");
        } else {
            double[][] averageRGBDeconvolved = averageRGB(ipDeconvolved, newPoints);

            ImageProcessor ipCorrected;
            if (algorithmChoice.equals("Plane")) {
                double[][] correctionPlane = ColorCorrection.calculateCorrectionPlane(averageRGBDeconvolved, tableRGB);
                ipCorrected = ColorCorrection.applyCorrectionPlane(correctionPlane, ipDeconvolved);
            } else {
                double[][] correctionThin = ColorCorrection.calculateCorrectionThin(averageRGBDeconvolved, tableRGB);
                ipCorrected = ColorCorrection.applyCorrectionThin(correctionThin, averageRGBDeconvolved, ipDeconvolved);
            }

            impCorrected = new ImagePlus("" + imp.getShortTitle() + "_Corrigida", ipCorrected);
            impCorrected.show();

            double[][] averageRGBPlane = averageRGB(ipCorrected, newPoints);
            double[][] averageLABPlane = ColorTools.convertRGBtoLAB(averageRGBPlane);
            double meanDifferencePlane = meanColorDifference(averageLABPlane, tableLAB);

            IJ.log("Mean Difference Antes: " + meanDifference);
            IJ.log("Mean Difference Depois Plano: " + meanDifferencePlane);
            printStdErrorMinMax(meanDifferencePlane, averageLABPlane, tableLAB);

            ImageProcessor model1new = findModel(ipCorrected, newPoints, 1);
            ImageProcessor model2old = findModel(ipPerspective, newPoints, 2);
            ImageProcessor model2new = findModel(ipCorrected, newPoints, 2);

            double[] resultsAnt1 = new double[3];
            double[] resultsDep1 = new double[3];
            double[] resultsAnt2 = new double[3];
            double[] resultsDep2 = new double[3];
            analyzeModel(model1, resultsAnt1, true);
            analyzeModel(model1new, resultsDep1, true);
            analyzeModel(model2old, resultsAnt2, true);
            analyzeModel(model2new, resultsDep2, true);

            String resultText2 = QRDecoder.decode(ipCorrected);
            IJ.log(resultText2);
            calibrateImage(impCorrected);

            if (shouldClearModel) {
                impPerspective = clearModel(impPerspective, ipPerspective, newPoints);
                clearModel(impCorrected, ipCorrected, newPoints);
            }
        }

        calibrateImage(impPerspective);
        toc();
        IJ.log(resultText);

    }

    /**
     * Main method for debugging.
     * <p>
     * For debugging, it is convenient to have a method that starts ImageJ, loads
     * an image and calls the plugin, e.g. after setting breakpoints.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Correct_Image.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        // open the Clown sample
        ImagePlus image = (new Opener()).openImage("");
        image.show();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }

}
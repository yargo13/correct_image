package com.yargo.imagej;

import Jama.Matrix;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.HashMap;

public class Correct_Image implements PlugInFilter {
    ImagePlus imp;
    public static long startTime;
    final static double WIDTH_BLACK_EXT = 11.00;
    final static double WIDTH_WHITE = 10.70;
    final static double WIDTH_BLACK_INT = 10.40;
    final static double HEIGHT_BLACK_EXT = 5.50;
    final static double HEIGHT_WHITE = 5.20;
    final static double HEIGHT_BLACK_INT = 4.90;
    final static double SIDE_PATCH = 1.00;
    final static double INTER_DISTANCE = 0.20;

    static double width = WIDTH_WHITE;
    static double height = HEIGHT_WHITE;

    static int majorSide = 1;
    static int minorSide = 1;
    static int numberOfPatches = 16;
    static boolean isSuperior = true;
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

    public static void calibrateImageSizes(ImagePlus imp) {
        ij.measure.Calibration calibration = imp.getCalibration();
        calibration.pixelWidth = width / majorSide;
        calibration.pixelHeight = height / minorSide;
        calibration.setUnit("cm");
    }

    public static void checkSize(ImageProcessor ip, Rectangle r) {
        ImageProcessor ipByte = ip.convertToByte(false);
        int v = ipByte.get((int) r.getMinX() + 5, (int) r.getMinY() + 5);
        int u = ipByte.get((int) (r.getMinX() + r.getWidth() * 0.20 / (WIDTH_BLACK_EXT) + 5), (int) (r.getMinY() + r.getHeight() * 0.20 / (WIDTH_BLACK_EXT) + 5));
        IJ.log("U: " + u + " V: " + v);
        if ((u < 50 && u >= 2 * v) || (u >= 50 && u > 1.1 * v)) {
            width = WIDTH_BLACK_EXT;
            height = HEIGHT_BLACK_EXT;
            IJ.log("Black Ext");
        } else if ((u < 50 && u < 2 * v && u > 0.5 * v) || (u >= 50 && u > 1.1 * v && u < 0.9 * v)) {
            width = WIDTH_BLACK_INT;
            height = HEIGHT_BLACK_INT;
            IJ.log("Black Int");
        } else {
            width = WIDTH_WHITE;
            height = HEIGHT_WHITE;
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
            x = r.getCenterX() - (3.0 / 2 * SIDE_PATCH + 3.0 / 2 * INTER_DISTANCE) / width * majorSide;
            y = r.getCenterY() + i * (1.0 / 2 * SIDE_PATCH + 1.0 / 2 * INTER_DISTANCE) / height * minorSide;
            cont = 0;
            rgbSum[0] = 0;
            rgbSum[1] = 0;
            rgbSum[2] = 0;
            for (int row = (int) (x - (SIDE_PATCH / 4 * majorSide / width)); row < (int) (x + (SIDE_PATCH / 4 * majorSide / width)); row++) {
                for (int col = (int) (y - (SIDE_PATCH / 4 * minorSide / height)); col < (int) (y + (SIDE_PATCH / 4 * minorSide / height)); col++) {
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
        isSuperior = !(values[0] > values[1]);
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
                x = r.getCenterX() - (7.0 / 2 * SIDE_PATCH + 7.0 / 2 * INTER_DISTANCE) / width * majorSide + j * (SIDE_PATCH + INTER_DISTANCE) / width * majorSide;
                if (isSuperior)
                    y = r.getCenterY() - (3.0 / 2 * SIDE_PATCH + 3.0 / 2 * INTER_DISTANCE) / height * minorSide + i * (SIDE_PATCH + INTER_DISTANCE) / height * minorSide;
                else
                    y = r.getCenterY() + (1.0 / 2 * SIDE_PATCH + 1.0 / 2 * INTER_DISTANCE) / height * minorSide + i * (SIDE_PATCH + INTER_DISTANCE) / height * minorSide;
                cont = 0;
                rgbSum[0] = 0;
                rgbSum[1] = 0;
                rgbSum[2] = 0;
                for (int row = (int) (x - (SIDE_PATCH / 4 * majorSide / width)); row < (int) (x + (SIDE_PATCH / 4 * majorSide / width)); row++) {
                    for (int col = (int) (y - (SIDE_PATCH / 4 * minorSide / height)); col < (int) (y + (SIDE_PATCH / 4 * minorSide / height)); col++) {
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

        if (!isSuperior) {
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
        String[] algorithmOptions = {"Plane", "TPS-3D"};
        dialog.addChoice("Modelo para calibração", tableValuesRGB.keySet().toArray(new String[0]), "Ideal");
        dialog.addChoice("Algoritmo da calibração", algorithmOptions, "TPS-3D");
        dialog.showDialog();
        if (dialog.wasCanceled()) return;
        String table = dialog.getNextChoice();
        double[][] tableRGB = tableValuesRGB.get(table);
        double[][] tableLAB = tableValuesLAB.get(table);
        String algorithmChoice = dialog.getNextChoice();

        ImagePlus preprocessedImp = CardFinder.preprocessImage(imp);
        PointRoi newPoints = PerspectiveTransform.getNewPoints(preprocessedImp);
        ImagePlus impPerspective = PerspectiveTransform.transform(preprocessedImp, newPoints);
        impPerspective.show();

        ImageProcessor ipPerspective = impPerspective.getProcessor();

        Rectangle r = newPoints.getBounds();
        checkSize(ipPerspective, r);
        checkSuperior(ipPerspective, newPoints);

        ImageProcessor model1old;
        try {
            model1old = ResolutionCorrection.findValidModel(ipPerspective, newPoints);
        } catch (RuntimeException e) {
            return;
        }
        ImagePlus impDeconvolved = ResolutionCorrection.calibrate(impPerspective, model1old);
        ImageProcessor ipDeconvolved = impDeconvolved.getProcessor();

        double[][] averageRGBPerspective = averageRGB(ipPerspective, newPoints);
        double[][] averageLABPerspective = ColorTools.convertRGBtoLAB(averageRGBPerspective);
        double meanDifference = meanColorDifference(averageLABPerspective, tableLAB);

        if (meanDifference > 55) {
            IJ.log("Tabela de cores nao localizada");
            return;
        }

        PointRoi pr = ColorCorrection.findCenterColors(r);
        impPerspective.setRoi(pr);

        double[][] averageRGBDeconvolved = averageRGB(ipDeconvolved, newPoints);

        ImageProcessor ipCorrected = ColorCorrection.calculateAndApplyCorrection(ipDeconvolved, averageRGBDeconvolved, tableRGB, algorithmChoice);
        ImagePlus impCorrected = new ImagePlus("" + imp.getShortTitle() + "_Corrigida", ipCorrected);
        impCorrected.show();

        double[][] averageRGBCalibrated = averageRGB(ipCorrected, newPoints);
        double[][] averageLABCalibrated = ColorTools.convertRGBtoLAB(averageRGBCalibrated);
        double meanDifferencePlane = meanColorDifference(averageLABCalibrated, tableLAB);

        IJ.log("Mean Difference Before: " + meanDifference);
        IJ.log("Mean Difference After: " + meanDifferencePlane);
        printStdErrorMinMax(meanDifferencePlane, averageLABCalibrated, tableLAB);

        ImageProcessor model1new = ResolutionCorrection.findModel(ipCorrected, newPoints, 1);
        ImageProcessor model2old = ResolutionCorrection.findModel(ipPerspective, newPoints, 2);
        ImageProcessor model2new = ResolutionCorrection.findModel(ipCorrected, newPoints, 2);

        double[] resultsAnt1 = ResolutionCorrection.analyzeModel(model1old, true);
        double[] resultsDep1 = ResolutionCorrection.analyzeModel(model1new, true);
        double[] resultsAnt2 = ResolutionCorrection.analyzeModel(model2old, true);
        double[] resultsDep2 = ResolutionCorrection.analyzeModel(model2new, true);

        String resultText2 = QRDecoder.decode(ipCorrected);
        IJ.log(resultText2);
        calibrateImageSizes(impCorrected);

        if (shouldClearModel) {
            impPerspective = clearModel(impPerspective, ipPerspective, newPoints);
            clearModel(impCorrected, ipCorrected, newPoints);
        }

        calibrateImageSizes(impPerspective);
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
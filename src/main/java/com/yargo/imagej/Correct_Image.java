package com.yargo.imagej;

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
    final static int NUMBER_OF_PATCHES = 16;

    static double width = WIDTH_WHITE;
    static double height = HEIGHT_WHITE;
    static int majorSide = 1;
    static int minorSide = 1;
    static boolean isSuperior = true;
    static boolean shouldSkipResolutionCorrection = false;
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

    public static void defineOuterSize(ImageProcessor ip, Rectangle cardOuterRectangle) {
        /*
        Defines in which of the three borders the outer rectangle is located
        based on pixel intensities of an external point (located at the outer
        rectangle) and an internal point (located at the distance of a border)

        - If the external value is low (black) and the internal value is high
        (white), it is the external black border
        - If the external value is low (black) and the external value is low
        (black), it is the internal black border
        - Otherwise, it is the white border
         */
        ImageProcessor ipByte = ip.convertToByte(false);
        int externalValue = ipByte.get(
                (int) cardOuterRectangle.getMinX() + 5,
                (int) cardOuterRectangle.getMinY() + 5
        );
        int internalValue = ipByte.get(
                (int) (cardOuterRectangle.getMinX() + 5 + cardOuterRectangle.getWidth() * 0.20 / (WIDTH_BLACK_EXT)),
                (int) (cardOuterRectangle.getMinY() + 5 + cardOuterRectangle.getHeight() * 0.20 / (WIDTH_BLACK_EXT))
        );
        if ((internalValue < 50 && internalValue >= 2 * externalValue)
                || (internalValue >= 50 && internalValue > 1.1 * externalValue)) {
            width = WIDTH_BLACK_EXT;
            height = HEIGHT_BLACK_EXT;
            IJ.log("Black External Border");
        } else if ((internalValue < 50 && internalValue < 2 * externalValue && internalValue > 0.5 * externalValue)
                || (internalValue >= 50 && internalValue > 1.1 * externalValue && internalValue < 0.9 * externalValue)) {
            width = WIDTH_BLACK_INT;
            height = HEIGHT_BLACK_INT;
            IJ.log("Black Internal Border");
        } else {
            width = WIDTH_WHITE;
            height = HEIGHT_WHITE;
            IJ.log("White Border");
        }

    }

    public static void defineIsSuperior(ImageProcessor ip, Rectangle cardOuterRectangle) {
        /*
        Identify if the card is oriented upwards or downwards based on the
        location of the white patches
         */
        double[] rgbSum = new double[3];
        double[] rgbTemp = new double[3];
        int cont;
        int value;
        double x, y;

        double[] values = {0, 0};
        for (int i = -1; i < 2; i += 2) {
            x = (
                cardOuterRectangle.getCenterX()
                - (3.0 / 2 * SIDE_PATCH + 3.0 / 2 * INTER_DISTANCE) / width * majorSide
            );
            y = (
                cardOuterRectangle.getCenterY()
                + i * (1.0 / 2 * SIDE_PATCH + 1.0 / 2 * INTER_DISTANCE) / height * minorSide
            );
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
        isSuperior = !(values[0] > values[1]);
    }

    public ImagePlus clearCard(ImagePlus input, PointRoi cardExtremePoints) {
        ImageProcessor ip = input.getProcessor();
        ip.setRoi(cardExtremePoints.getBounds());
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
        ImagePlus impClear = new ImagePlus(input.getTitle(), ip);
        impClear.show();
        return impClear;
    }

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB;
    }


    public void run(ImageProcessor ip) {
        IJ.log("\\Clear");
        IJ.log("" + imp.getTitle());
        tic();

        GenericDialog dialog = new GenericDialog("Select options");
        String[] algorithmOptions = {"Plane", "TPS-3D"};
        dialog.addChoice("Calibration Model", tableValuesRGB.keySet().toArray(new String[0]), "Ideal");
        dialog.addChoice("Calibration Algorithm", algorithmOptions, "TPS-3D");
        dialog.addCheckbox("Skip resolution correction", false);
        dialog.addCheckbox("Clear model from final result", false);
        dialog.showDialog();
        if (dialog.wasCanceled()) return;
        String table = dialog.getNextChoice();
        double[][] tableRGB = tableValuesRGB.get(table);
        double[][] tableLAB = tableValuesLAB.get(table);
        String algorithmChoice = dialog.getNextChoice();
        shouldSkipResolutionCorrection = dialog.getNextBoolean();
        shouldClearModel = dialog.getNextBoolean();

        ImagePlus preprocessedImp = CardFinder.preprocessImage(imp);
        PointRoi cardExtremePoints = PerspectiveTransform.getCardExtremePoints(preprocessedImp);
        preprocessedImp.setRoi(cardExtremePoints);
        ImagePlus impPerspective = PerspectiveTransform.transform(preprocessedImp, cardExtremePoints);
        impPerspective.show();

        ImageProcessor ipPerspective = impPerspective.getProcessor();

        Rectangle cardOuterRectangle = cardExtremePoints.getBounds();
        defineOuterSize(ipPerspective, cardOuterRectangle);
        defineIsSuperior(ipPerspective, cardOuterRectangle);

        ImageProcessor model1old;
        try {
            model1old = ResolutionCorrection.findValidModel(ipPerspective, cardExtremePoints);
        } catch (RuntimeException e) {
            return;
        }

        ImageProcessor ipDeconvolved;
        if (!shouldSkipResolutionCorrection) {
            ImagePlus impDeconvolved = ResolutionCorrection.calibrate(impPerspective, model1old);
            ipDeconvolved = impDeconvolved.getProcessor();
        }
        else {
            ipDeconvolved = ipPerspective;
        }

        double[][] averageRGBPerspective = ColorCorrection.averageRGB(ipPerspective, cardOuterRectangle, isSuperior);
        double[][] averageLABPerspective = ColorTools.convertRGBtoLAB(averageRGBPerspective);
        double meanDifference = ColorCorrection.meanColorDifference(averageLABPerspective, tableLAB);
        if (meanDifference > 55) {
            IJ.log("Color table not found");
            return;
        }

        PointRoi centerPointsColors = ColorCorrection.findCenterPointsColors(cardOuterRectangle);
        impPerspective.setRoi(centerPointsColors);

        double[][] averageRGBDeconvolved = ColorCorrection.averageRGB(ipDeconvolved, cardOuterRectangle, isSuperior);

        ImageProcessor ipCorrected = ColorCorrection.calculateAndApplyCorrection(ipDeconvolved, averageRGBDeconvolved, tableRGB, algorithmChoice);
        ImagePlus impCorrected = new ImagePlus("" + imp.getShortTitle() + "_Corrected", ipCorrected);
        impCorrected.show();

        double[][] averageRGBCalibrated = ColorCorrection.averageRGB(ipCorrected, cardOuterRectangle, isSuperior);
        double[][] averageLABCalibrated = ColorTools.convertRGBtoLAB(averageRGBCalibrated);
        double meanDifferenceCalibrated = ColorCorrection.meanColorDifference(averageLABCalibrated, tableLAB);

        IJ.log("Mean Color Difference Before: " + meanDifference);
        IJ.log("Mean Color Difference After: " + meanDifferenceCalibrated);
        ColorCorrection.printStdErrorMinMax(averageLABCalibrated, tableLAB);

        ImageProcessor model1new = ResolutionCorrection.findModel(ipCorrected, cardExtremePoints, 1);
        ImageProcessor model2old = ResolutionCorrection.findModel(ipPerspective, cardExtremePoints, 2);
        ImageProcessor model2new = ResolutionCorrection.findModel(ipCorrected, cardExtremePoints, 2);

        double[] resultsBefore1 = ResolutionCorrection.analyzeModel(model1old, false);
        double[] resultsAfter1 = ResolutionCorrection.analyzeModel(model1new, false);
        double[] resultsBefore2 = ResolutionCorrection.analyzeModel(model2old, false);
        double[] resultsAfter2 = ResolutionCorrection.analyzeModel(model2new, false);


        calibrateImageSizes(impCorrected);

        if (shouldClearModel) {
            impPerspective = clearCard(impPerspective, cardExtremePoints);
            clearCard(impCorrected, cardExtremePoints);
        }

        calibrateImageSizes(impPerspective);
        toc();

        String QRText= QRDecoder.decode(ip);
        if (QRText.contains("Error")) QRText = QRDecoder.decode(ipCorrected);
        IJ.log("QR Text: " + QRText);

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
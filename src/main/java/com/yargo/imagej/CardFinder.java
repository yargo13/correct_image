package com.yargo.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.Undo;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;

import java.awt.*;

public class CardFinder {

    public enum IrregularTypes {
        IS_CORRECT, IS_SQUARE, IS_LINE_1, IS_LINE_2
    }

    public static Polygon findCard(ImageProcessor ip, boolean isIrregular) {
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
        if (isIrregular) {
            int threshold = ipByte.getAutoThreshold();
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
    public static IrregularTypes checkIrregularRectangle(Polygon polygon) {
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
        if (r.getHeight() > 0.9 * r.getWidth() && r.getHeight() < 1.1 * r.getWidth()) return IrregularTypes.IS_SQUARE;
        for (int i = 0; i < 4; i++) {
            deltax[i] = Math.abs(xpoints[i] - xpoints[(i + 1) % 4]);
            deltay[i] = Math.abs(ypoints[i] - ypoints[(i + 1) % 4]);
        }

        for (int i = 0; i < 4; i++) {
            if (deltax[i] < r.getWidth() * 0.15 || deltay[i] < r.getHeight() * 0.15) {
                if (deltax[0] > deltax[3]) {
                    return IrregularTypes.IS_LINE_1;
                } else return IrregularTypes.IS_LINE_2;
            }
        }

        return IrregularTypes.IS_CORRECT;
    }

    public static boolean shouldRotate(ImageProcessor ip, Polygon polygon) {
        /*
        Check if the found model is in landscape or portrait model, returning
        true if it needs a 90 degree rotation
         */
        Rectangle r = polygon.getBounds();

        return (r.getHeight() > r.getWidth() && ip.getHeight() > ip.getWidth())
                || r.getHeight() > 1.4 * r.getWidth();
    }

    public static void enlargeAndRotateCanvas(ImagePlus imp, double angle) {
        imp.unlock();
        if (imp.getStackSize() == 1) Undo.setup(Undo.COMPOUND_FILTER, imp);
        IJ.run(imp, "Select All", "");
        IJ.run(imp, "Rotate...", "angle=" + angle);
        Roi roi = imp.getRoi();
        Rectangle r = roi.getBounds();
        if (r.width < imp.getWidth()) r.width = imp.getWidth();
        if (r.height < imp.getHeight()) r.height = imp.getHeight();
        IJ.run(imp, "Canvas Size...", "width=" + r.width + " height=" + r.height + " position=Center zero");
    }

    public static ImagePlus preprocessImage(ImagePlus baseImage) {
        // TODO: Refactor this and findCard to make more sense
        ImageProcessor baseProcessor = baseImage.getProcessor();
        ImageProcessor preprocessedIp = baseProcessor.duplicate();
        preprocessedIp.setBackgroundValue(0);
        ImagePlus preprocessedImp = new ImagePlus("Preprocessed", preprocessedIp);

        Polygon polygon = findCard(preprocessedIp, false);
        IrregularTypes irregular = checkIrregularRectangle(polygon);

        if (irregular != IrregularTypes.IS_CORRECT) {
            enlargeAndRotateCanvas(preprocessedImp, -12.5);
            preprocessedIp = preprocessedImp.getProcessor();
            preprocessedIp.setBackgroundValue(0);
            if (irregular == IrregularTypes.IS_LINE_1) preprocessedIp.rotate(-12.5);
            else preprocessedIp.rotate(12.5);
            polygon = findCard(preprocessedIp, false);
        }

        irregular = checkIrregularRectangle(polygon);
        if (irregular == IrregularTypes.IS_SQUARE) {
            polygon = findCard(preprocessedIp, true);
        }

        if (shouldRotate(baseProcessor, polygon)) {
            preprocessedIp = preprocessedIp.rotateRight();
            polygon = findCard(preprocessedIp, irregular == IrregularTypes.IS_CORRECT);
            preprocessedImp = new ImagePlus(baseImage.getTitle(), preprocessedIp);
        }

        PointRoi pointsRoi = new PointRoi(polygon.xpoints, polygon.ypoints, polygon.npoints);
        preprocessedImp.setRoi(pointsRoi);

        return preprocessedImp;
    }


}

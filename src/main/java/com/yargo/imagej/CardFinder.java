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

    public static boolean shouldRotate(ImageProcessor ip, Polygon polygon) {
        /*
        Check if the found model is in landscape or portrait model, returning
        true if it needs a 90 degree rotation
         */
        Rectangle r = polygon.getBounds();

        return (r.getHeight() > r.getWidth() && ip.getHeight() > ip.getWidth())
                || r.getHeight() > 1.4 * r.getWidth();
    }

    public static void enlargeCanvas(ImagePlus imp, double angle) {
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

    public static ImagePlus preprocessImage(ImagePlus baseImage) {
        ImageProcessor baseProcessor = baseImage.getProcessor();
        ImageProcessor preprocessedIp = baseProcessor.duplicate();
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

        if (shouldRotate(baseProcessor, polygon)) {
            preprocessedIp = preprocessedIp.rotateRight();
            polygon = findCard(preprocessedIp, irregular);
            preprocessedImp = new ImagePlus(baseImage.getTitle(), preprocessedIp);
        }

        PointRoi pointsRoi = new PointRoi(polygon.xpoints, polygon.ypoints, polygon.npoints);
        preprocessedImp.setRoi(pointsRoi);

        return preprocessedImp;
    }


}

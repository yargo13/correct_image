package com.yargo.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;
import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.Mapping;
import mpicbg.ij.util.Util;
import mpicbg.models.*;
import mpicbg.models.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PerspectiveTransform {

    public static PointRoi getCardExtremePoints(ImagePlus impSource) {
        ImageProcessor source = impSource.getProcessor();
        Polygon polygon = impSource.getRoi().getPolygon();
        int h = source.getHeight();
        int w = source.getWidth();

        int[] x = polygon.xpoints;
        int[] y = polygon.ypoints;
        int[] xnew = new int[4];
        int[] ynew = new int[4];
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
        Correct_Image.minorSide = (int) minLength;
        Correct_Image.majorSide = (int) (area / minLength);
        xnew[position] = x[position] + w / 20;
        ynew[position] = y[position] + h / 20;

        if (position == 0) {
            xnew[1] = xnew[0];
            xnew[2] = xnew[0] + Correct_Image.majorSide;
            xnew[3] = xnew[0] + Correct_Image.majorSide;

            ynew[1] = ynew[0] - Correct_Image.minorSide;
            ynew[2] = ynew[0] - Correct_Image.minorSide;
            ynew[3] = ynew[0];

        } else if (position == 1) {
            xnew[0] = xnew[1] - Correct_Image.majorSide;
            xnew[2] = xnew[1];
            xnew[3] = xnew[1] - Correct_Image.majorSide;

            ynew[0] = ynew[1];
            ynew[2] = ynew[1] + Correct_Image.minorSide;
            ynew[3] = ynew[1] + Correct_Image.minorSide;
        } else if (position == 2) {
            xnew[0] = xnew[2] - Correct_Image.majorSide;
            xnew[1] = xnew[2] - Correct_Image.majorSide;
            xnew[3] = xnew[2];

            ynew[0] = ynew[2] + Correct_Image.minorSide;
            ynew[1] = ynew[2];
            ynew[3] = ynew[2] + Correct_Image.minorSide;
        } else {
            xnew[0] = xnew[3];
            xnew[1] = xnew[3] + Correct_Image.majorSide;
            xnew[2] = xnew[3] + Correct_Image.majorSide;

            ynew[0] = ynew[3] - Correct_Image.minorSide;
            ynew[1] = ynew[3] - Correct_Image.minorSide;
            ynew[2] = ynew[3];
        }
        return new PointRoi(xnew, ynew, 4);
    }

    public static ImagePlus transform(ImagePlus impSource, PointRoi cardExtremePoints) {
        /*
         * Use two sets of {@link PointRoi landmarks} selected in two images to map
         * one image to the other.
         *
         * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
         * @version 0.2b
         */
        final ArrayList<PointMatch> matches = new ArrayList<PointMatch>();
        final ImagePlus target = impSource.createImagePlus();
        final ImageProcessor ipSource = impSource.getProcessor();
        final ImageProcessor ipTarget;

        ipTarget = impSource.getProcessor().createProcessor(impSource.getWidth() * 11 / 10, impSource.getHeight() * 11 / 10);

        final List<Point> sourcePoints = Util.pointRoiToPoints((PointRoi) impSource.getRoi());
        final List<Point> templatePoints = Util.pointRoiToPoints(cardExtremePoints);
        final int numMatches = Math.min(sourcePoints.size(), templatePoints.size());
        for (int i = 0; i < numMatches; ++i)
            matches.add(new PointMatch(sourcePoints.get(i), templatePoints.get(i)));
        final Mapping<?> mapping;
        Model<?> model;
        InverseCoordinateTransform ict;
        final HomographyModel2D h = new HomographyModel2D();
        model = h;
        ict = h;
        try {
            model.fit(matches);
        } catch (final NotEnoughDataPointsException e) {
            IJ.showMessage("Not enough landmarks selected to find a transformation model.");
            return target;
        } catch (final IllDefinedDataPointsException e) {
            IJ.showMessage("The set of landmarks is ill-defined in terms of the desired transformation.");
            return target;
        }

        mapping = new InverseTransformMapping<InverseCoordinateTransform>(ict);
        ipSource.setInterpolationMethod(ImageProcessor.BILINEAR);
        mapping.mapInterpolated(ipSource, ipTarget);
        target.setProcessor("" + impSource.getShortTitle() + "_Perspective", ipTarget);

        return target;
    }

}

package com.yargo.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;
import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.Mapping;
import mpicbg.ij.util.Util;
import mpicbg.models.*;

import java.util.ArrayList;
import java.util.List;

public class PerspectiveTransform {
    public static ImagePlus transform(ImagePlus source, PointRoi newPoints) {
        /**
         * Use two sets of {@link PointRoi landmarks} selected in two images to map
         * one image to the other.
         *
         * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
         * @version 0.2b
         */
        final ArrayList<PointMatch> matches = new ArrayList<PointMatch>();
        final ImagePlus target = source.createImagePlus();
        final ImageProcessor ipSource = source.getProcessor();
        final ImageProcessor ipTarget;

        ipTarget = source.getProcessor().createProcessor(source.getWidth() * 11 / 10, source.getHeight() * 11 / 10);

        final List<Point> sourcePoints = Util.pointRoiToPoints((PointRoi) source.getRoi());
        final List<Point> templatePoints = Util.pointRoiToPoints(newPoints);
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
        target.setProcessor("" + source.getShortTitle() + "_Perspectiva", ipTarget);

        return target;
    }
}

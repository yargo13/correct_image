package com.yargo.imagej;

import ij.*;
import ij.io.Opener;
import ij.process.*;
import ij.gui.*;

import java.awt.*;
import ij.plugin.filter.*;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Hashtable;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.Mapping;
import mpicbg.ij.util.Util;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

import Jama.*;

public class Correct_Image implements PlugInFilter {
    ImagePlus imp;
    public static long startTime;
    static double larguraBlackExt = 11.00;
    static double larguraWhite = 10.70;
    static double larguraBlackInt = 10.40;
    static double largura = larguraWhite;
    static double alturaBlackExt = 5.50;
    static double alturaWhite = 5.20;
    static double alturaBlackInt = 4.90;
    static double altura = alturaWhite;
    static double sidePatch = 1.00;
    static double interDistance = 0.20;
    static int majorSide = 1;
    static int minorSide = 1;
    static int numberOfPatches = 16;
    static boolean superior = true;
    static HashMap<String, double[][]> tableValuesRGB = new HashMap<String, double[][]>(){
        {
            double[][] idealRGB = {{92,66,59},{108,73,60},{147,112,83},{171,119,109},
                    {172,139,106},{186,145,124},{181,148,122},{195,168,144},
                    {132,96,70},{147,110,78},{166,130,101},{170,128,99},
                    {186,145,124},{197,152,132},{195,158,140},{200,168,147}};
            double[][] aRGB = {{137,108,102},{147,109,98},{166,142,123},{185,158,156},
                    {199,179,163},{198,178,173},{194,178,169},{203,193,189},
                    {156,128,108},{166,138,116},{184,163,147},{186,162,146},
                    {198,178,172},{203,183,180},{202,189,189},{205,194,193}};
            double[][] bRGB = {{124,105,102},{135,108,103},{160,132,120},{178,139,139},
                    {185,158,145},{186,156,151},{183,158,149},{193,175,170},
                    {151,123,111},{161,131,117},{173,145,133},{175,142,130},
                    {187,157,152},{195,162,158},{193,168,169},{198,176,174}};
            double[][] cRGB = {{107,93,85},{118,96,86},{164,131,111},{185,145,142},
                    {197,168,148},{196,167,160},{196,171,161},{205,188,182},
                    {144,116,99},{163,129,106},{183,152,136},{185,150,134},
                    {196,167,160},{203,174,172},{201,178,176},{207,188,185}};
            put("Ideais", idealRGB);
            put("a",aRGB);
            put("b",bRGB);
            put("c",cRGB);
        }
    };
    static HashMap<String, double[][]> tableValuesLAB = new HashMap<String, double[][]>(){
        {
            double[][] idealLAB =  {{30.611,10.175,8.681},{34.558,13.157,13.786},{49.997,9.862,21.447},{55.027,19.056,13.677},
                    {60.138,7.836,22.381},{63.463,12.471,17.069},{63.679,8.539,18.405},{70.609,6.136,16.036},
                    {43.819,11.179,20.570},{49.381,10.457,23.515},{57.087,9.784,20.988},{56.963,12.345,22.029},
                    {63.463,12.471,17.069},{66.514,14.026,17.001},{67.983,11.023,14.576},{71.140,8.497,15.203}};
            double[][] aLAB = {{48.288,10.313,7.995},{49.583,13.206,12.229},{60.630,6.398,13.368},{67.423,9.827,4.516},
                    {74.132,4.490,10.854},{74.078,6.324,4.949},{73.642,4.473,6.645},{78.633,2.963,2.873},
                    {55.627,8.014,14.800},{59.610,7.127,16.085},{68.310,5.263,11.145},{68.174,6.472,11.600},
                    {74.132,6.152,5.580},{76.050,6.494,4.118},{77.636,4.454,1.844},{79.248,3.730,2.068}};
            double[][] bLAB = {{46.067,7.177,4.374},{48.032,10.060,6.798},{57.365,8.963,10.584},{61.569,14.449,5.540},
                    {67.275,7.820,10.760},{66.866,10.595,6.564},{66.985,8.010,8.215},{72.855,5.545,5.077},
                    {53.895,9.093,10.349},{57.217,9.124,12.205},{62.336,8.902,10.291},{61.621,10.727,11.017},
                    {67.275,10.429,6.545},{69.363,11.657,6.576},{71.119,9.204,3.182},{73.484,7.559,3.818}};
            double[][] cLAB = {{40.557,4.122,6.374},{42.686,6.873,9.273},{57.431,9.286,15.645},{63.667,14.797,7.021},
                    {70.850,7.613,14.116},{70.835,9.713,7.487},{71.739,7.485,8.041},{77.425,5.179,4.969},
                    {51.012,8.177,13.489},{56.597,9.957,17.171},{65.310,8.815,12.806},{64.960,10.542,13.337},
                    {70.850,9.537,7.454},{73.614,9.798,5.131},{74.512,7.878,4.043},{77.619,6.481,3.966}};
            double[][] bLABCM = {{45.967,8.100,8.600},{48.300,11.300,11.100},{57.333,10.700,16.700},{61.200,16.567,12.300},
                    {63.967,9.300,17.533},{66.500,11.867,13.800},{66.267,9.000,14.200},{71.400,6.200,12.200},
                    {53.567,11.100,15.200},{56.567,11.067,16.767},{62.200,10.500,16.267},{62.133,13.067,17.100},
                    {66.733,12.000,14.067},{70.033,13.300,13.633},{70.700,10.100,11.500},{72.667,7.900,12.400}};
            double[][] cLABCM = {{38.200,6.333,8.300},{41.300,9.833,11.500},{56.267,12.267,19.667},{62.200,18.333,13.133},
                    {66.533,10.300,19.767},{69.533,11.900,14.800},{70.000,9.367,15.933},{75.500,6.100,13.700},
                    {49.000,11.367,16.233},{55.500,13.600,20.633},{63.467,11.567,17.700},{63.767,13.433,18.733},
                    {69.667,11.900,14.633},{72.433,11.867,13.033},{73.267,9.400,11.533},{75.567,7.767,13.033}};


            put("Ideais", idealLAB);
            put("a",aLAB);
            put("b",bLAB);
            put("c",cLAB);
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

    public static String QRDecoder(ImageProcessor ip){
        //https://elliottslaughter.com/2011/07/qr-decoder-imagej/
        BufferedImage myimg = ip.convertToByte(false).getBufferedImage();
        Reader reader = new QRCodeReader();
        LuminanceSource source = new BufferedImageLuminanceSource(myimg);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        String resultText = new String();
        try {
            Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result result = reader.decode(bitmap, hints);
            resultText = result.getText();
        } catch (NotFoundException e) { return "Nao Encontrado";
        } catch (ChecksumException e) {
        } catch (FormatException e) {
        }
        return resultText;
    }

    public ImagePlus enlargeCanvas(ImagePlus imp, double angle) {
        imp.unlock();
        if (imp.getStackSize()==1)
            Undo.setup(Undo.COMPOUND_FILTER, imp);
        IJ.run(imp, "Select All", "");
        IJ.run(imp, "Rotate...", "angle="+angle);
        Roi roi = imp.getRoi();
        Rectangle r = roi.getBounds();
        if (r.width<imp.getWidth()) r.width = imp.getWidth();
        if (r.height<imp.getHeight()) r.height = imp.getHeight();
        IJ.run(imp, "Canvas Size...", "width="+r.width+" height="+r.height+" position=Center zero");
        return imp;
    }

    public static Polygon findCard(ImageProcessor ip, int irregular){
        //Registra dimensoes do ROI
        Rectangle roi=ip.getRoi();
        int r0 = roi.y;
        int c0 = roi.x;
        int hROI = roi.height;
        int wROI = roi.width;
        ImageProcessor improc = ip.convertToByte(true);
        ImageProcessor ip2 = improc.duplicate();
        int h = ip2.getHeight();
        int w = ip2.getWidth();
        int v,u;
        improc.findEdges();
        improc.medianFilter(); //Filtro da mediana retira parte do ruido de "bordas" virtuais
        if(irregular == 3){
            int threshold = improc.getAutoThreshold();
            IJ.log(""+(int)(0.65*threshold));
            improc.threshold((int)(0.65*threshold));
        }
        else{
            improc.autoThreshold();
        }
        improc.medianFilter();
        ImagePlus impa = new ImagePlus("Teste", improc); //Imagem das bordas, para verificacao de problemas
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

        for(int j=r0+1; j<r0+hROI; j++){
            for(int i=c0+1; i<c0+wROI; i++){
                v = ipParticles.get(i, j);
                u = ip2.get(i, j);
                if(v==0 && (polygonfinal == null || !polygonfinal.contains(i, j))){
                    wand.autoOutline(i, j);
                    polygonfinal = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
                    Rectangle r = polygonfinal.getBounds();
                    double rarea = r.getWidth()*r.getHeight();
                    if(rarea > area){
                        area = rarea;
                        //IJ.log(""+area+" i: "+i+" j: "+j);
                        xi = i;
                        yi = j;
                    }

                }
            }
        }
        wand.autoOutline(xi, yi);

        int[] xretang = {w,0,0,0};
        int[] yretang = {0,h,0,0};

        for(int i=0;i<wand.npoints;i++){
            //0: Ponto mais a esquerda
            if(wand.xpoints[i]<xretang[0]){
                xretang[0] = wand.xpoints[i];
                yretang[0] = wand.ypoints[i];
            }
            //1: Ponto mais em cima
            if(wand.ypoints[i]<yretang[1]){
                xretang[1] = wand.xpoints[i];
                yretang[1] = wand.ypoints[i];
            }
            //2: Ponto mais a direita
            if(wand.xpoints[i]>xretang[2]){
                xretang[2] = wand.xpoints[i];
                yretang[2] = wand.ypoints[i];
            }
            //3: Ponto mais embaixo
            if(wand.ypoints[i]>yretang[3]){
                xretang[3] = wand.xpoints[i];
                yretang[3] = wand.ypoints[i];
            }
        }

        Polygon polygon = new Polygon(xretang, yretang, 4);
        return polygon;
    }

    public static int findIrregularRetang(ImageProcessor ip, Polygon polygon){
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
        if(r.getHeight() > 0.9*r.getWidth() && r.getHeight()<1.1*r.getWidth()) return 3;
        for (int i=0; i<4; i++){
            deltax[i] = Math.abs(xpoints[i]-xpoints[(i+1)%4]);
            deltay[i] = Math.abs(ypoints[i]-ypoints[(i+1)%4]);
        }

        for (int i=0; i<4; i++){
            if(deltax[i] < r.getWidth()*0.15 || deltay[i] < r.getHeight()*0.15){
                IJ.log("Delta x: "+deltax[i]+" Deltay: "+deltay[i]);
                if(deltax[0] > deltax[3]){
                    return 1;
                }
                else return 2;
            }
        }

        return 0;
    }

    public static Boolean checkOrientation(ImageProcessor ip, Polygon polygon){
        Boolean turn=false;
        Rectangle r = polygon.getBounds();

        if((r.getHeight()>r.getWidth() && ip.getHeight()>ip.getWidth()) || r.getHeight()>1.4*r.getWidth()) turn=true;
        return turn;
    }

    public static void getNewPoints(ImageProcessor source, Polygon polygon, int[] xnew, int[] ynew){
        int h = source.getHeight();
        int w = source.getWidth();

        int[] x = polygon.xpoints;
        int[] y = polygon.ypoints;
        double area = 0.5*(x[0]*y[1]+x[1]*y[2]+x[2]*y[3]+x[3]*y[0]-y[0]*x[1]-y[1]*x[2]-y[2]*x[3]-y[3]*x[0]);

        double length;
        double minLength = 99999;
        int position = 0;

        //Ve qual é o lado menor, indicando o ponto inicial do segmento
        for(int i=0; i<4; i++){
            length = Math.sqrt((x[(i+1)%4]-x[i])*(x[(i+1)%4]-x[i])+(y[(i+1)%4]-y[i])*(y[(i+1)%4]-y[i]));
            if(length< minLength){
                minLength = length;
                position = i;
            }
        }
        minorSide = (int)minLength;
        majorSide = (int)(area/minLength);
        xnew[position] = x[position]+w/20;
        ynew[position] = y[position]+h/20;

        if(position==0){
            xnew[1] = xnew[0];
            xnew[2] = xnew[0]+majorSide;
            xnew[3] = xnew[0]+majorSide;

            ynew[1] = ynew[0]-minorSide;
            ynew[2] = ynew[0]-minorSide;
            ynew[3] = ynew[0];

        }
        else if(position==1){
            xnew[0] = xnew[1]-majorSide;
            xnew[2] = xnew[1];
            xnew[3] = xnew[1]-majorSide;

            ynew[0] = ynew[1];
            ynew[2] = ynew[1]+minorSide;
            ynew[3] = ynew[1]+minorSide;
        }
        else if(position==2){
            xnew[0] = xnew[2]-majorSide;
            xnew[1] = xnew[2]-majorSide;
            xnew[3] = xnew[2];

            ynew[0] = ynew[2]+minorSide;
            ynew[1] = ynew[2];
            ynew[3] = ynew[2]+minorSide;
        }
        else if(position==3){
            xnew[0] = xnew[3];
            xnew[1] = xnew[3]+majorSide;
            xnew[2] = xnew[3]+majorSide;

            ynew[0] = ynew[3]-minorSide;
            ynew[1] = ynew[3]-minorSide;
            ynew[2] = ynew[3];
        }

    }

    public static ImagePlus transformPerspective(ImagePlus source, PointRoi newPoints){
        /**
         * Use two sets of {@link PointRoi landmarks} selected in two images to map
         * one image to the other.
         *
         * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
         * @version 0.2b
         */
        final ArrayList< PointMatch > matches = new ArrayList< PointMatch >();
        final ImagePlus target = source.createImagePlus();
        final ImageProcessor ipSource = source.getProcessor();
        final ImageProcessor ipTarget;

        ipTarget = source.getProcessor().createProcessor( source.getWidth()*11/10, source.getHeight()*11/10 );

        final List< Point > sourcePoints = Util.pointRoiToPoints( ( PointRoi )source.getRoi() );
        final List< Point > templatePoints = Util.pointRoiToPoints(newPoints);
        final int numMatches = Math.min( sourcePoints.size(), templatePoints.size() );
        for ( int i = 0; i < numMatches; ++i )
            matches.add( new PointMatch( sourcePoints.get( i ), templatePoints.get( i ) ) );
        final Mapping< ? > mapping;
        Model< ? > model;
        InverseCoordinateTransform ict;
        final HomographyModel2D h = new HomographyModel2D();
        model = h;
        ict = h;
        try	{
            model.fit( matches );
        }
        catch ( final NotEnoughDataPointsException e ){
            IJ.showMessage( "Not enough landmarks selected to find a transformation model." );
            return target;
        }
        catch ( final IllDefinedDataPointsException e )	{
            IJ.showMessage( "The set of landmarks is ill-defined in terms of the desired transformation." );
            return target;
        }

        mapping = new InverseTransformMapping< InverseCoordinateTransform >( ict );
        ipSource.setInterpolationMethod( ImageProcessor.BILINEAR);
        mapping.mapInterpolated( ipSource, ipTarget );
        target.setProcessor(""+source.getShortTitle()+"_Perspectiva", ipTarget );

        return target;
    }

    public static void calibrateImage(ImagePlus imp){
        ij.measure.Calibration calibration = imp.getCalibration();
        calibration.pixelWidth = largura/majorSide;
        calibration.pixelHeight = altura/minorSide;
        calibration.setUnit("cm");
    }

    public static void checkSize(ImageProcessor ip, int[] xnew, int[] ynew, Rectangle r){
        ImageProcessor ipByte = ip.convertToByte(false);
        int v = ipByte.get((int)r.getMinX()+5, (int)r.getMinY()+5);
        int u = ipByte.get((int)(r.getMinX()+r.getWidth()*0.20/(larguraBlackExt)+5), (int)(r.getMinY()+r.getHeight()*0.20/(larguraBlackExt)+5));
        IJ.log("U: "+u+" V: "+v);
        if((u<50 && u >= 2*v) || (u>=50 && u>1.1*v)){
            largura = larguraBlackExt;
            altura = alturaBlackExt;
            IJ.log("Black Ext");
        }
        else if((u<50 && u < 2*v && u>0.5*v) || (u>=50 && u>1.1*v && u<0.9*v)){
            largura = larguraBlackInt;
            altura = alturaBlackInt;
            IJ.log("Black Int");
        }
        else{
            largura = larguraWhite;
            altura = alturaWhite;
            IJ.log("White");
        }

    }

    public static void checkSuperior(ImageProcessor ip, PointRoi newpoints){
        Rectangle r = newpoints.getBounds();
        double[] rgbSum = new double[3];
        double[] rgbTemp = new double[3];
        int cont;
        int value;
        double x,y;

        //Identificar se cores estão na metade superior ou inferior através do patch em branco.
        double[] values = {0,0};
        for(int i=-1;i<2;i+=2){
            x = r.getCenterX() - (3.0/2*sidePatch+3.0/2*interDistance)/largura*majorSide;
            y = r.getCenterY() + i*(1.0/2*sidePatch+1.0/2*interDistance)/altura*minorSide;
            cont = 0;
            rgbSum[0] = 0; rgbSum[1] = 0; rgbSum[2] = 0;
            for(int row=(int)(x-(sidePatch/4*majorSide/largura));row<(int)(x+(sidePatch/4*majorSide/largura));row++){
                for(int col=(int)(y-(sidePatch/4*minorSide/altura));col<(int)(y+(sidePatch/4*minorSide/altura));col++){
                    value = ip.get(row, col);
                    extractRGB(value, rgbTemp);
                    rgbSum[0] += rgbTemp[0]; rgbSum[1] += rgbTemp[1]; rgbSum[2] += rgbTemp[2];
                    cont += 1;
                }
            }
            rgbSum[0] /= cont; rgbSum[1] /= cont; rgbSum[2] /= cont;
            values[(int)((i+1)*0.5)] = Math.sqrt(rgbSum[0]*rgbSum[0]+rgbSum[1]*rgbSum[1]+rgbSum[2]*rgbSum[2]);

        }
        //IJ.log("Values[0] "+values[0]+ " Values[1] "+values[1]);
        if(values[0]>values[1]) superior = false;
        else superior = true;
    }

    public static ImageProcessor findModel(ImageProcessor ip, PointRoi points, int position){
        Rectangle r = points.getBounds();
        double xCenterSquare = 0, yCenterSquare = 0;
        int x = 0,y = 0, width = 0, height = 0;
        switch(position){
            case 1:
                xCenterSquare = (r.getCenterX() - (3*sidePatch + 3*interDistance)*r.getWidth()/largura);
                if(superior) yCenterSquare = (r.getCenterY() + (sidePatch + interDistance)*r.getHeight()/altura);
                else yCenterSquare = (r.getCenterY() - (sidePatch + interDistance)*r.getHeight()/altura);
                x = (int)(xCenterSquare - (sidePatch+0.5*interDistance)*r.getWidth()/largura);
                y = (int)(yCenterSquare - (sidePatch+0.5*interDistance)*r.getHeight()/altura);
                width = (int)(1*(2*sidePatch+interDistance)*r.getWidth()/largura);
                height = (int)(1*(2*sidePatch+interDistance)*r.getHeight()/altura);
                break;
            case 2:
                xCenterSquare = (r.getCenterX() + (3*sidePatch + 3*interDistance)*r.getWidth()/largura);
                if(superior) yCenterSquare = (r.getCenterY() + (sidePatch + interDistance)*r.getHeight()/altura);
                else yCenterSquare = (r.getCenterY() - (sidePatch + interDistance)*r.getHeight()/altura);
                x = (int)(xCenterSquare - (sidePatch+0.5*interDistance)*r.getWidth()/largura);
                y = (int)(yCenterSquare - (sidePatch+0.5*interDistance)*r.getHeight()/altura);
                width = (int)(1*(2*sidePatch+interDistance)*r.getWidth()/largura);
                height = (int)(1*(2*sidePatch+interDistance)*r.getHeight()/altura);
                break;
        }
        Rectangle rSquare = new Rectangle(x, y, width, height);
        ip.setRoi(rSquare);
        ImageProcessor ipByte = ip.crop();
        ipByte = ipByte.convertToByte(false);
        ImageProcessor ipThresh = ipByte.duplicate();
        int w = ipThresh.getWidth();
        int h = ipThresh.getHeight();
        int v, xi = 0, yj = 0;
        double max = -2;
        double min = 256;
        for(int i=0;i<h*w;i++){
            v = ipThresh.get(i);
            if(v>max) max = v;
            if(v<min) min = v;
        }
        double threshold = 0;
        if(max>190 && min>90){
            threshold = (max+min)*0.5;
            ipThresh.threshold((int)threshold);
        }
        else{
            ipThresh.autoThreshold();
        }

        //IJ.log("Max: "+max+" Min: "+min+" Thresh: "+(int)threshold);

        ImagePlus impThresh = new ImagePlus("Threshold", ipThresh);
        Wand wand = new Wand(ipThresh);
        Polygon polygon = null;
        r = ipThresh.getRoi();
        double area = r.getHeight()*r.getWidth();
        int xWand = (int)(r.getCenterX() + r.getWidth()*40.5/100);
        int yWand = (int)(r.getCenterY());
        wand.autoOutline(xWand, yWand);
        polygon = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
        if(polygon.getBounds().getWidth()*polygon.getBounds().getHeight() > 0.85*area){
            xWand = (int)(r.getCenterX() + r.getWidth()*37.0/100);
            yWand = (int)(r.getCenterY());
        }
        if(polygon.getBounds().getWidth()*polygon.getBounds().getHeight() < 0.70*area){
            xWand = (int)(r.getCenterX());
            yWand = (int)(r.getCenterY() + r.getHeight()*40.0/100);
        }
        wand.autoOutline(xWand, yWand);
        polygon = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
        r = polygon.getBounds();
        r = new Rectangle((int)r.getMinX()+1, (int)r.getMinY()+1, r.width-2, r.height-2);
        ipByte.setRoi(r);
        ipByte = ipByte.crop();

        impThresh.setRoi(new PolygonRoi(polygon, Roi.POLYGON));
        //impThresh.show();

        ImagePlus impByte = new ImagePlus("Model", ipByte);
        //impByte.show();

        return ipByte;
    }


    public static ImagePlus extractPSF(ImagePlus imX){
        ImageProcessor ipPSF = imX.getChannelProcessor();
        int x=0,y=0, v, max=0;
        for(int i=0;i<ipPSF.getWidth()/5;i++){
            for(int j=0;j<ipPSF.getHeight()/5;j++){
                v = ipPSF.get(i, j);
                if(v>max){
                    x = i;
                    y = j;
                    max = v;
                }
            }
        }
        Rectangle rPSF = null;
        int min = Math.min(x, y);
        min = Math.min(min, 7);
        rPSF = new Rectangle(x-min, y-min, 2*min+1, 2*min+1);
        ipPSF.setRoi(rPSF);
        ipPSF = ipPSF.crop();

        ImagePlus psf = new ImagePlus("PSF", ipPSF);
        //psf.show();

        return psf;
    }

    public static void extractRGB(int value, double[] extracted){
        extracted[0] = (value >> 16) & 0xff;
        extracted[1] = (value >> 8) & 0xff;
        extracted[2] = value & 0xff;
    }

    public static double[][] averageRGB(ImageProcessor ip, PointRoi newpoints){
        Rectangle r = newpoints.getBounds();
        double[] rgbSum = new double[3];
        double[][] averageValues = new double[numberOfPatches][3];
        double[] rgbTemp = new double[3];
        int cont;
        int value;
        double x,y;

        for(int i=0;i<2;i++){
            for(int j=0;j<8;j++){
                x = r.getCenterX() - (7.0/2*sidePatch+7.0/2*interDistance)/largura*majorSide + j*(sidePatch+interDistance)/largura*majorSide;
                if(superior) y = r.getCenterY() - (3.0/2*sidePatch+3.0/2*interDistance)/altura*minorSide + i*(sidePatch+interDistance)/altura*minorSide;
                else y = r.getCenterY() + (1.0/2*sidePatch+1.0/2*interDistance)/altura*minorSide + i*(sidePatch+interDistance)/altura*minorSide;
                cont = 0;
                rgbSum[0] = 0; rgbSum[1] = 0; rgbSum[2] = 0;
                for(int row=(int)(x-(sidePatch/4*majorSide/largura));row<(int)(x+(sidePatch/4*majorSide/largura));row++){
                    for(int col=(int)(y-(sidePatch/4*minorSide/altura));col<(int)(y+(sidePatch/4*minorSide/altura));col++){
                        value = ip.get(row, col);
                        extractRGB(value, rgbTemp);
                        rgbSum[0] += rgbTemp[0]; rgbSum[1] += rgbTemp[1]; rgbSum[2] += rgbTemp[2];
                        cont += 1;
                    }
                }
                rgbSum[0] /= cont; rgbSum[1] /= cont; rgbSum[2] /= cont;
                averageValues[8*i+j][0] = rgbSum[0];
                averageValues[8*i+j][1] = rgbSum[1];
                averageValues[8*i+j][2] = rgbSum[2];
            }
        }
        //Verifica cor do patch na primeira linha e primeira coluna e na ultima linha e ultima coluna, vendo qual é maior

        if(!superior){
            double[][] rearrangedValues = new double[numberOfPatches][3];
            for(int i=0;i<numberOfPatches;i++){
                rearrangedValues[i] = averageValues[numberOfPatches-i-1];
            }
            averageValues = rearrangedValues.clone();
        }

        Matrix correctM = new Matrix(averageValues);
        correctM.print(2, 4);


        return averageValues;
    }


    /*
     * Converte primeiramente RGB para XYZ e então para LAB, assumindo que RGB já estaria calibrado
     */

    public static double[][] convertRGBtoLAB(double[][] RGBValues){
        double[][] LABValues = new double[RGBValues.length][3];
        for(int i=0;i<RGBValues.length;i++){
            LABValues[i] = convertRGBtoLAB(RGBValues[i]);
        }
        return LABValues;
    }

    public static double[] convertRGBtoLAB(double[] RGBValues){
        double[] LABValues = new double[3];
        double r, g, b;
        double Xaux, Yaux, Zaux;
        // Conversão RGB para XYZ

        /* Passagem para 0-1 */
        r = RGBValues[0]/255.0;
        g = RGBValues[1]/255.0;
        b = RGBValues[2]/255.0;

        /* Linearização do sRGB */
        if (r>0.04045) r = Math.pow(((r+0.055)/1.055),2.4);
        else r = r/12.92;
        if (g>0.04045) g = Math.pow(((g+0.055)/1.055),2.4);
        else g = g/12.92;
        if (b>0.04045) b = Math.pow(((b+0.055)/1.055),2.4);
        else b = b/12.92;
        r = r*100;
        g = g*100;
        b = b*100;

        /* Conversão através de matriz */
        Xaux = (r*0.4124 + g*0.3576 + b*0.1805);
        Yaux = (r*0.2126 + g*0.7152 + b*0.0722);
        Zaux = (r*0.0193 + g*0.1192 + b*0.9505);

        // Conversão XYZ para LAB
        /* Normalização pelo Reference White Point */
        Xaux = Xaux/95.0429;
        Yaux = Yaux/100.000;
        Zaux = Zaux/108.890;

        if ( Xaux > 0.008856 ) Xaux = Math.pow(Xaux, 1.0/3.0);
        else Xaux = (7.787*Xaux)+(16.0/116.0);
        if ( Yaux > 0.008856 ) Yaux = Math.pow(Yaux, 1.0/3.0);
        else Yaux = (7.787*Yaux)+(16.0/116.0);
        if ( Zaux > 0.008856 ) Zaux = Math.pow(Zaux, 1.0/3.0);
        else Zaux = (7.787*Zaux)+(16.0/116.0);
        LABValues[0] = 116.0*Yaux - 16.0;
        LABValues[1] = 500.0*(Xaux - Yaux);
        LABValues[2] = 200.0*(Yaux - Zaux);
        return LABValues;
    }

    public static int[][] convertLABtoRGB(double[][] LABValues){
        int[][] RGBValues = new int[LABValues.length][3];
        for(int i=0;i<LABValues.length;i++){
            RGBValues[i] = convertLABtoRGB(LABValues[i]);
        }
        return RGBValues;
    }

    public static int[] convertLABtoRGB(double[] LABValues){
        int[] RGBValues = new int[3];
        double Xaux, Yaux, Zaux;
        double varR, varG, varB;

        // Conversão LAB para XYZ
        Yaux = (LABValues[0]+16)/116.0;
        Xaux = LABValues[1]/500.0 + Yaux;
        Zaux = Yaux - LABValues[2]/200.0;
        if(Xaux*Xaux*Xaux > 0.008856) Xaux = Xaux*Xaux*Xaux;
        else Xaux = (Xaux - 16.0/116.0)/7.787;
        if(Yaux*Yaux*Yaux > 0.008856) Yaux = Yaux*Yaux*Yaux;
        else Yaux = (Yaux - 16.0/116.0)/7.787;
        if(Zaux*Zaux*Zaux > 0.008856) Zaux = Zaux*Zaux*Zaux;
        else Zaux = (Zaux - 16.0/116.0)/7.787;

        /* Normalização pelo Reference White Point */
        Xaux = Xaux*95.0429;
        Yaux = Yaux*100.000;
        Zaux = Zaux*108.890;

        //Conversão XYZ para RGB
        Xaux = Xaux/100;
        Yaux = Yaux/100;
        Zaux = Zaux/100;
        varR = Xaux*3.2406 + Yaux*-1.5372 + Zaux*-0.4986;
        varG = Xaux*-0.9689 + Yaux*1.8758 + Zaux*0.0415;
        varB = Xaux*0.0557 + Yaux*-0.2040 + Zaux*1.0570;
        if (varR > 0.0031308) varR = 1.055*(Math.pow(varR, 1.0/2.4)) - 0.055;
        else varR = 12.92*varR;
        if (varG > 0.0031308) varG = 1.055*(Math.pow(varG, 1.0/2.4)) - 0.055;
        else varG = 12.92*varG;
        if (varB > 0.0031308) varB = 1.055*(Math.pow(varB, 1.0/2.4)) - 0.055;
        else varB = 12.92*varB;
        varR = (varR < 0) ? 0: varR;
        varG = (varG < 0) ? 0: varG;
        varB = (varB < 0) ? 0: varB;
        varR = (varR > 1) ? 1: varR;
        varG = (varG > 1) ? 1: varG;
        varB = (varB > 1) ? 1: varB;
        RGBValues[0] = (int)(varR*255);
        RGBValues[1] = (int)(varG*255);
        RGBValues[2] = (int)(varB*255);
        return RGBValues;
    }

    public static double[][] calculateCorrectionPlane(double[][] averageValues, double[][] tableValues){
        double[][] M = new double[4][numberOfPatches];
        for(int i=0; i<4;i++){
            for(int j=0; j<(averageValues.length);j++){
                if(i != 3) M[i][j] = averageValues[j][i];
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

    public static ImageProcessor applyCorrectionPlaneLAB(double[][] correction, ImageProcessor ip){
        ImageProcessor plane = ip.duplicate();
        int h = ip.getHeight();
        int w = ip.getWidth();
        int v;
        double[] tempRGB = new double[3];
        int[] finalRGB = new int[3];
        double[] tempLAB = new double[3];
        double[] finalLAB = new double[3];
        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                v = ip.get(i, j);
                extractRGB(v, tempRGB);
                tempLAB = convertRGBtoLAB(tempRGB);
                for(int k=0;k<3;k++){
                    finalLAB[k] = (correction[0][k]*tempLAB[0]+correction[1][k]*tempLAB[1]+correction[2][k]*tempLAB[2]+correction[3][k]*1);
                }
                finalRGB = convertLABtoRGB(finalLAB);
                plane.putPixel(i, j, finalRGB);
            }
        }
        return plane;
    }

    public static ImageProcessor applyCorrectionPlane(double[][] correction, ImageProcessor ip){
        ImageProcessor plane = ip.duplicate();
        int h = ip.getHeight();
        int w = ip.getWidth();
        int v;
        double[] tempRGB = new double[3];
        int[] finalRGB = new int[3];
        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                v = ip.get(i, j);
                extractRGB(v, tempRGB);

                for(int k=0;k<3;k++){
                    finalRGB[k] = (int)(correction[0][k]*tempRGB[0]+correction[1][k]*tempRGB[1]+correction[2][k]*tempRGB[2]+correction[3][k]);
                }
                for(int k=0;k<3;k++){
                    if(finalRGB[k]>255) finalRGB[k] = 255;
                    if(finalRGB[k]<0) finalRGB[k] = 0;
                }
                plane.putPixel(i, j, finalRGB);
            }
        }
        return plane;
    }

    public double meanColorDifference(double[][] averageValues, double[][] tableValues){
        double sum=0, partialSum;
        for(int i=0;i<averageValues.length;i++){
            partialSum = 0;
            for(int j=0;j<3;j++){
                partialSum += (averageValues[i][j]-tableValues[i][j])*(averageValues[i][j]-tableValues[i][j]);
            }
            sum += Math.sqrt(partialSum);
        }
        return sum/averageValues.length;
    }

    public void printStdErrorMinMax(double meanDifference, double[][] averageValues, double[][] tableValues){
        double sum=0, partialSum, stdError, minDif=999, patchMin=0, maxDif=-1, patchMax=0;
        IJ.log("Values Color");
        for(int i=0;i<averageValues.length;i++){
            partialSum = 0;
            for(int j=0;j<3;j++){
                partialSum += (averageValues[i][j]-tableValues[i][j])*(averageValues[i][j]-tableValues[i][j]);
            }
            sum += (Math.sqrt(partialSum)-meanDifference)*(Math.sqrt(partialSum)-meanDifference);
            //IJ.log(""+Math.sqrt(partialSum));
            if(Math.abs(Math.sqrt(partialSum)) > maxDif){
                maxDif = Math.sqrt(partialSum);
                patchMax = i;
            }
            if(Math.abs(Math.sqrt(partialSum)) < minDif){
                minDif = Math.sqrt(partialSum);
                patchMin = i;
            }
        }
        stdError = Math.sqrt(sum/averageValues.length);
        IJ.log("Desvio Padrao: "+stdError);
        IJ.log("Diferenca Minima: "+minDif+" no patch: "+patchMin);
        IJ.log("Diferenca Maxima: "+maxDif+" no patch: "+patchMax+"\n");
    }

    //Analisa modelo aproximadamente quadrado e devolve no vetor results o valor de MSE, Contagem de Threshold e SSIM
    //Caso print seja selecionado, informa detalhes das medições no log
    public void analyzeModel(ImageProcessor model, double[] results, boolean print){
        int h = model.getHeight();
        int w = model.getWidth();
        if(h < 3.0/4*w || w < 3.0/4*h){
            IJ.log("Modelo Invalido");
            results[0] = 1;
            results[1] = 1;
            results[2] = 0;
        }
        double[] modelArray = new double[(w-4)*(h-4)];
        double[] baseArray = new double[(w-4)*(h-4)];
        double[] modelArrayNorm = new double[(w-4)*(h-4)];
        double[] baseArrayNorm = new double[(w-4)*(h-4)];

        InputStream is = Correct_Ilumination.class.getResourceAsStream("/Modelo.tif");
        Opener opener = new Opener();
        ImagePlus imBase = opener.openTiff(is, "Modelo");
        ImageProcessor ipBase = imBase.getProcessor();
        ipBase = ipBase.resize(w, h, false);
        double min = 255, max = 0, prodIntern=0, normModel=0, normBase=0,
                difQuad=0, avgModel=0, avgBase=0,
                sumDifferences=0, sumAbsDifferences=0,
                countThreshold = 0, sumSquares=0;

        //Threshold de 10% para classificacao correta da cor
        double threshold = 0.1;

        for(int j=0;j<h-4;j++){
            for(int i=0;i<w-4;i++){
                modelArray[(w-4)*j + i] = (double)(model.get(i+2, j+2))/255;
                baseArray[(w-4)*j + i] = (double)(ipBase.get(i+2, j+2))/255;
            }
        }

        w=w-4;
        h=h-4; //troca valores para facilitar calculos

        for(int i=0;i<modelArray.length;i++){
            if(modelArray[i]>max) max = modelArray[i];
            if(modelArray[i]<min) min = modelArray[i];
        }
        for(int i=0;i<modelArray.length;i++){
            modelArray[i] = (modelArray[i]-min)/(max-min);
            avgModel += modelArray[i];
            avgBase += baseArray[i];
            prodIntern += modelArray[i]*baseArray[i];
            normModel += modelArray[i]*modelArray[i];
            normBase += baseArray[i]*baseArray[i];
            difQuad += (baseArray[i]-modelArray[i])*(baseArray[i]-modelArray[i]);
            sumSquares += (baseArray[i]*baseArray[i]);
            if(Math.abs(baseArray[i]-modelArray[i])>threshold) countThreshold +=1;
        }

        countThreshold = countThreshold/modelArray.length;
        avgModel = avgModel/modelArray.length;
        avgBase = avgBase/modelArray.length;

        double covar=0, varModel=0, varBase=0, c=0.0001;
        for(int i=0;i<modelArray.length;i++){
            varModel += (modelArray[i]-avgModel)*(modelArray[i]-avgModel);
            varBase += (baseArray[i]-avgBase)*(baseArray[i]-avgBase);
            covar += (modelArray[i]-avgModel)*(baseArray[i]-avgBase);
        }
        varModel = varModel/modelArray.length;
        varBase = varBase/modelArray.length;
        covar = covar/modelArray.length;

        double numeratorCoAvg, numeratorCoVar, denominatorAvg, denominatorVar;
        numeratorCoAvg = (2*avgModel*avgBase + c);
        numeratorCoVar = (2*covar + c);
        denominatorAvg = (avgModel*avgModel + avgBase*avgBase + c);
        denominatorVar = (varModel+varBase + c);
        double ssim = numeratorCoAvg/denominatorAvg*numeratorCoVar/denominatorVar;
        double difQuadMed = difQuad/modelArray.length;
        double NRMSE = Math.sqrt(difQuad/sumSquares);


        double minQ = 1, maxQ = 0, ssimSmall, sumSsimSmall=0;
        for(int lin=0;lin<10;lin++){
            for(int col=0;col<10;col++){
                double sumSmallModel=0, sumSmallBase=0, avgSmallModel=0, avgSmallBase=0, covarSmall=0, varSmallModel=0, varSmallBase=0;
                for(int i=0;i<w/10;i++){
                    for(int j=0;j<h/10;j++){
                        sumSmallModel += modelArray[w*(h/10*lin+j)+(i+w/10*col)]; //indice do elemento da submatriz (lin, col)
                        sumSmallBase += baseArray[w*(h/10*lin+j)+(i+w/10*col)];
                    }
                }
                avgSmallModel = sumSmallModel/(h/10*w/10);
                avgSmallBase = sumSmallBase/(h/10*w/10);
                for(int i=0;i<w/10;i++){
                    for(int j=0;j<h/10;j++){
                        covarSmall += (modelArray[w*(h/10*lin+j)+(i+w/10*col)]-avgSmallModel)*(baseArray[w*(h/10*lin+j)+(i+w/10*col)]-avgSmallBase);
                        varSmallModel += (modelArray[w*(h/10*lin+j)+(i+w/10*col)]-avgSmallModel)*(modelArray[w*(h/10*lin+j)+(i+w/10*col)]-avgSmallModel);
                        varSmallBase += (baseArray[w*(h/10*lin+j)+(i+w/10*col)]-avgSmallBase)*(baseArray[w*(h/10*lin+j)+(i+w/10*col)]-avgSmallBase);
                    }
                }
                covarSmall = covarSmall/(h/10*w/10);
                varSmallModel = varSmallModel/(h/10*w/10);
                varSmallBase = varSmallBase/(h/10*w/10);
                ssimSmall = (2*avgSmallModel*avgSmallBase + c)*(2*covarSmall + c)/((avgSmallModel*avgSmallModel + avgSmallBase*avgSmallBase + c)*(varSmallModel+varSmallBase + c));
                sumSsimSmall += ssimSmall;
                if(ssimSmall > maxQ) maxQ = ssimSmall;
                if(ssimSmall < minQ) minQ = ssimSmall;
            }
        }

        if(print){
            IJ.log("MSE: "+difQuadMed);
            IJ.log("NRMSE: " +NRMSE);
            IJ.log("Q: "+ssim);
            //IJ.log("N1: "+numeratorCoAvg+" D1: "+denominatorAvg+" N2: "+numeratorCoVar+" D2: "+denominatorVar);
            IJ.log("Mean Q: "+sumSsimSmall/100);
            IJ.log("Count Threshold: "+countThreshold+" Multiply: "+difQuadMed*countThreshold);
        }
        results[0] = difQuadMed;
        results[1] = countThreshold;
        results[2] = sumSsimSmall/100;
    }

    public ImagePlus clearModel(ImagePlus imp, ImageProcessor ip, PointRoi points){
        ip.setRoi(points.getBounds());
        ip.fill();
        ImageProcessor ipByte = ip.convertToByte(true);
        int h=ip.getHeight();
        int w=ip.getWidth();
        int minX=w, maxX=0, minY=h, maxY=0;
        int u = ipByte.get(1, 1);
        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                if(ipByte.get(i, j)!=u){
                    if(i<minX) minX = i;
                    if(i>maxX) maxX = i;
                    if(j<minY) minY = j;
                    if(j>maxY) maxY = j;
                }
            }
        }


        Rectangle r = new Rectangle(minX, minY, (maxX-minX), (maxY-minY));
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
    	double[][] RGB = convertLABtoRGB(tableValuesLAB);
    	Matrix RGBM = new Matrix(RGB);
    	RGBM.print(2, 4);*/


        IJ.log("\\Clear");
        IJ.log(""+imp.getTitle());
        tic();
        String resultText = QRDecoder(ip);

        GenericDialog dialog = new GenericDialog("Selecionar Calibração");
        String[] algorithmOptions = {"Plane","DID"};
        dialog.addChoice("TableRGB - Calcular Correção", tableValuesRGB.keySet().toArray(new String[tableValuesRGB.keySet().size()]), "Ideal");
        dialog.addChoice("TableLAB - Calcular Erro", tableValuesLAB.keySet().toArray(new String[tableValuesLAB.keySet().size()]), "Ideal");
        dialog.addChoice("Correção", algorithmOptions, "Plane");
        dialog.showDialog();
        if (dialog.wasCanceled()) return;
        double[][] tableRGB = tableValuesRGB.get(dialog.getNextChoice());
        double[][] tableLAB = tableValuesLAB.get(dialog.getNextChoice());
        String algorithmChoice = dialog.getNextChoice();

        ip.setBackgroundValue(0);
        Polygon polygon = findCard(ip, 0);
        int irregular = findIrregularRetang(ip,polygon);

        if(irregular != 0){
            imp = enlargeCanvas(imp, -12.5);
            ip = imp.getChannelProcessor();
            ip.setBackgroundValue(0);
            if(irregular == 1)ip.rotate(-12.5);
            else ip.rotate(12.5);
            polygon = findCard(ip, 0);
            irregular = 0;
        }

        irregular = findIrregularRetang(ip,polygon);
        if(irregular == 3){
            IJ.log("Tá Errado");
            polygon = findCard(ip, irregular);
        }


        Boolean turn = checkOrientation(ip, polygon);
        if (turn) {
            ip = ip.rotateRight();
            polygon = findCard(ip, irregular);
            imp = new ImagePlus(imp.getTitle(), ip);
            //imp.show();
        }

        PointRoi pointsRoi = new PointRoi(polygon.xpoints, polygon.ypoints, polygon.npoints);
        imp.setRoi(pointsRoi);

        int[] xnew = new int[4];
        int[] ynew = new int[4];
        getNewPoints(ip, polygon, xnew, ynew);

        PointRoi newPoints = new PointRoi(xnew, ynew, 4);
        ImagePlus impPerspective = transformPerspective(imp, newPoints);
        //impPerspective.show();

        ImageProcessor ipPerspective = impPerspective.getChannelProcessor();

        Rectangle r = newPoints.getBounds();
        checkSize(ipPerspective, xnew, ynew, r);
        checkSuperior(ipPerspective, newPoints);

        double[] resultsBefore = new double[3];
        ImageProcessor ipDeconvolved;
        ImageProcessor model1 = findModel(ipPerspective, newPoints, 1);
        analyzeModel(model1,  resultsBefore, false);
        if(model1.getHeight() < model1.getWidth()*3.0/4 || model1.getWidth() < model1.getHeight()*3.0/4 || resultsBefore[0] > 0.2){
            model1 = findModel(ipPerspective, newPoints, 2);
            analyzeModel(model1, resultsBefore, false);
        }

        if(model1.getHeight() < model1.getWidth()*3.0/4 || model1.getWidth() < model1.getHeight()*3.0/4 ||  resultsBefore[0] > 0.2){
            IJ.log("Nao foi possivel detectar o modelo de resolucao");
            ipDeconvolved = ipPerspective;
        }

        else{
            ImagePlus imPSF;
            InputStream is = getClass().getResourceAsStream("/Modelo.tif");
            Opener opener = new Opener();
            ImagePlus modelTIF = opener.openTiff(is, "Modelo");
            double[] resultsModel = {1,1,0};
            double[] resultsTemp = new double[3];
            int i=0, sequence=0, iterationsFinal=0;
            if(resultsBefore[2] > 0.62) {
                resultsModel = resultsBefore.clone();
                i = 0;
            }
            while(i<100){
                ImagePlus imX1 = Deconvolution.deconvolveMRNSD(model1, modelTIF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", Integer.toString(i), "4", "false");
                imPSF = extractPSF(imX1);
                ImagePlus impModelDeconv = Deconvolution.deconvolveMRNSD(model1, imPSF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", "1", "4", "false");
                ImageProcessor ipModel = impModelDeconv.getChannelProcessor();
                ipModel = ipModel.convertToByte(false);
                analyzeModel(ipModel, resultsTemp, false);

                if(resultsTemp[0]*resultsTemp[1] < resultsModel[0]*resultsModel[1]){
                    resultsModel[0] = resultsTemp[0];
                    resultsModel[1] = resultsTemp[1];
                    resultsModel[2] = resultsTemp[2];
                    sequence = 0;
                    iterationsFinal = i;
                }
                else sequence+=1;
                if(sequence >= 15){
                    IJ.log("Numero de Iteracoes "+iterationsFinal);
                    break;
                }
                i++;
            }
            if(iterationsFinal > 0){
                ImagePlus imX1 = Deconvolution.deconvolveMRNSD(model1, modelTIF,"NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", Integer.toString(iterationsFinal), "4", "false");
                imPSF = extractPSF(imX1);
                ImagePlus impModelDeconv = Deconvolution.deconvolveMRNSD(model1, imPSF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", "1", "4", "false");
                //impModelDeconv.show();
                ImagePlus impDeconvolved = Deconvolution.deconvolveColorMRNSD(ipPerspective, imPSF, "NONE", "-1", "PERIODIC", "AUTO", "SAME_AS_SOURCE", "DOUBLE", "-1", "-1",
                        "false", "1", "4", "false");
                ipDeconvolved = impDeconvolved.getProcessor();
            }
            else ipDeconvolved = ipPerspective;
            //imPSF.show();

        }

        double[][] averageRGBPerspective = averageRGB(ipPerspective, newPoints);
        double[][] averageLABPerspective = convertRGBtoLAB(averageRGBPerspective);
        double meanDifference = meanColorDifference(averageLABPerspective, tableLAB);

        double x,y;
        int[] xp = new int[25];
        int[] yp = new int[25];
        int aux = 0;
        for(int i=0;i<2;i++){
            for(int j=0;j<8;j++){
                x = r.getCenterX() - (7.0/2*sidePatch+7.0/2*interDistance)/largura*majorSide + j*(sidePatch+interDistance)/largura*majorSide;
                if(superior) y = r.getCenterY() - (3.0/2*sidePatch+3.0/2*interDistance)/altura*minorSide + i*(sidePatch+interDistance)/altura*minorSide;
                else y = r.getCenterY() + (1.0/2*sidePatch+1.0/2*interDistance)/altura*minorSide + i*(sidePatch+interDistance)/altura*minorSide;
                xp[aux] = (int)x;
                yp[aux] = (int)y;
                aux++;
            }
        }
        xp[24] = (int)r.getCenterX();
        yp[24] = (int)r.getCenterY();
        PointRoi pr = new PointRoi(xp,yp,25);
        impPerspective.setRoi(pr);

        ImagePlus impPlane = null;
        if( meanDifference > 55){
            IJ.log("Tabela de cores nao localizada");
        }

        else{
            double[][] averageRGBDeconvolved = averageRGB(ipDeconvolved, newPoints);
            double[][] averageLABDeconvolved = convertRGBtoLAB(averageRGBDeconvolved);

            ImageProcessor ipPlane;
            if (algorithmChoice == "Plane") {
                double[][] correctionPlane = calculateCorrectionPlane(averageRGBDeconvolved, tableRGB);
                ipPlane = applyCorrectionPlane(correctionPlane,ipDeconvolved);
            }
            else {
                //double[][] correctionDID = _Dumpster.calculateCorrectionDID(averageRGBDeconvolved, tableRGB);
                //ipPlane = _Dumpster.applyCorrectionDID(correctionDID, ipDeconvolved);
                double[][] correctionThin = _Dumpster.calculateCorrectionThin(averageRGBDeconvolved, tableRGB);
                ipPlane = _Dumpster.applyCorrectionThin(correctionThin, averageRGBDeconvolved, ipDeconvolved);
            }
            //
            //double[][] correctionPlane = calculateCorrectionPlane(averageLABDeconvolved, tableValuesLAB);
            //ImageProcessor ipPlane = applyCorrectionPlaneLAB(correctionPlane,ipDeconvolved);

            impPlane = new ImagePlus(""+imp.getShortTitle()+"_Corrigida", ipPlane);
            impPlane.show();

            double[][] averageRGBPlane = averageRGB(ipPlane, newPoints);
            double[][] averageLABPlane = convertRGBtoLAB(averageRGBPlane);
            double meanDifferencePlane = meanColorDifference(averageLABPlane, tableLAB);

            IJ.log("Mean Difference Antes: "+meanDifference);
            IJ.log("Mean Difference Depois Plano: "+meanDifferencePlane);
            printStdErrorMinMax(meanDifferencePlane, averageLABPlane,tableLAB);

            ImageProcessor model1new = findModel(ipPlane, newPoints, 1);
            ImageProcessor model2old = findModel(ipPerspective, newPoints, 2);
            ImageProcessor model2new = findModel(ipPlane, newPoints, 2);

            double[] resultsAnt1 = new double[3];
            double[] resultsDep1 = new double[3];
            double[] resultsAnt2 = new double[3];
            double[] resultsDep2 = new double[3];
            analyzeModel(model1, resultsAnt1, true);
            analyzeModel(model1new, resultsDep1, true);
            analyzeModel(model2old, resultsAnt2, true);
            analyzeModel(model2new, resultsDep2, true);

            String resultText2 = QRDecoder(ipPlane);
            IJ.log(resultText2);
            calibrateImage(impPlane);

            //impPerspective = clearModel(impPerspective, ipPerspective, newPoints);
            //impPlane = clearModel(impPlane, ipPlane, newPoints);
        }

        calibrateImage(impPerspective);
        toc();
        //IJ.selectWindow("Log");
        //IJ.saveAs("Text", "C:\\Users\\Yargo\\Desktop\\PTC2892\\Iniciação\\Fotos\\Carlos Eduardo - Projeto\\Logs\\"+imp.getTitle());
        IJ.log(resultText);


        //IJ.saveAs(impPerspective, "png", "C:\\Users\\Yargo\\Desktop\\PTC2892\\Iniciação\\Fotos\\Carlos Eduardo - Projeto\\Perspectiva\\"+impPerspective.getTitle());
        //IJ.saveAs(impPlane, "png", "C:\\Users\\Yargo\\Desktop\\PTC2892\\Iniciação\\Fotos\\Carlos Eduardo - Projeto\\Corrigida\\"+impPlane.getTitle());

    }
}
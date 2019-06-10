package com.yargo.imagej;

public class ColorTools {
    public static void extractRGB(int value, double[] extracted){
        extracted[0] = (value >> 16) & 0xff;
        extracted[1] = (value >> 8) & 0xff;
        extracted[2] = value & 0xff;
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
//        Xaux = (r*0.4124 + g*0.3576 + b*0.1805);
//        Yaux = (r*0.2126 + g*0.7152 + b*0.0722);
//        Zaux = (r*0.0193 + g*0.1192 + b*0.9505);
        Xaux = (r*0.4361 + g*0.3807 + b*0.1431);
        Yaux = (r*0.2225 + g*0.7169 + b*0.0606);
        Zaux = (r*0.0139 + g*0.0971 + b*0.7142);

        // Conversão XYZ para LAB
        /* Normalização pelo Reference White Point  D65*/
//        Xaux = Xaux/95.0429;
//        Yaux = Yaux/100.000;
//        Zaux = Zaux/108.890;
        Xaux = Xaux/96.422;
        Yaux = Yaux/100.000;
        Zaux = Zaux/82.521;

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
        Xaux = Xaux*96.422;
        Yaux = Yaux*100.000;
        Zaux = Zaux*82.521;

        //Conversão XYZ para RGB
        Xaux = Xaux/100;
        Yaux = Yaux/100;
        Zaux = Zaux/100;
//        varR = Xaux*3.2406 + Yaux*-1.5372 + Zaux*-0.4986;
//        varG = Xaux*-0.9689 + Yaux*1.8758 + Zaux*0.0415;
//        varB = Xaux*0.0557 + Yaux*-0.2040 + Zaux*1.0570;
        varR = Xaux*3.1339 + Yaux*-1.6169 + Zaux*-0.4906;
        varG = Xaux*-0.9788 + Yaux*1.9161 + Zaux*0.0335;
        varB = Xaux*0.0719 + Yaux*-0.2290 + Zaux*1.4052;
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
}

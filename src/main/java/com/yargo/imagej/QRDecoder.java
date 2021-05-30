package com.yargo.imagej;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.util.Hashtable;

public class QRDecoder {
    public static String decode(ImageProcessor ip) {
        //https://elliottslaughter.com/2011/07/qr-decoder-imagej/
        BufferedImage myimg = ip.convertToByte(false).getBufferedImage();
        Reader reader = new QRCodeReader();
        LuminanceSource source = new BufferedImageLuminanceSource(myimg);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        String resultText = "";
        try {
            Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result result = reader.decode(bitmap, hints);
            resultText = result.getText();
        } catch (NotFoundException e) {
            return "Error: not found";
        } catch (ChecksumException e) {
            return "Error: checksum";
        } catch (FormatException e) {
            return "Error: format";
        }
        return resultText;
    }
}

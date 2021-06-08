package qrcode;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成二维码工具
 * @author xiaozj
 */
@Slf4j
public class QrCodeBuilder {
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    private static BufferedImage toBufferedImage(BitMatrix bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_3BYTE_BGR);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                image.setRGB(i, j, bm.get(i, j) ? BLACK : WHITE);
            }
        }
        return image;
    }

    private static File writeBitMatricToFile(BitMatrix bm, String format,
                                             File file) {
        BufferedImage image = toBufferedImage(bm);
        try {
            if (!ImageIO.write(image, format, file)) {
                throw new RuntimeException("Can not write an image to file" + file);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return file;
    }

    private static OutputStream writeToStream(BitMatrix matrix, String format, OutputStream stream) throws IOException {
        BufferedImage image = toBufferedImage(matrix);
        if (!ImageIO.write(image, format, stream)) {

        }
        return stream;
    }

    public static OutputStream qrCodeStream(String codeText, String format) throws Exception {
        int width = 300;
        int height = 300;
        // 二维码的输入是字符串
        HashMap<EncodeHintType, String> hints = new HashMap<>(2);
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        // 条形码的格式是 BarcodeFormat.EAN_13
        // 二维码的格式是BarcodeFormat.QR_CODE
        BitMatrix bm = new MultiFormatWriter().encode(codeText,
                BarcodeFormat.QR_CODE, width, height, hints);

        // 生成二维码图片
        OutputStream outputStream = new ByteArrayOutputStream();
        QrCodeBuilder.writeToStream(bm,format,outputStream);
        return outputStream;
    }

    public static File qrCodeFile(String codeText, String format, String fileName) throws Exception {
        int width = 300;
        int height = 300;
        // 二维码的输入是字符串
        HashMap<EncodeHintType, String> hints = new HashMap<>(2);
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        // 条形码的格式是 BarcodeFormat.EAN_13
        // 二维码的格式是BarcodeFormat.QR_CODE
        BitMatrix bm = new MultiFormatWriter().encode(codeText,
                BarcodeFormat.QR_CODE, width, height, hints);

        // 生成二维码图片
        File out = new File(fileName);
        QrCodeBuilder.writeBitMatricToFile(bm, format, out);

        return out;
    }

    public static BufferedImage qrCodeImage(String codeText) throws Exception {
        int width = 300;
        int height = 300;
        // 二维码的输入是字符串
        HashMap<EncodeHintType, Object> hints = new HashMap<>(2);
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        // 条形码的格式是 BarcodeFormat.EAN_13
        // 二维码的格式是BarcodeFormat.QR_CODE
        BitMatrix bm = new MultiFormatWriter().encode(codeText,
                BarcodeFormat.QR_CODE, width, height, hints);

        return QrCodeBuilder.toBufferedImage(bm);
    }

    /**
     * 解码图片
     * @param imageFile 图片文件
     * @return String
     */
    public static String decoderImage(File imageFile) {
        //二维码图片路径
        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<>(2);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            //解码获取二维码中信息
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            return result.getText();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * 解码图片
     * @param file 图片文件
     * @return String
     */
    public static String decoderImage(MultipartFile file) {
        //二维码图片路径
        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<>(2);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            //解码获取二维码中信息
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            return result.getText();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

}

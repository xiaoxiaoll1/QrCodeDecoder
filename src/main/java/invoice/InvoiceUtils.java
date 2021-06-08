package invoice;

import com.google.common.collect.Lists;
import invoice.bean.InvoiceBean;
import invoice.bean.InvoiceType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import pdf.PdfParsingOptimizingUtil;
import qrcode.QrCodeBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

/**
 * 该类实现了发票的二维码解析
 * @author xiaozj
 */
@Slf4j
public class InvoiceUtils {

    

    private static String INVOICE_MULTIPART_LOCATION;

    @Value("${spring.http.multipart.location}")
    public void setInvoiceMultipartLocation(String invoiceMultipartLocation) {
        if(StringUtils.isBlank(invoiceMultipartLocation)) {
            //若未配置发票文件存放路径，则使用系统默认的临时文件存放路径
            INVOICE_MULTIPART_LOCATION = System.getProperty("java.io.tmpdir");
        } else {
            INVOICE_MULTIPART_LOCATION = invoiceMultipartLocation;
        }
    }

    private static InvoiceUtils invoiceUtils;
    
    @PostConstruct
    public void init() {
        invoiceUtils = this;
    }
    
    /**
     * 获取文件中的发票信息
     * @param file 发票文件
     * @return Invoice
     */
    public static InvoiceBean fileToInvoice(MultipartFile file){
        if (null == file) {
            log.error("文件为空");
        }

        //文件格式校验
        String fileName = file.getOriginalFilename();
        String fileFormat = FilenameUtils.getExtension(fileName).toUpperCase();
        if(!InvoiceSupportFile.contains(fileFormat)){
            log.error("不支持的发票文件格式");
        }

        try{
            InvoiceBean invoiceBean;
            if(InvoiceSupportFile.PDF.name().equals(fileFormat)) {
                invoiceBean = pdfFileToInvoice(file);
            } else {
                String decoderImage = QrCodeBuilder.decoderImage(file);
                invoiceBean = toInvoice(decoderImage);
            }
            return invoiceBean;
        }catch (Exception e){
            log.error("发票解析失败", e);
            return null;
        }
    }

    /**
     * 获取文件中的发票信息
     * @param filePath 文件地址
     * @return Invoice
     */
    public static InvoiceBean fileToInvoice(String filePath){
        String sourceFileNameExt = FilenameUtils.getExtension(filePath).toUpperCase();
        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            log.error("找不到源文件");
        }

        if(!InvoiceSupportFile.contains(sourceFileNameExt)){
            log.error(StringUtils.join("不支持的发票文件格式，支持的格式如下：", Arrays.toString(InvoiceSupportFile.values())));
        }

        try{
            String decoderImage = QrCodeBuilder.decoderImage(inputFile);
            InvoiceBean invoiceBean = toInvoice(decoderImage);
            return invoiceBean;
        }catch (Exception e){
            log.error("发票解析失败", e);
            return null;
        }
    }

    /**
     * 获取pdf转化为的图片中的发票信息
     * @param filePathList 文件地址
     * @return Invoice
     */
    public static InvoiceBean pdfImgToInvoice(List<String> filePathList){
        String decoderImage = null;
        try{
            for (String filePath : filePathList) {
                File inputFile = new File(filePath);
                decoderImage = QrCodeBuilder.decoderImage(inputFile);
                if (null!=decoderImage) {
                    break;
                }
            }
            InvoiceBean invoiceBean = toInvoice(decoderImage);
            return invoiceBean;
        }catch (Exception e){
            log.error("发票解析失败", e);
            return null;
        }
    }
    
    

    /**
     * 保存发票文件
     * @param invoiceFile
     * @return 发票文件名称
     * @throws IOException
     */
    public static String saveInvoiceFile(MultipartFile invoiceFile) throws IOException {
        //获取文件格式
        String fileName = invoiceFile.getOriginalFilename();
        String fileFormat = FilenameUtils.getExtension(fileName);

        if(!InvoiceSupportFile.contains(fileFormat.toUpperCase())) {
            log.error("无效的文件格式");
        }

        File file;
        do {
            String newFileName = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16).toUpperCase() + "." + fileFormat;
            file = new File(StringUtils.join(INVOICE_MULTIPART_LOCATION, newFileName));
        } while (file.exists());

        invoiceFile.transferTo(file);
        return file.getName();
    }

    /**
     * pdf转jpg
     * @param filePath
     * @return
     */
    public static List<String> pdfToImagePath(String filePath) {
        String fileFormat = FilenameUtils.getExtension(filePath).toUpperCase();
        String pdf = "PDF";
        if(!pdf.equals(fileFormat)) {
            log.error("不支持的文件格式");
        }

        String today = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
        String fileDirectory = filePath.substring(0, filePath.lastIndexOf("/") + 1) + today;

        List<String> list = Lists.newArrayList();
        try {
            PDDocument doc = PDDocument.load(new File(filePath));
            list = createImage(doc, fileDirectory, fileName);
            doc.close();
        } catch (IOException e) {
            log.error("PDF转JPG失败", e);
        }
        return list;
    }

    /**
     * pdf转jpg
     * @param invoiceFile
     * @return
     */
    public static List<String> pdfToImagePath(MultipartFile invoiceFile) {
        String originalFileName = invoiceFile.getOriginalFilename();
        String fileFormat = FilenameUtils.getExtension(originalFileName).toUpperCase();
        String pdf = "PDF";
        if(!pdf.equals(fileFormat)) {
            log.error("不支持的文件格式");
        }

        String today = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        String fileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        String fileDirectory = INVOICE_MULTIPART_LOCATION + today;

        List<String> list = Lists.newArrayList();
        try {
            PDDocument doc = PDDocument.load(invoiceFile.getInputStream());
            list = PdfParsingOptimizingUtil.convertImage(doc, fileDirectory);
            //list = createImage(doc, fileDirectory, fileName);
            doc.close();
        } catch (IOException e) {
            log.error("PDF转JPG失败", e);
        }
        return list;
    }

    /**
     * PDF生成JPG图片
     * @param doc
     * @param fileDirectory
     * @param fileName
     * @return
     * @throws IOException
     */
    private static List<String> createImage(PDDocument doc, String fileDirectory, String fileName) throws IOException {
        File f = new File(fileDirectory);
        if (!f.exists()) {
            f.mkdir();
        }
        //PDF转JPG默认缩放比
        Float scale = 2.0f;
        List<String> list = Lists.newArrayList();
        PDFRenderer renderer = new PDFRenderer(doc);
        int pageCount = doc.getNumberOfPages();
        for (int i = 0; i < pageCount; i++) {
            //第二个参数是设置缩放比(即像素),参数越大生成图片分辨率越高，转换时间也就越长
            BufferedImage image = renderer.renderImage(i, scale);
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String imagePath = String.format("%s/%s-%s-%s.jpg", fileDirectory, fileName, i + 1, uuid);
            ImageIO.write(image, "JPG", new File(imagePath));
            list.add(imagePath);
        }
        return list;
    }

    /**
     * 删除文件
     * @param fileList
     */
    public static void deleteFileList(List<String> fileList) {
        fileList.forEach(filePath -> {
            File f = new File(filePath);
            if(f.exists() & !f.isDirectory()){
                f.delete();
            }
        });
    }

    /**
     * 获取PDF文件中的发票信息
     * @param invoiceFile
     * @return Invoice
     */
    public static InvoiceBean pdfFileToInvoice(MultipartFile invoiceFile){
        //PDF转JPG
        List<String> filePathList = new ArrayList<>();
        try{
            filePathList = pdfToImagePath(invoiceFile);

            if(CollectionUtils.isEmpty(filePathList)) {
                log.error("无效的文件");
            }
        } catch (Exception e) {
            log.error("发票解析失败");
        }

        //只解析第一张图片
//        String qrCodeImgPath = filePathList.get(0);
//        InvoiceBean invoiceBean = fileToInvoice(qrCodeImgPath);
        InvoiceBean invoiceBean = pdfImgToInvoice(filePathList);

        //删除转化后的临时文件
        try {
            deleteFileList(filePathList);
        } catch (Exception e) {
            log.error("临时文件删除失败：" + filePathList.toString(), e);
        }

        return invoiceBean;
    }

    private static InvoiceBean toInvoice(String decoderImage){
        String[] decoders = decoderImage.split("[,]");

        //增值税电子普通发票，二维码解析出的数据个数，7个或者8个
        List<Integer> vatElectronicGeneralDataLength = Arrays.asList(7, 8);
        if(!vatElectronicGeneralDataLength.contains(decoders.length)){
            log.error("无效的二维码");
        }

        InvoiceType invoiceType = null;
        String typeStr = decoders[0]+","+decoders[1];
        if(InvoiceType.contains(typeStr)){
            invoiceType = InvoiceType.getInvoiceType(typeStr);
        }else {
            log.error("暂时不支持该发票类型");
        }

        Date billingDate;
        try {
            billingDate = DateUtils.parseDate(decoders[5],"yyyyMMdd");
        } catch (ParseException e) {
            billingDate = null;
        }

        InvoiceBean invoiceBean = InvoiceBean
                .builder()
                .invoiceType(invoiceType)
                .invoiceCode(decoders[2])
                .invoiceNo(decoders[3])
                .amount(new BigDecimal(decoders[4]))
                .billingDate(billingDate)
                .invoiceVerificationCode(decoders[6])
                .build();

        return invoiceBean;
    }
}

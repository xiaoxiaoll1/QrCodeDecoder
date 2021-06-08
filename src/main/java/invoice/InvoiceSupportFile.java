package invoice;

/**
 * 发票支持文件类型
 * @author xiaozj
 */
public enum InvoiceSupportFile {

    /**
     * PNG
     */
    PNG,

    /**
     * JPG
     */
    JPG,

    /**
     * JPG
     */
    JPEG,

    /**
     * GIF
     */
    GIF,

    /**
     * BMP
     */
    BMP,

    /**
     * PDF
     */
    PDF
    ;

    public static boolean contains(String type){
        for(InvoiceSupportFile invoiceSupportFile : InvoiceSupportFile.values()){
            if(invoiceSupportFile.name().equals(type)){
                return true;
            }
        }
        return false;
    }

}

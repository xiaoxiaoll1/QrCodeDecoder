package invoice.bean;

/**
 * 发票种类
 * @author xiaozj
 */
public enum InvoiceType {

    /**
     * 增值税电子普通发票
     */
    VAT_ELECTRONIC_GENERAL("01,10","增值税电子普通发票"),

    /**
     * 增值税专用发票
     */
    VAT_SPECIAL("01,10","增值税专用发票"),

    /**
     * 增值税普通发票
     */
    VAT_GENERAL("01,04","增值税普通发票"),

    /**
     * 广东通用机打发票
     */
    GUANGDONG_GENERAL("01,20","广东通用机打发票"),
    ;

    private String typeName;
    private String typeCode;

    InvoiceType(String typeCode,String typeName){
        this.typeCode = typeCode;
        this.typeName = typeName;
    }

    public static boolean contains(String type){
        for(InvoiceType invoiceType : InvoiceType.values()){
            if(invoiceType.typeCode.equals(type)){
                return true;
            }
        }
        return false;
    }

    public static InvoiceType getInvoiceType(String type){
        for(InvoiceType invoiceType : InvoiceType.values()){
            if(invoiceType.typeCode.equals(type)){
                return invoiceType;
            }
        }
        return null;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeCode() {
        return typeCode;
    }
}

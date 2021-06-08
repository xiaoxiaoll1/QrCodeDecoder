package invoice.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 发票
 * @author xiaozj
 */
@Data
@Builder
@AllArgsConstructor
public class InvoiceBean {
    /**
     * 发票类型
     */
    private InvoiceType invoiceType;
    /**
     * 发票代码
     */
    private String invoiceCode;
    /**
     * 发票号码
     */
    private String invoiceNo;
    /**
     * 合计金额（不含税金额）
     */
    private BigDecimal amount;
    /**
     * 发票日期
     */
    private Date billingDate;
    /**
     * 发票验证码
     */
    private String invoiceVerificationCode;
}

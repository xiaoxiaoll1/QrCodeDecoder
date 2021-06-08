package pdf;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.springframework.web.multipart.MultipartFile;
import pdf.bean.ReceiptPosition;
import pdf.util.NumberValidationUtils;
import pdf.util.SortUtil;
import pdf.util.StringUtil;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取PDF中的二维码信息
 * @author xiaozj
 */
public class PdfInvoiceUtils {
    protected static final String kpje_key = "(小写)";
    protected static final String kpje_key2 = "（小写）";
    protected static final String slv_key = "税率";
    private static final String FPDM = "发票代码";
    private static final String FPHM = "发票号码";
    private static final String KPRQ = "开票日期";
    private static final String JYM = "校验码";
    private static final String MC = "名称";
    private static final String NSRSBH = "纳税人识别号";
    private static final String DZDH = "地址、电话";
    private static final String KHHJZH = "开户行及账号";
    private static final String JQBH = "机器编号";
    private static final String HJ = "合计";
    private static final String HJ_H = "合";
    private static final String HJ_J = "计";
    private static final String JSHJ = "价税合计";
    private static final String HWLWFWMC = "货物或应税劳务、服务名称";
    private static final String HWLWFWMC_P = "货物或应税劳务、服";
    private static final String GGXH = "规格型号";
    private static final String DW = "单位";
    private static final String DW_D = "单";
    private static final String DW_W = "位";
    private static final String SL = "数量";
    private static final String SL_S = "数";
    private static final String SL_L = "量";
    private static final String DJ = "单价";
    private static final String DJ_D = "单";
    private static final String DJ_J = "价";
    private static final String JE = "金额";
    private static final String JE_J = "金";
    private static final String JE_E = "额";
    private static final String SLV = "税率";
    private static final String SLV_S = "税";
    private static final String SLV_LV = "率";
    private static final String SE = "税额";
    private static final String SE_S = "税";
    private static final String SE_E = "额";
    private static final String MMQ = "密码区";
    private static final String BZ = "备注";
    private static final String MMQ_BZ = "密码区备注";
    private static ReceiptPosition jqbhR = null;
    private static ReceiptPosition hjR = null;
    private static ReceiptPosition jshjR = null;
    private static ReceiptPosition hwmcR = null;
    private static ReceiptPosition ggxhR = null;
    private static ReceiptPosition danweiR = null;
    private static ReceiptPosition shuliangR = null;
    private static ReceiptPosition danjiaR = null;
    private static ReceiptPosition jineR = null;
    private static ReceiptPosition shuilvR = null;
    private static ReceiptPosition shuieR = null;
    private static ReceiptPosition mmqR = null;
    private static ReceiptPosition bzR = null;
    private static ReceiptPosition xh$R = null;
    private static ReceiptPosition hwmc$R = null;
    private static ReceiptPosition ggxh$R = null;
    private static ReceiptPosition dw$R = null;
    private static ReceiptPosition sl$R = null;
    private static ReceiptPosition dj$R = null;
    private static ReceiptPosition je$R = null;
    private static ReceiptPosition slv$R = null;
    private static ReceiptPosition se$R = null;

    private static Pattern NUMBER_PATTERN = Pattern.compile("[0-9\\.]+");

    public PdfInvoiceUtils() {
    }

    private static String trim(String str) {
        if (str == null) {
            return "";
        } else {
            str = str.trim();
            str = str.replaceFirst(":", "");
            str = str.replaceFirst("：", "");
            return str.trim();
        }
    }

    public static JSONObject readReceiptPdfFile(File pdfFileName) throws Exception {
        PDDocument document = PDDocument.load(pdfFileName);
        return readReceiptPdf(document);
    }

    public static JSONObject readReceiptPdfFile(MultipartFile file) throws Exception {
        PDDocument document = PDDocument.load(file.getInputStream());
        return readReceiptPdf(document);
    }

    public static JSONObject readReceiptPdfURL(String pdfFileName) throws Exception {
        URL url = new URL(pdfFileName);
        PDDocument document = PDDocument.load(url.openStream());
        return readReceiptPdf(document);
    }

    private static JSONObject readReceiptPdf(PDDocument document) throws Exception {
        List<ReceiptPosition> mainList = null;
        List addendumList = null;

        try {
            PDPageTree pages = document.getPages();
            int pageCount = pages.getCount();
            PdfTextStripperNew stripperMain;
            if (pageCount == 1) {
                stripperMain = new PdfTextStripperNew();
                stripperMain.setStartPage(1);
                stripperMain.setEndPage(1);
                stripperMain.setSortByPosition(true);
                stripperMain.getText(document);
                mainList = stripperMain.getPosList();
            } else if (pageCount > 1) {
                stripperMain = new PdfTextStripperNew();
                stripperMain.setStartPage(1);
                stripperMain.setEndPage(1);
                stripperMain.setSortByPosition(true);
                stripperMain.getText(document);
                mainList = stripperMain.getPosList();
                PdfTextStripperNew stripperAddendum = new PdfTextStripperNew();
                stripperAddendum.setStartPage(2);
                stripperAddendum.setEndPage(2);
                stripperAddendum.setSortByPosition(true);
                stripperAddendum.getText(document);
                addendumList = stripperAddendum.getPosList();
            }
        } finally {
            if (document != null) {
                document.close();
            }

        }

        specificDW(mainList);
        if (addendumList != null) {
            JSONObject json = reorganizationRegulation(mainList);

            try {
                JSONObject xhqdJson = reorganizationRegulationSA(addendumList);
                json.put("qdxxs", xhqdJson);
                return json;
            } catch (Exception var10) {
                return json;
            }
        } else {
            return reorganizationRegulation(mainList);
        }
    }

    private static JSONObject reorganizationRegulationSA(List<ReceiptPosition> addendumList) {
        JSONObject json = new JSONObject();
        SortUtil.sort(addendumList, new String[]{"posY"});
        List<ReceiptPosition> indexList = new ArrayList();
        List<ReceiptPosition> lineStrList = new ArrayList();

        for(int i = 0; i < addendumList.size(); ++i) {
            ReceiptPosition rp = addendumList.get(i);
            ReceiptPosition receiptPosition;
            ReceiptPosition r;
            if (i + 1 < addendumList.size()) {
                receiptPosition = addendumList.get(i + 1);
                if (Math.abs(rp.getPosY() - receiptPosition.getPosY()) < 2.0F) {
                    indexList.add(rp);
                } else {
                    indexList.add(rp);
                    SortUtil.sort(indexList, new String[]{"posX"});
                    r = new ReceiptPosition();
                    Iterator var50 = indexList.iterator();

                    while(var50.hasNext()) {
                        ReceiptPosition r1 = (ReceiptPosition)var50.next();
                        String text = r1.getText();
                        if (text == null) {
                            text = "";
                        }

                        text = String.valueOf(text) + r.getText();
                        r.setPosEndX(r.getPosEndX());
                        r.setPosEndY(r.getPosEndY());
                        r.setPosLastX(r.getPosLastX());
                        r.setPosLastY(r.getPosLastY());
                        r.setPosX(r.getPosX());
                        r.setPosY(r.getPosY());
                        r.setText(text);
                    }

                    lineStrList.add(r);
                    indexList = new ArrayList();
                }
            } else if (i + 1 == addendumList.size()) {
                SortUtil.sort(indexList, new String[]{"posX"});
                indexList.add(rp);
                receiptPosition = new ReceiptPosition();
                Iterator var8 = indexList.iterator();

                while(var8.hasNext()) {
                    r = (ReceiptPosition)var8.next();
                    String text = receiptPosition.getText();
                    if (text == null) {
                        text = "";
                    }

                    text = String.valueOf(text) + r.getText();
                    receiptPosition.setPosEndX(r.getPosEndX());
                    receiptPosition.setPosEndY(r.getPosEndY());
                    receiptPosition.setPosLastX(r.getPosLastX());
                    receiptPosition.setPosLastY(r.getPosLastY());
                    receiptPosition.setPosX(r.getPosX());
                    receiptPosition.setPosY(r.getPosY());
                    receiptPosition.setText(text);
                }

                lineStrList.add(receiptPosition);
            }
        }

        if ("销售货物或者提供应税劳务清单".equals(((ReceiptPosition)lineStrList.get(lineStrList.size() - 1)).getText())) {
            float xsfY$ = 0.0F;
            float bzY$ = 0.0F;
            float zjY$ = 0.0F;
            float xjY$ = 0.0F;
            float xhY$ = 0.0F;
            float fpdmY$ = 0.0F;
            float xsfmcY$ = 0.0F;
            float gmfmcY$ = 0.0F;

            ReceiptPosition rp;
            String xjse;
            for(int j = 0; j < lineStrList.size(); ++j) {
                rp = (ReceiptPosition)lineStrList.get(j);
                xjse = rp.getText().trim().replaceAll(" ", "").replaceAll("：", ":");
                if (xjse.indexOf("销售方") > -1 && xjse.indexOf("填开日期") > -1) {
                    int lastIndex = xjse.lastIndexOf(":");
                    xjse = xjse.substring(lastIndex + 1, xjse.length()).replace("年", "-").replace("月", "-").replace("日", "");
                    json.put("xhqd_tkrq", xjse);
                    if (xsfY$ == 0.0F) {
                        xsfY$ = rp.getPosY();
                    }
                }

                if (xjse.indexOf("备注") > -1 && bzY$ == 0.0F) {
                    bzY$ = rp.getPosY();
                }

                if (xjse.indexOf("总计") > -1 && zjY$ == 0.0F) {
                    zjY$ = rp.getPosY();
                }

                if (xjse.indexOf("小计") > -1 && xjY$ == 0.0F) {
                    xjY$ = rp.getPosY();
                }

                if (xjse.indexOf("序号") > -1 && xhY$ == 0.0F) {
                    xhY$ = rp.getPosY();
                }

                if (xjse.indexOf("所属增值税电子普通发票代码") > -1 && fpdmY$ == 0.0F) {
                    json.put("xhqd_fpdm", xjse.substring(xjse.indexOf(":") + 1, xjse.lastIndexOf("号码")));
                    json.put("xhqd_fphm", xjse.substring(xjse.lastIndexOf(":") + 1, xjse.lastIndexOf("共")));
                    fpdmY$ = rp.getPosY();
                }

                if (xjse.indexOf("销售方名称") > -1 && xsfmcY$ == 0.0F) {
                    json.put("xhqd_xsfmc", xjse.substring(xjse.indexOf(":") + 1, xjse.length()));
                    xsfmcY$ = rp.getPosY();
                }

                if (xjse.indexOf("购买方名称") > -1 && gmfmcY$ == 0.0F) {
                    json.put("xhqd_gmfmc", xjse.substring(xjse.indexOf(":") + 1, xjse.length()));
                    gmfmcY$ = rp.getPosY();
                }
            }

            ArrayList<ReceiptPosition> beanList = new ArrayList();
            Iterator var55 = addendumList.iterator();

            while(var55.hasNext()) {
                rp = (ReceiptPosition)var55.next();
                if (Math.abs(rp.getPosY() - xhY$) < 2.0F) {
                    beanList.add(rp);
                }
            }

            SortUtil.sort(beanList, new String[]{"posX"});
            String text;
            if (xh$R == null) {
                var55 = beanList.iterator();

                while(var55.hasNext()) {
                    rp = (ReceiptPosition)var55.next();
                    text = rp.getText().trim();
                    if ("序号".equals(text)) {
                        xh$R = rp;
                        xh$R.setText("序号");
                    }

                    if ("货物(劳务)名称".equals(text)) {
                        hwmc$R = rp;
                        hwmc$R.setText("货物名称");
                    }

                    if ("规格型号".equals(text)) {
                        ggxh$R = rp;
                        ggxh$R.setText("规格型号");
                    }

                    if ("单位".equals(text)) {
                        dw$R = rp;
                        dw$R.setText("单位");
                    }

                    if ("数量".equals(text.replaceAll(" ", ""))) {
                        sl$R = rp;
                        sl$R.setText("数量");
                    }

                    if ("单价".equals(text.replaceAll(" ", ""))) {
                        dj$R = rp;
                        dj$R.setText("单价");
                    }

                    if ("金额".equals(text.replaceAll(" ", ""))) {
                        je$R = rp;
                        je$R.setText("金额");
                    }

                    if ("税率".equals(text.replaceAll(" ", ""))) {
                        slv$R = rp;
                        slv$R.setText("税率");
                    }

                    if ("税额".equals(text.replaceAll(" ", ""))) {
                        se$R = rp;
                        se$R.setText("税额");
                    }
                }
            }

            String xjje = "";
            xjse = "";
            text = "";
            String zjse = "";
            StringBuilder bzText = new StringBuilder();
            ArrayList<ReceiptPosition> xh$List = new ArrayList();
            ArrayList<ReceiptPosition> hwmc$List = new ArrayList();
            ArrayList<ReceiptPosition> ggxh$List = new ArrayList();
            ArrayList<ReceiptPosition> dw$List = new ArrayList();
            ArrayList<ReceiptPosition> sl$List = new ArrayList();
            ArrayList<ReceiptPosition> dj$List = new ArrayList();
            ArrayList<ReceiptPosition> je$List = new ArrayList();
            ArrayList<ReceiptPosition> slv$List = new ArrayList();
            ArrayList<ReceiptPosition> se$List = new ArrayList();
            ArrayList<ReceiptPosition> bz$List = new ArrayList();

            for(int i = addendumList.size() - 1; i >= 0; --i) {
                ReceiptPosition rpf = addendumList.get(i);
                String textf = rpf.getText().trim().replaceAll(" ", "");
                if (rpf.getPosY() < zjY$ - 2.0F && rpf.getPosY() > bzY$ - 5.0F && rpf.getPosLastX() > xh$R.getPosLastX() + 2.0F) {
                    bz$List.add(rpf);
                }

                if (rpf.getPosY() < xjY$ + 2.0F && rpf.getPosY() > zjY$ - 2.0F) {
                    if (rpf.getPosLastX() < sl$R.getPosX() && rpf.getPosX() > je$R.getPosX() - 10.0F) {
                        if (StringUtils.isBlank(xjje)) {
                            xjje = textf.replace("¥", "").replace("￥", "");
                        } else {
                            textf = textf.replace("¥", "").replace("￥", "");
                        }
                    }

                    if (rpf.getPosX() > je$R.getPosLastX()) {
                        if (StringUtils.isBlank(xjse)) {
                            xjse = textf.replace("¥", "").replace("￥", "");
                        } else {
                            zjse = textf.replace("¥", "").replace("￥", "");
                        }
                    }
                }

                if (rpf.getPosY() < xhY$ - 2.0F && rpf.getPosY() > xjY$ + 2.0F) {
                    rpf.setText(textf);
                    if (rpf.getPosLastX() < xh$R.getPosLastX() + 2.0F) {
                        xh$List.add(rpf);
                    } else if (rpf.getPosLastX() < ggxh$R.getPosX() - 8.0F) {
                        hwmc$List.add(rpf);
                    } else if (rpf.getPosLastX() < dw$R.getPosX() - 3.0F) {
                        ggxh$List.add(rpf);
                    } else if (rpf.getPosLastX() < sl$R.getPosX() - 10.0F) {
                        dw$List.add(rpf);
                    } else if (rpf.getPosLastX() < dj$R.getPosX() - 10.0F) {
                        sl$List.add(rpf);
                    } else if (rpf.getPosLastX() < je$R.getPosX() - 10.0F) {
                        dj$List.add(rpf);
                    } else if (rpf.getPosLastX() < slv$R.getPosX() - 2.0F) {
                        je$List.add(rpf);
                    } else if (rpf.getPosLastX() < se$R.getPosX() - 10.0F) {
                        slv$List.add(rpf);
                    } else {
                        se$List.add(rpf);
                    }
                }
            }

            SortUtil.sort(bz$List, new String[]{"posX"});
            Iterator var59 = bz$List.iterator();

            while(var59.hasNext()) {
                ReceiptPosition bzRp = (ReceiptPosition)var59.next();
                bzText.append(bzRp.getText());
            }

            json.put("xhqd_bz", bzText.toString());
            json.put("xhqd_xjje", xjje);
            json.put("xhqd_xjse", xjse);
            json.put("xhqd_zjje", text);
            json.put("xhqd_zjse", zjse);
            if (xh$List.size() > 0) {
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();

                for(int i = 0; i < xh$List.size(); ++i) {
                    ReceiptPosition xhRp = xh$List.get(i);
                    String xh = xhRp.getText();
                    StringBuilder hwmc = new StringBuilder();
                    String ggxh = "";
                    String dw = "";
                    String sl = "";
                    String dj = "";
                    String je = "";
                    String slv = "";
                    String se = "";
                    Iterator var42 = ggxh$List.iterator();

                    ReceiptPosition hwmcRp;
                    while(var42.hasNext()) {
                        hwmcRp = (ReceiptPosition)var42.next();
                        if (Math.abs(xhRp.getPosY() - hwmcRp.getPosY()) < 2.0F) {
                            ggxh = hwmcRp.getText();
                        }
                    }

                    var42 = dw$List.iterator();

                    while(var42.hasNext()) {
                        hwmcRp = (ReceiptPosition)var42.next();
                        if (Math.abs(xhRp.getPosY() - hwmcRp.getPosY()) < 2.0F) {
                            dw = hwmcRp.getText();
                        }
                    }

                    var42 = sl$List.iterator();

                    while(var42.hasNext()) {
                        hwmcRp = (ReceiptPosition)var42.next();
                        if (Math.abs(xhRp.getPosY() - hwmcRp.getPosY()) < 2.0F) {
                            sl = hwmcRp.getText();
                        }
                    }

                    var42 = dj$List.iterator();

                    while(var42.hasNext()) {
                        hwmcRp = (ReceiptPosition)var42.next();
                        if (Math.abs(xhRp.getPosY() - hwmcRp.getPosY()) < 2.0F) {
                            dj = hwmcRp.getText();
                        }
                    }

                    var42 = je$List.iterator();

                    while(var42.hasNext()) {
                        hwmcRp = (ReceiptPosition)var42.next();
                        if (Math.abs(xhRp.getPosY() - hwmcRp.getPosY()) < 2.0F) {
                            je = hwmcRp.getText();
                        }
                    }

                    var42 = slv$List.iterator();

                    while(var42.hasNext()) {
                        hwmcRp = (ReceiptPosition)var42.next();
                        if (Math.abs(xhRp.getPosY() - hwmcRp.getPosY()) < 2.0F) {
                            slv = hwmcRp.getText();
                        }
                    }

                    var42 = se$List.iterator();

                    while(var42.hasNext()) {
                        hwmcRp = (ReceiptPosition)var42.next();
                        if (Math.abs(xhRp.getPosY() - hwmcRp.getPosY()) < 2.0F) {
                            se = hwmcRp.getText();
                        }
                    }

                    if (i + 1 < xh$List.size()) {
                        hwmcRp = (ReceiptPosition)xh$List.get(i + 1);
                        Iterator var43 = hwmc$List.iterator();

                        while(var43.hasNext()) {
                            ReceiptPosition hwmcRp1 = (ReceiptPosition)var43.next();
                            if (hwmcRp1.getPosY() > hwmcRp1.getPosY() + 2.0F && hwmcRp1.getPosY() < xhRp.getPosY() + 2.0F) {
                                hwmc.append(hwmcRp1.getText());
                            }
                        }
                    } else {
                        var42 = hwmc$List.iterator();

                        while(var42.hasNext()) {
                            hwmcRp = (ReceiptPosition)var42.next();
                            if (hwmcRp.getPosY() < xhRp.getPosY() + 2.0F) {
                                hwmc.append(hwmcRp.getText());
                            }
                        }
                    }

                    jsonObject.put("xhqd_xh", xh);
                    jsonObject.put("xhqd_hwmc", hwmc.toString());
                    jsonObject.put("xhqd_ggxh", ggxh);
                    jsonObject.put("xhqd_dw", dw);
                    jsonObject.put("xhqd_sl", sl);
                    jsonObject.put("xhqd_dj", dj);
                    jsonObject.put("xhqd_je", je);
                    jsonObject.put("xhqd_slv", slv.replace("%", ""));
                    jsonObject.put("xhqd_se", se);
                    jsonArray.add(jsonObject);
                    jsonObject = new JSONObject();
                }

                json.put("xhqd_xq", jsonArray);
            }
        }

        return json;
    }

    private static void specificDW(List<ReceiptPosition> list) {
        jqbhR = new ReceiptPosition();
        hjR = new ReceiptPosition();
        jshjR = new ReceiptPosition();
        hwmcR = new ReceiptPosition();
        ggxhR = new ReceiptPosition();
        danweiR = new ReceiptPosition();
        shuliangR = new ReceiptPosition();
        danjiaR = new ReceiptPosition();
        jineR = new ReceiptPosition();
        shuilvR = new ReceiptPosition();
        shuieR = new ReceiptPosition();
        mmqR = new ReceiptPosition();
        bzR = new ReceiptPosition();
        List<ReceiptPosition> hjList = new ArrayList();
        List<ReceiptPosition> jshjList = new ArrayList();
        List<ReceiptPosition> hwmcList = new ArrayList();
        List<ReceiptPosition> ggxhList = new ArrayList();
        List<ReceiptPosition> danweiList = new ArrayList();
        List<ReceiptPosition> shuliangList = new ArrayList();
        List<ReceiptPosition> danjiaList = new ArrayList();
        List<ReceiptPosition> jineList = new ArrayList();
        List<ReceiptPosition> shuilvList = new ArrayList();
        List<ReceiptPosition> shuieList = new ArrayList();
        List<ReceiptPosition> jqbhList = new ArrayList();
        SortUtil.sort(list, new String[]{"posY"});
        Iterator var13 = list.iterator();

        ReceiptPosition item;
        while(var13.hasNext()) {
            item = (ReceiptPosition)var13.next();
            if (trim(StringUtil.removeSpace(item.getText())).startsWith("合计")) {
                hjList.add(item);
            }

            if (trim(StringUtil.removeSpace(item.getText())).startsWith("价税合计")) {
                jshjList.add(item);
            }

            if (trim(StringUtil.removeSpace(item.getText())).startsWith("机器编号")) {
                jqbhList.add(item);
            }

            if (trim(StringUtil.removeSpace(item.getText())).startsWith("(小写)")) {
                hjList.add(item);
            }
        }

        SortUtil.sort(jqbhList, new String[]{"posY"});
        jqbhR = jqbhList.get(jqbhList.size() - 1);
        reInspection(hjList, list, "合计", "合", "计");
        var13 = hjList.iterator();

        ReceiptPosition rp;
        while(var13.hasNext()) {
            item = (ReceiptPosition)var13.next();
            Iterator var15 = jshjList.iterator();

            while(var15.hasNext()) {
                rp = (ReceiptPosition)var15.next();
                if (Math.abs(item.getPosY() - rp.getPosY()) < 25.0F) {
                    hjR.setPosX(item.getPosX());
                    hjR.setPosY(item.getPosY());
                    hjR.setText("合计");
                    jshjR.setPosX(rp.getPosX());
                    jshjR.setPosY(rp.getPosY());
                    jshjR.setText("价税合计");
                }
            }
        }

        SortUtil.sort(list, new String[]{"posX"});
        List<List<ReceiptPosition>> baseList = new ArrayList();
        List<ReceiptPosition> indexList = new ArrayList();
        rp = null;

        for(int i = 1; i < list.size(); ++i) {
            ReceiptPosition itemf = (ReceiptPosition)list.get(i);
            float posX = itemf.getPosX();
            ReceiptPosition item1 = (ReceiptPosition)list.get(i - 1);
            float posX1 = item1.getPosX();
            indexList.add(item1);
            if (Math.abs(posX - posX1) > 2.0F) {
                baseList.add(indexList);
                indexList = new ArrayList();
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("货物或应税劳务、服")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("货物或应税劳务、服务名称");
                hwmcList.add(rp);
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("规格型号")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("规格型号");
                ggxhList.add(rp);
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("单位")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("单位");
                danweiList.add(rp);
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("数量")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("数量");
                shuliangList.add(rp);
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("单价")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("单价");
                danjiaList.add(rp);
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("金额")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("金额");
                jineList.add(rp);
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("税率")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("税率");
                shuilvList.add(rp);
            }

            if (trim(StringUtil.removeSpace(itemf.getText())).startsWith("税额")) {
                rp = new ReceiptPosition();
                rp.setPosX(itemf.getPosX());
                rp.setPosY(itemf.getPosY());
                rp.setText("税额");
                shuieList.add(rp);
            }
        }

        if (hwmcList.size() > 0) {
            hwmcR = hwmcList.get(hwmcList.size() - 1);
        }

        Iterator var24 = ggxhList.iterator();

        ReceiptPosition x;
        while(var24.hasNext()) {
            x = (ReceiptPosition)var24.next();
            if (Math.abs(hwmcR.getPosY() - x.getPosY()) < 2.0F) {
                ggxhR = x;
            }
        }

        reInspection(danweiList, list, "单位", "单", "位");
        var24 = danweiList.iterator();

        while(var24.hasNext()) {
            x = (ReceiptPosition)var24.next();
            if (Math.abs(hwmcR.getPosY() - x.getPosY()) < 2.0F) {
                danweiR = x;
            }
        }

        reInspection(shuliangList, list, "数量", "数", "量");
        var24 = shuliangList.iterator();

        while(var24.hasNext()) {
            x = (ReceiptPosition)var24.next();
            if (Math.abs(hwmcR.getPosY() - x.getPosY()) < 2.0F) {
                shuliangR = x;
            }
        }

        reInspection(danjiaList, list, "单价", "单", "价");
        var24 = danjiaList.iterator();

        while(var24.hasNext()) {
            x = (ReceiptPosition)var24.next();
            if (Math.abs(hwmcR.getPosY() - x.getPosY()) < 2.0F) {
                danjiaR = x;
            }
        }

        reInspection(jineList, list, "金额", "金", "额");
        var24 = jineList.iterator();

        while(var24.hasNext()) {
            x = (ReceiptPosition)var24.next();
            if (Math.abs(hwmcR.getPosY() - x.getPosY()) < 2.0F) {
                jineR = x;
            }
        }

        reInspection(shuilvList, list, "税率", "税", "率");
        var24 = shuilvList.iterator();

        while(var24.hasNext()) {
            x = (ReceiptPosition)var24.next();
            if (Math.abs(hwmcR.getPosY() - x.getPosY()) < 2.0F) {
                shuilvR = x;
            }
        }

        reInspection(shuieList, list, "税额", "税", "额");
        var24 = shuieList.iterator();

        while(var24.hasNext()) {
            x = (ReceiptPosition)var24.next();
            if (Math.abs(hwmcR.getPosY() - x.getPosY()) < 2.0F) {
                shuieR = x;
            }
        }

        var24 = baseList.iterator();

        while(true) {
            String t;
            List base;
            do {
                if (!var24.hasNext()) {
                    return;
                }

                base = (List)var24.next();
                SortUtil.sort(base, new String[]{"posY"});
                t = "";

                for(int i = base.size() - 1; i >= 0; --i) {
                    ReceiptPosition itemf = (ReceiptPosition)base.get(i);
                    t = String.valueOf(t) + itemf.getText();
                }
            } while(!trim(StringUtil.removeSpace(t)).startsWith("密码区") && !trim(StringUtil.removeSpace(t)).equals("密码区备注"));

            mmqR.setText("密码区");
            mmqR.setPosX(((ReceiptPosition)base.get(0)).getPosX());
            mmqR.setPosEndX(((ReceiptPosition)base.get(0)).getPosLastX());
            mmqR.setPosY(((ReceiptPosition)base.get(base.size() - 1)).getPosY());
            bzR.setText("备注");
            bzR.setPosX(((ReceiptPosition)base.get(0)).getPosX());
            bzR.setPosEndX(((ReceiptPosition)base.get(0)).getPosLastX());
            bzR.setPosEndY(((ReceiptPosition)base.get(0)).getPosLastY());
        }
    }

    private static void reInspection(List<ReceiptPosition> cjList, List<ReceiptPosition> list, String cj, String cj_c, String cj_j) {
    }

    private static JSONObject reorganizationRegulation(List<ReceiptPosition> list) {
        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("fplx", 10);
        String fpdm = "";
        String fphm = "";
        String kprq = "";
        String jym = "";
        String spfmc = "";
        String spfsbh = "";
        String spfdzdh = "";
        String spfyhzh = "";
        String kpfmc = "";
        String kpfsbh = "";
        String kpfdzdh = "";
        String kpfyhzh = "";
        String hjje = "";
        String hjse = "";
        String kpje = "";
        String slv = "";
        String hwmc = "";
        String jqbh = "";
        String skr = "";
        String fh = "";
        String kpr = "";
        String xsf = "";
        List<ReceiptPosition> sfkList = new ArrayList();
        List<List<ReceiptPosition>> baseList = new ArrayList();
        List<ReceiptPosition> lineList = new ArrayList();
        SortUtil.sort(list, new String[]{"posY"});
        List<ReceiptPosition> indexList = new ArrayList();

        ReceiptPosition sfk;
        for(int i = list.size() - 1; i > 0; --i) {
            sfk = list.get(i);
            float posY = sfk.getPosY();
            ReceiptPosition item1 = list.get(i - 1);
            float posY1 = item1.getPosY();
            indexList.add(sfk);
            if (Math.abs(posY - posY1) > 2.0F) {
                baseList.add(indexList);
                indexList = new ArrayList();
            }

            if (posY1 < bzR.getPosEndY() - 10.0F) {
                sfkList.add(item1);
            }
        }

        SortUtil.sort(sfkList, new String[]{"posX"});
        String sfkText = "";

        for(Iterator var53 = sfkList.iterator(); var53.hasNext(); sfkText = String.valueOf(sfkText) + sfk.getText()) {
            sfk = (ReceiptPosition)var53.next();
        }

        String regex = "收款人|复核|开票人|销售方";
        Pattern pattern = Pattern.compile(regex);
        String[] splitStrs = pattern.split(sfkText.trim());
        if (splitStrs.length > 4) {
            skr = splitStrs[1];
            fh = splitStrs[2];
            kpr = splitStrs[3];
            xsf = splitStrs[4];
        }

        Iterator var33 = baseList.iterator();

        label336:
        while(var33.hasNext()) {
            List<ReceiptPosition> itemList = (List)var33.next();
            SortUtil.sort(itemList, new String[]{"posX"});
            ReceiptPosition r = new ReceiptPosition();
            String text = "";
            Iterator var37 = itemList.iterator();

            while(true) {
                ReceiptPosition item;
                do {
                    do {
                        do {
                            if (!var37.hasNext()) {
                                text = StringUtil.removeSpace(text);
                                r.setText(text);
                                r.setPosY(((ReceiptPosition)itemList.get(0)).getPosY());
                                r.setPosEndY(((ReceiptPosition)itemList.get(itemList.size() - 1)).getPosY());
                                lineList.add(r);
                                continue label336;
                            }

                            item = (ReceiptPosition)var37.next();
                        } while(item.getPosY() < jqbhR.getPosY() - 5.0F && item.getPosY() > hwmcR.getPosY() && item.getPosX() > mmqR.getPosX());
                    } while(item.getPosY() < jshjR.getPosY() && item.getPosX() > bzR.getPosX());
                } while(item.getPosY() < hwmcR.getPosY() && item.getPosY() > hjR.getPosY() && item.getPosX() > ggxhR.getPosX());

                text = String.valueOf(text) + item.getText();
            }
        }

        List<String> hwmcL = new ArrayList();
        List<String> slL = new ArrayList();
        List<String> djL = new ArrayList();
        List<String> dxjeL = new ArrayList();
        List<String> slvL = new ArrayList();
        List<String> dxseL = new ArrayList();
        List<Float> agioYList = new ArrayList();
        Iterator var40 = list.iterator();

        ReceiptPosition rec;
        String text;
        while(var40.hasNext()) {
            rec = (ReceiptPosition)var40.next();
            if (rec.getPosY() > hjR.getPosY() - 5.0F && rec.getPosY() < jineR.getPosY() && rec.getPosX() > jineR.getPosX() - 20.0F && rec.getPosX() < shuilvR.getPosX()) {
                text = StringUtil.removeSpace(rec.getText());
                if (StringUtils.isNotBlank(text) && StringUtils.isBlank(hjje)) {
                    hjje = trim(text.replaceAll("\\(小写\\)", "").replaceAll("¥", "").replaceAll("￥", ""));
                }
            }

            if (rec.getPosY() > hjR.getPosY() && rec.getPosY() < shuilvR.getPosY() && rec.getPosX() > shuilvR.getPosX() - 5.0F && rec.getPosX() < shuilvR.getPosX() + 20.0F) {
                text = StringUtil.removeSpace(rec.getText());
                if (StringUtils.isNotBlank(text)) {
                    slv = text;
                }

                agioYList.add(rec.getPosY());
                slvL.add(StringUtil.removeSpace(rec.getText()));
            }

            if (rec.getPosY() > hjR.getPosY() - 5.0F && rec.getPosY() < shuieR.getPosY() && rec.getPosX() > shuieR.getPosX() - 20.0F) {
                text = StringUtil.removeSpace(rec.getText());
                if (StringUtils.isNotBlank(text) && StringUtils.isBlank(hjse)) {
                    hjse = trim(text.replaceAll("¥", "").replaceAll("￥", ""));
                }
            }
        }

        String rqX;
        for(int i = 0; i < agioYList.size(); ++i) {
            Float indexY = (Float)agioYList.get(i);
            Float indexY0 = indexY;
            if (i > 0) {
                indexY0 = (Float)agioYList.get(i - 1);
            }

            String dxhwmc$ = "";
            String sl$ = "";
            String dj$ = "";
            String dxje$ = "";
            rqX = "";

            for(int j = list.size() - 1; j > 0; --j) {
                float posY = (list.get(j)).getPosY();
                float posX = (list.get(j)).getPosX();
                String textf = StringUtil.removeSpace((list.get(j)).getText());
                if (posY < agioYList.get(agioYList.size() - 1) + 2.0F && posY > hjR.getPosY() + 2.0F) {
                    if (posX < ggxhR.getPosX() - 5.0F) {
                        if (i == 0) {
                            if (posY < indexY + 2.0F && posY > hjR.getPosY() + 2.0F) {
                                dxhwmc$ = String.valueOf(dxhwmc$) + textf;
                            }
                        } else if (posY > indexY0 && posY < indexY + 2.0F) {
                            dxhwmc$ = String.valueOf(dxhwmc$) + textf;
                        }
                    }

                    if (Math.abs(posY - indexY) < 2.0F && posX < danjiaR.getPosX() - 20.0F && posX > shuliangR.getPosX() - 10.0F) {
                        sl$ = textf;
                    }

                    if (Math.abs(posY - indexY) < 2.0F && posX < jineR.getPosX() - 20.0F && posX > danjiaR.getPosX() - 20.0F) {
                        dj$ = textf;
                    }

                    if (Math.abs(posY - indexY) < 2.0F && posX < shuilvR.getPosX() - 5.0F && posX > jineR.getPosX() - 20.0F) {
                        dxje$ = textf;
                    }

                    if (Math.abs(posY - indexY) < 2.0F && posX > shuieR.getPosX() - 20.0F) {
                        rqX = textf;
                    }
                }
            }

            hwmcL.add(dxhwmc$);
            slL.add(sl$);
            djL.add(dj$);
            dxjeL.add(dxje$);
            dxseL.add(rqX);
        }

        var40 = lineList.iterator();

        while(var40.hasNext()) {
            rec = (ReceiptPosition)var40.next();
            text = rec.getText();
            int kpfmcIndex;
            int kpfsbhIndex;
            int kpfdzdhIndex;
            int kpfyhzhIndex;
            if (rec.getPosY() > hwmcR.getPosY()) {
                kpfmcIndex = rec.getText().indexOf("机器编号");
                if (kpfmcIndex > -1) {
                    jqbh = text.substring(kpfmcIndex + 5, kpfmcIndex + 17);
                }

                kpfsbhIndex = rec.getText().indexOf("发票代码");
                if (kpfsbhIndex > -1) {
                    fpdm = text.substring(kpfsbhIndex + 5, text.length());
                }

                kpfdzdhIndex = rec.getText().indexOf("发票号码");
                if (kpfdzdhIndex > -1) {
                    fphm = text.substring(kpfdzdhIndex + 5, text.length());
                }

                kpfyhzhIndex = rec.getText().indexOf("开票日期");
                if (kpfyhzhIndex > -1) {
                    rqX = text.substring(kpfyhzhIndex + 5, text.length());
                    String rq = rqX.replaceAll("年", "").replaceAll("月", "").replaceAll("日", "");
                    kprq = String.valueOf(rq.substring(0, 4)) + "-" + rq.substring(4, 6) + "-" + rq.substring(6, 8);
                }

                int jymIndex = rec.getText().indexOf("校验码");
                if (jymIndex > -1) {
                    jym = text.substring(jymIndex + 4, text.length());
                }
            }

            if (rec.getPosY() < hwmcR.getPosY() && rec.getPosY() > hjR.getPosY() && rec.getPosX() < ggxhR.getPosX()) {
                if (agioYList.size() > 1 && rec.getPosY() <= (Float)agioYList.get(agioYList.size() - 2)) {
                    continue;
                }

                if (text.contains("名") && text.contains("称")) {
                    kpfmc = text.substring(text.indexOf(":") + 1, text.length());
                } else if (text.contains("纳税人识别号")) {
                    kpfsbh = text.substring(text.indexOf(":") + 1, text.length());
                } else if (text.contains("开户行及账号")) {
                    kpfyhzh = text.substring(text.indexOf(":") + 1, text.length());
                } else if (text.contains("地址、电话")) {
                    kpfdzdh = text.substring(text.indexOf(":") + 1, text.length());
                } else {
                    hwmc = String.valueOf(hwmc) + text;
                }
            }

            if (rec.getPosY() > hwmcR.getPosY() && rec.getPosY() < jqbhR.getPosY()) {
                kpfmcIndex = rec.getText().indexOf("名称");
                if (kpfmcIndex > -1) {
                    spfmc = text.substring(kpfmcIndex + 3, text.length());
                }

                kpfsbhIndex = rec.getText().indexOf("纳税人识别号");
                if (kpfsbhIndex > -1) {
                    spfsbh = text.substring(kpfsbhIndex + 7, text.length());
                }

                kpfdzdhIndex = rec.getText().indexOf("地址、电话");
                if (kpfdzdhIndex > -1) {
                    spfdzdh = text.substring(kpfdzdhIndex + 6, text.length());
                }

                kpfyhzhIndex = rec.getText().indexOf("开户行及账号");
                if (kpfyhzhIndex > -1) {
                    spfyhzh = text.substring(kpfyhzhIndex + 7, text.length());
                }
            }

            if (rec.getPosY() < jshjR.getPosY()) {
                kpfmcIndex = rec.getText().indexOf("名称");
                if (kpfmcIndex > -1) {
                    kpfmc = text.substring(kpfmcIndex + 3, text.length());
                }

                kpfsbhIndex = rec.getText().indexOf("纳税人识别号");
                if (kpfsbhIndex > -1) {
                    kpfsbh = text.substring(kpfsbhIndex + 7, text.length());
                }

                kpfdzdhIndex = rec.getText().indexOf("地址、电话");
                if (kpfdzdhIndex > -1) {
                    kpfdzdh = text.substring(kpfdzdhIndex + 6, text.length());
                }

                kpfyhzhIndex = rec.getText().indexOf("开户行及账号");
                if (kpfyhzhIndex > -1) {
                    kpfyhzh = text.substring(kpfyhzhIndex + 7, text.length());
                }
            }

            if (Math.abs(rec.getPosY() - jshjR.getPosY()) < 2.0F) {
                kpje = text.split("小写")[1].replaceAll("¥", "").replaceAll("￥", "");
                kpje = kpje.substring(1, kpje.length());
            }
        }

        if (!NumberValidationUtils.isRealNumber(kpje)) {
            var40 = list.iterator();

            while(var40.hasNext()) {
                rec = (ReceiptPosition)var40.next();
                if (Math.abs(rec.getPosY() - jshjR.getPosY()) < 2.0F) {
                    kpje = String.valueOf(kpje) + rec.getText();
                }
            }
        }

        if (!NumberValidationUtils.isRealNumber(kpje)) {
            kpje = extractM(kpje);
        }

        jsonObject.put("invoiceCode", trim(fpdm));
        jsonObject.put("invoiceNo", trim(fphm));
        Date billingDate;
        try {
            billingDate = DateUtils.parseDate(trim(kprq),"yyyy-MM-dd");
        } catch (ParseException e) {
            billingDate = null;
        }
        jsonObject.put("billingDate", billingDate);
//        jsonObject.put("jym", trim(jym));
//        jsonObject.put("spfmc", trim(spfmc));
//        jsonObject.put("spfsbh", trim(spfsbh));
//        jsonObject.put("spfdzdh", trim(spfdzdh));
//        jsonObject.put("spfyhzh", trim(spfyhzh));
//        jsonObject.put("kpfmc", trim(kpfmc));
//        jsonObject.put("kpfsbh", trim(kpfsbh));
//        jsonObject.put("kpfdzdh", trim(kpfdzdh));
//        jsonObject.put("kpfyhzh", trim(kpfyhzh));
        BigDecimal amount;
        try {
            amount = new BigDecimal(trim(hjje)).add(new BigDecimal(trim(hjse)));
        } catch (Exception e) {
            amount = null;
        }
        jsonObject.put("amount", amount);
        slv = trim(slv);
        slv = slv.replaceAll("%", "");
        Collections.reverse(hwmcL);
        Collections.reverse(slL);
        Collections.reverse(djL);
        Collections.reverse(dxjeL);
        Collections.reverse(slvL);
        Collections.reverse(dxseL);
        return jsonObject;
    }

    private static String extractM(String parm) {
        Matcher m = NUMBER_PATTERN.matcher(parm);

        String str;
        for(str = ""; m.find(); str = String.valueOf(str) + m.group()) {

        }
        return str;
    }


}


package pdf;


import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import pdf.bean.ReceiptPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author xiaozj
 */
public class PdfTextStripperNew extends PDFTextStripper {
    private List<ReceiptPosition> posList = new ArrayList<>();

    public PdfTextStripperNew() throws IOException {
    }

    public List<ReceiptPosition> getPosList() {
        return this.posList;
    }

    @Override
    protected void processTextPosition(TextPosition pos) {
        float posEndX = pos.getEndX();
        float posEndY = pos.getEndY();
        float posX = pos.getX();
        String text = pos.getUnicode();
        boolean hasData = false;
        Iterator var8 = this.posList.iterator();

        while(true) {
            ReceiptPosition item;
            String textItem;
            do {
                do {
                    do {
                        do {
                            do {
                                do {
                                    do {
                                        if (!var8.hasNext()) {
                                            if (!hasData && StringUtils.isNotBlank(text)) {
                                                item = new ReceiptPosition();
                                                item.setText(text);
                                                item.setPosX(posX);
                                                item.setPosY(posEndY);
                                                item.setPosLastX(posEndX);
                                                item.setPosLastY(posEndY);
                                                this.posList.add(item);
                                            }

                                            return;
                                        }

                                        item = (ReceiptPosition)var8.next();
                                    } while(Math.abs(item.getPosLastY() - posEndY) >= 1.0F);
                                } while(Math.abs(item.getPosLastX() - posX) >= 1.0F && Math.abs(item.getPosLastX() - posEndX) >= 1.0F);

                                textItem = item.getText();
                            } while(textItem.endsWith(":"));
                        } while(textItem.indexOf("����") > -1);
                    } while(textItem.indexOf("(����)") > -1);
                } while(textItem.indexOf("��������") > -1);
            } while(textItem.indexOf("    ") > -1 && textItem.indexOf("??") == -1);

            if (Math.abs(item.getPosLastX() - posX) < 1.0F) {
                textItem = String.valueOf(textItem) + text;
                item.setPosLastX(posEndX);
            } else if (Math.abs(item.getPosLastX() - posEndX) < 1.0F) {
                textItem = String.valueOf(text) + textItem;
                item.setPosLastX(posX);
            }

            item.setText(textItem);
            hasData = true;
        }
    }
}


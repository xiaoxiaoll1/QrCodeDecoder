package pdf.bean;

import lombok.Data;

/**
 * PDF实体类
 * @author xiaozj
 */
@Data
public class ReceiptPosition {
    private float posX = 0.0F;
    private float posY = 0.0F;
    private float posLastX = 0.0F;
    private float posEndX = 0.0F;
    private float posLastY = 0.0F;
    private float posEndY = 0.0F;
    private String text;

    @Override
    public String toString() {
        return "ReceiptPosition [posEndX=" + this.posEndX + ", posEndY=" + this.posEndY + ", posLastX=" + this.posLastX + ", posLastY=" + this.posLastY + ", posX=" + this.posX + ", posY=" + this.posY + ", text=" + this.text + "]";
    }
}


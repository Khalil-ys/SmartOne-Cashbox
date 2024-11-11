package az.smartonecashbox.smartCassa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    private String itemId;
    private String itemName;
    private int itemAmount;
    private int itemQty;
    private int discount;
    private int itemAttr;
    private String itemQRCode;
    private List<ItemTaxe> itemTaxes;
}

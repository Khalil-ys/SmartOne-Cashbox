package az.smartonecashbox.smartCassa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemTaxe {

    private String taxName;
    private double taxRate;
    private double taxAmount;
}

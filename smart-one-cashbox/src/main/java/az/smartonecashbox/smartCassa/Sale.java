package az.smartonecashbox.smartCassa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sale {

    private String docNumber;
    private String docTime;
    private String employeeName;
    private String currency;
    private int amount;
    private String departmentCode;
    private String departmentName;
    private List<Item> items;
    private Payment payments;
    private String fiscalID;
    private String wsName;
    private String creditContract;
    private String prepayDocID;
    private String prepayDocNum;
    private String printFooter;
    private String rrn;
    private String checkNum;
    private Integer originAmount;
}

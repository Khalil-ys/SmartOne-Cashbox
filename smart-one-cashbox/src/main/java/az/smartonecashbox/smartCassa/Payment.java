package az.smartonecashbox.smartCassa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private int cashAmount;
    private int cashlessAmount;
    private String paymentType;
}

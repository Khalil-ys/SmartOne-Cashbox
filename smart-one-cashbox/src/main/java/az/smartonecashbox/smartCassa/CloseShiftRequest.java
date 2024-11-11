package az.smartonecashbox.smartCassa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloseShiftRequest {

    private String projectName;
    private String namespace;
    private String employeeName;
}

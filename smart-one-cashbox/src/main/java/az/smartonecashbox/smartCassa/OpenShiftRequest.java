package az.smartonecashbox.smartCassa;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level=AccessLevel.PRIVATE)
public class OpenShiftRequest {
    String employeeName;
    String pinCode;
}

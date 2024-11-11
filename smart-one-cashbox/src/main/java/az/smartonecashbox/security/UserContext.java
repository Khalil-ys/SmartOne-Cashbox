package az.smartonecashbox.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope
public class UserContext {

    private long id;
    private String username;
    private String session;
    private long branchId;
    private long companyId;
    private long userRoleId;
    private String userRole;
    private Long employeeId;
    private String transactionId;
    private String ipAddress;
    private String url;
}

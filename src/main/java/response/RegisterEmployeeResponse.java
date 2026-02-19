package response;
import lombok.*;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RegisterEmployeeResponse {
    public String message;
    public String status;
    public String errorMessage;
    public String jwt;
    public int employeeId;
}

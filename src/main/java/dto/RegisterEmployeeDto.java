package dto;
import lombok.*;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RegisterEmployeeDto {
//    it will be manager
    public int factoryId;
//    this will be his manager
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String role;
}

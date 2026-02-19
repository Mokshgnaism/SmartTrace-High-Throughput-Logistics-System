package EntryPoint.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GenerateAndSendImmediate {
    public int noOfPallets;
    public int cartonsPerPallet;
    public int unitsPerCarton;
    public String companyPrefix;
    public String factoryId;
    public String employeeId;
}

package EntryPoint.model;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "employee")
@Getter
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer employeeId;

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private String role;

    @ManyToOne
    @JoinColumn(name = "factory_id")
    private Factory factory;

    public Employee(String firstName, String lastName, String email, String password, String role, Factory factory) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.factory = factory;
    }


    public Employee() {
    }
}

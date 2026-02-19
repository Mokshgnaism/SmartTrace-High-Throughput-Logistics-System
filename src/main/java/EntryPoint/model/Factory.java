package EntryPoint.model;

import jakarta.persistence.*;

@Entity
public class Factory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String companyName;

    private String location;

    @OneToOne
    private Employee manager_id;

}

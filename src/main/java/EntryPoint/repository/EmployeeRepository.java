package EntryPoint.repository;

import EntryPoint.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    @Query("SELECT e.employeeId FROM Employee e where e.email=?1")
    Optional<Integer> findIdByEmail(String email);

    Optional<Employee> findEmployeeByEmail(String email);
}

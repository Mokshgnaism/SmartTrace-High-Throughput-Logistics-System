package controller;

import dto.LoginRequestDto;
import dto.RegisterEmployeeDto;
import model.Employee;
import model.Factory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import repository.EmployeeRepository;
import repository.FactoryRepository;
import response.LoginResponse;
import response.RegisterEmployeeResponse;
import util.JwtUtil;

import java.util.Optional;
@RestController
@RequestMapping("/auth")

public class Auth {
    private EmployeeRepository employeeRepository;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private FactoryRepository factoryRepository;
//    injection
    public Auth(EmployeeRepository employeeRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, FactoryRepository factoryRepository) {
        this.employeeRepository = employeeRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.factoryRepository = factoryRepository;
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterEmployeeDto register) {
        String email = register.getEmail();
        String firstName = register.getFirstName();
        String lastName = register.getLastName();
        String Password =  register.getPassword();
        String role = register.getRole();
        String HashedPassword = passwordEncoder.encode(Password);
        Optional<Factory> factory = factoryRepository.findById(register.getFactoryId());
        if(factory.isPresent()) {
            Employee employee = new Employee(firstName,lastName,email,HashedPassword,role,factory.get());
            employeeRepository.save(employee);
            Optional<Integer> userId = employeeRepository.findIdByEmail(email);
            if(userId.isEmpty()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email not found -> internal server error");
            }
            String jwtToken = jwtUtil.generateJWT(userId.get().toString(),role);
            return new ResponseEntity<>(new RegisterEmployeeResponse("success","success",null,jwtToken, userId.get()), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new RegisterEmployeeResponse("factoryNotfound","failed",null,null,-1), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?>login(@RequestBody LoginRequestDto loginRequestDto) {
        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();
        Optional<Employee> existingEmp = employeeRepository.findEmployeeByEmail(email);
        if(existingEmp.isEmpty()){
            return new ResponseEntity<>(new LoginResponse(null,"emailNotfound","NOT_FOUND_EMAIL"),HttpStatus.NOT_FOUND);
        }
        if(!passwordEncoder.matches(password,loginRequestDto.getPassword())){
            return new ResponseEntity<>(new LoginResponse(null,"passwordMismatch",null), HttpStatus.UNAUTHORIZED);
        }
        String jwtToken = jwtUtil.generateJWT(existingEmp.get().getEmployeeId().toString(),existingEmp.get().getRole());
        return new ResponseEntity<>(new LoginResponse(jwtToken,"SUCCESS","NULL"),HttpStatus.OK);
    }
}

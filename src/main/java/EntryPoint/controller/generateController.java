package EntryPoint.controller;

import EntryPoint.dbService.DBfetch;
import EntryPoint.dto.GenerateRequest;
import EntryPoint.model.Carton;
import EntryPoint.model.Employee;
import EntryPoint.model.Pallet;
import EntryPoint.model.Unit;
import EntryPoint.repository.EmployeeRepository;
import EntryPoint.response.GenerateRequestResponse;
import EntryPoint.response.SendRequestResponse;
import EntryPoint.util.JwtUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import EntryPoint.generator.LabelGenerator;
import EntryPoint.service.GeneratorService;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.print.attribute.standard.ReferenceUriSchemesSupported.HTTP;

@RestController
@RequestMapping("/products")
public class generateController {
    private final GeneratorService generatorService;
    private static ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final SecureRandom random = new SecureRandom();
    private DBfetch dbfetch;
    private DataSource ds;

    public generateController(GeneratorService generatorService, DBfetch dbfetch, DataSource ds) {
        this.generatorService = generatorService;
        this.dbfetch = dbfetch;
        this.ds = ds;
    }

// async version of the api . will be implementing later . dont have time now
// we can use the Redis to store the jobs . and then we can do it but for now maybe just make it syncronous .
//    parital implementation only
    @PreAuthorize("hasAuthority('MANAGER')")
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest generateRequest) {
        byte[] bytes = new byte[8]; // 8 bytes = short but good randomness
        random.nextBytes(bytes);

        String jobId = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        executor.submit(() -> {
            try {
                generatorService.Generate(
                        generateRequest.getNoOfPallets(),
                        generateRequest.cartonsPerPallet,
                        generateRequest.getUnitsPerCarton(),
                        generateRequest.getCompanyPrefix(),
                        generateRequest.getFactoryId(),
                        generateRequest.getEmployeeId(),
                        jobId
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return new ResponseEntity<>(new GenerateRequestResponse("sorry"), HttpStatus.CREATED);
    }


    @PreAuthorize("hasAuthority('EMPLOYEE')")
    @PostMapping("/generateImmediate")
    public ResponseEntity<?> generateAndSend(@RequestBody GenerateRequest generateRequest) {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);

        String jobId = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        CompletableFuture<Void> cf1 = CompletableFuture.runAsync(() -> {
            try {
                generatorService.Generate(
                        generateRequest.getNoOfPallets(),
                        generateRequest.cartonsPerPallet,
                        generateRequest.getUnitsPerCarton(),
                        generateRequest.getCompanyPrefix(),
                        generateRequest.getFactoryId(),
                        generateRequest.getEmployeeId(),
                        jobId
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },executor);
        cf1.join();
        List<Unit> ls = new ArrayList<>();
        List<Carton> lCartons = new ArrayList<>();
        List<Pallet> lPallets = new ArrayList<>();

        byte[] jobIdBytes = jobId.getBytes(StandardCharsets.UTF_8);

        CompletableFuture<Integer> unitsCF = CompletableFuture.supplyAsync(()->{
            try {
                dbfetch.getUnitsByJobId(jobIdBytes,ls);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return -1;
        },executor);
        CompletableFuture<Integer>cartonsCF = CompletableFuture.supplyAsync(()->{
            try {
                dbfetch.getCartonsByJobId(jobIdBytes,lCartons);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return -1;
        },executor);
        CompletableFuture<Integer>palletsCF = CompletableFuture.supplyAsync(()->{
            try {
                dbfetch.getPalletsByJobId(jobIdBytes,lPallets);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return -1;
        },executor);
// wait for all
        CompletableFuture.allOf(unitsCF, cartonsCF, palletsCF).join();
        return new ResponseEntity<>(new SendRequestResponse(ls,lCartons,lPallets), HttpStatus.OK);
    }
}

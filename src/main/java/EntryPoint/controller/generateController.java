package EntryPoint.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import EntryPoint.generator.LabelGenerator;
import EntryPoint.service.GeneratorService;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }


    public HikariDataSource ds ;
    public generateController(GeneratorService generatorService) {
        this.generatorService = generatorService;
        HikariConfig config = new HikariConfig();
        config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/testdb");
        config.setUsername("postgres");
        config.setPassword("Mokshgna@123");
        config.setMaximumPoolSize(16);
        config.setAutoCommit(false);
        ds = new HikariDataSource(config);
    }

// async version of the api . will be implementing later . dont have time now
// we can use the Redis to store the jobs . and then we can do it but for now maybe just make it syncronous .
//    parital implementation only
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
        });

        List<Unit> ls = new ArrayList<>();
        List<Carton> lCartons = new ArrayList<>();
        List<Pallet> lPallets = new ArrayList<>();

        byte[] jobIdBytes = jobId.getBytes(StandardCharsets.UTF_8);

        CompletableFuture<Void> unitsCF = CompletableFuture.runAsync(() -> {

            try (var conn = ds.getConnection()) {

                PreparedStatement ps = conn.prepareStatement(
                        "SELECT serial_id,parent_carton_id,hash,hash_prefix FROM units WHERE job_id = ?"
                );

                ps.setBytes(1, jobIdBytes);

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {

                    String serialId = toHex(rs.getBytes("serial_id"));
                    String parentCarton = toHex(rs.getBytes("parent_carton_id"));
                    String hash = toHex(rs.getBytes("hash"));
                    String hashPrefix = toHex(rs.getBytes("hash_prefix"));

                    ls.add(new Unit(serialId, hash, hashPrefix, parentCarton));
                }

            } catch (Exception e) {
                throw new RuntimeException("error fetching units", e);
            }

        }, executor);



        CompletableFuture<Void> cartonsCF = CompletableFuture.runAsync(() -> {

            try (var conn = ds.getConnection()) {

                PreparedStatement ps = conn.prepareStatement(
                        "SELECT serial_id,parent_pallet_id,hash,hash_prefix FROM cartons WHERE job_id = ?"
                );

                ps.setBytes(1, jobIdBytes);

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {

                    String serialId = toHex(rs.getBytes("serial_id"));
                    String parentPallet = toHex(rs.getBytes("parent_pallet_id"));
                    String hash = toHex(rs.getBytes("hash"));
                    String hashPrefix = toHex(rs.getBytes("hash_prefix"));

                    lCartons.add(new Carton(serialId, hash, hashPrefix, parentPallet));
                }

            } catch (Exception e) {
                throw new RuntimeException("error fetching cartons", e);
            }

        }, executor);



        CompletableFuture<Void> palletsCF = CompletableFuture.runAsync(() -> {

            try (var conn = ds.getConnection()) {

                PreparedStatement ps = conn.prepareStatement(
                        "SELECT ssic,hash,hash_prefix FROM pallets WHERE job_id = ?"
                );

                ps.setBytes(1, jobIdBytes);

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {

                    String serialId = toHex(rs.getBytes("ssic"));
                    String hash = toHex(rs.getBytes("hash"));
                    String hashPrefix = toHex(rs.getBytes("hash_prefix"));

                    lPallets.add(new Pallet(serialId, hash, hashPrefix, null));
                }

            } catch (Exception e) {
                throw new RuntimeException("error fetching pallets", e);
            }


        }, executor);
// wait for all
        CompletableFuture.allOf(unitsCF, cartonsCF, palletsCF).join();
        return new ResponseEntity<>(new SendRequestResponse(ls,lCartons,lPallets), HttpStatus.OK);
    }
}

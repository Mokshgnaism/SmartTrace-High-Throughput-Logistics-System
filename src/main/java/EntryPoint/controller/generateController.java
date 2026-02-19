package EntryPoint.controller;

import EntryPoint.dto.GenerateRequest;
import EntryPoint.model.Employee;
import EntryPoint.repository.EmployeeRepository;
import EntryPoint.response.GenerateRequestResponse;
import EntryPoint.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import EntryPoint.generator.LabelGenerator;
import EntryPoint.service.GeneratorService;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.print.attribute.standard.ReferenceUriSchemesSupported.HTTP;

@RestController
@RequestMapping("/products")
public class generateController {
    private final GeneratorService generatorService;
    private static ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public generateController(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }
// async version of the api . will be implementing later . dont have time now
// we can use the Redis to store the jobs . and then we can do it but for now maybe just make it syncronous .
//    parital implementation only
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest generateRequest) {
        int jobId = -1;
        executor.submit(() -> {
            try {
                generatorService.Generate(
                        generateRequest.getNoOfPallets(),
                        generateRequest.cartonsPerPallet,
                        generateRequest.getUnitsPerCarton(),
                        generateRequest.getCompanyPrefix(),
                        generateRequest.getFactoryId(),
                        generateRequest.getEmployeeId()
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
//        for now lets just bring all of them si

    }
}

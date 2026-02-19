package EntryPoint.controller;

import EntryPoint.dto.scanDTO;
import EntryPoint.model.Carton;
import EntryPoint.model.Pallet;
import EntryPoint.model.Unit;
import EntryPoint.response.scanResponse;
import EntryPoint.service.ProductVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
@RequestMapping("/verify")
public class ProductVerification {

    private final ProductVerificationService service;

    public ProductVerification(ProductVerificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> verify(@RequestBody scanDTO dto) throws SQLException {

        String hash = dto.getName();

        // try unit first
        Unit u = service.verifyUnit(hash);

        if(u != null) {
            return new ResponseEntity<>(
                    new scanResponse("success","authentic unit","good item"),
                    HttpStatus.OK);
        }

        // try carton
        Carton c = service.verifyCarton(hash);

        if(c != null) {
            return new ResponseEntity<>(
                    new scanResponse("success","authentic carton","good item"),
                    HttpStatus.OK);
        }

        // try pallet
        Pallet p = service.verifyPallet(hash);

        if(p != null) {
            return new ResponseEntity<>(
                    new scanResponse("success","authentic pallet","good item"),
                    HttpStatus.OK);
        }

        // nothing matched
        return new ResponseEntity<>(
                new scanResponse("invalid","product not found or already verified","fake item"),
                HttpStatus.NOT_FOUND);
    }
}

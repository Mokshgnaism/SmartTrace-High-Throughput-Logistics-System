package EntryPoint.controller;

import EntryPoint.dbService.DBfetch;
import EntryPoint.dto.scanDTO;
import EntryPoint.model.Carton;
import EntryPoint.model.Pallet;
import EntryPoint.model.Unit;
import EntryPoint.response.scanResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

@RestController
@PreAuthorize("hasAuthority('EMPLOYEE')")
/// i want to optimise this to using the simple query of the join query without 3 queries per function but for now let it be this  .
///     blocking the http thread is not great, but this api needs to be synchronous cant do anything .
@RequestMapping("/scan")
public class ProductScanning {
    private DBfetch dbfetch;
    public ProductScanning(DBfetch dbfetch) {
        this.dbfetch = dbfetch;
    }
    @PostMapping("/unit/prefix")
    public ResponseEntity<?> scanUnitWithHashPrefix(@RequestBody scanDTO scanDTO) throws SQLException {
     String HashPrefix = scanDTO.getName();
     Unit u = dbfetch.getUnitByHashPrefix(HashPrefix);


     if(u==null){
         return new ResponseEntity<>(new scanResponse("bad Request","unit not found","fake item"),HttpStatus.NOT_FOUND);
     }
     Carton p = dbfetch.getCartonById(u.getParentCartonId());

     if(!dbfetch.isAlreadyScannedCarton(u.getParentCartonId())) {
         return new ResponseEntity<>(new scanResponse("bad Request","unit Scanned before Parent Carton","fake item"),HttpStatus.NOT_FOUND);
     }

     boolean isAlreadyScanned = dbfetch.isAlreadyScannedUnit(u.getSerialId());
     if(isAlreadyScanned){
         return new ResponseEntity<>(new scanResponse("multipleTimes scanned","null","could be fake item"),HttpStatus.CONFLICT);
     }

     dbfetch.setScannedUnits(List.of(u.getSerialId()));
     return  new ResponseEntity<>(new scanResponse("success","scan success","good item"),HttpStatus.OK);
    }

    @PostMapping("/carton/prefix")
    public ResponseEntity<?> scanCartonWithHashPrefix(@RequestBody scanDTO scanDTO) throws SQLException {

        String HashPrefix = scanDTO.getName();

        Carton c = dbfetch.getCartonByHashPrefix(HashPrefix);

        if (c == null) {
            return new ResponseEntity<>(
                    new scanResponse("bad Request","carton not found","fake item"),
                    HttpStatus.NOT_FOUND);
        }

        // parent pallet must be scanned first
        if (!dbfetch.isAlreadyScannedPallet(c.getParentPalletId())) {
            return new ResponseEntity<>(
                    new scanResponse("bad Request","carton scanned before parent pallet","fake item"),
                    HttpStatus.CONFLICT);
        }

        boolean isAlreadyScanned =
                dbfetch.isAlreadyScannedCarton(c.getSerialId());

        if (isAlreadyScanned) {
            return new ResponseEntity<>(
                    new scanResponse("multipleTimes scanned","null","could be fake item"),
                    HttpStatus.CONFLICT);
        }

        dbfetch.setScannedCartons(List.of(c.getSerialId()));

        return new ResponseEntity<>(
                new scanResponse("success","scan success","good item"),
                HttpStatus.OK);
    }
    @PostMapping("/pallet/prefix")
    public ResponseEntity<?> scanPalletWithHashPrefix(@RequestBody scanDTO scanDTO) throws SQLException {

        String HashPrefix = scanDTO.getName();

        Pallet p = dbfetch.getPalletByHashPrefix(HashPrefix);

        if (p == null) {
            return new ResponseEntity<>(
                    new scanResponse("bad Request","pallet not found","fake item"),
                    HttpStatus.NOT_FOUND);
        }

        boolean isAlreadyScanned =
                dbfetch.isAlreadyScannedPallet(p.getSerialId());

        if (isAlreadyScanned) {
            return new ResponseEntity<>(
                    new scanResponse("multipleTimes scanned","null","could be fake item"),
                    HttpStatus.CONFLICT);
        }

        dbfetch.setScannedPallets(List.of(p.getSerialId()));

        return new ResponseEntity<>(
                new scanResponse("success","scan success","good item"),
                HttpStatus.OK);
    }

}

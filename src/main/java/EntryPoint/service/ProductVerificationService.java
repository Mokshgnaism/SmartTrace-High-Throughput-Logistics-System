package EntryPoint.service;

import EntryPoint.model.Carton;
import EntryPoint.model.Pallet;
import EntryPoint.model.Unit;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class ProductVerificationService {

    private final HikariDataSource ds;

    public ProductVerificationService(HikariDataSource ds) {
        this.ds = ds;
    }

    // convert hex string → byte[]
    private byte[] hexToBytes(String hex) {

        int len = hex.length();
        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            out[i / 2] =
                    (byte)((Character.digit(hex.charAt(i),16) << 4)
                            + Character.digit(hex.charAt(i+1),16));
        }

        return out;
    }

    // ---------------------------------------------------
    // UNIT
    // ---------------------------------------------------

    public Unit verifyUnit(String hash) throws SQLException {

        String sql =
                "UPDATE units " +
                        "SET hash = NULL " +
                        "WHERE hash = ? " +
                        "RETURNING serial_id, parent_carton_id";

        try(Connection conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hash));

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {

                String serial =
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8);

                String parent =
                        new String(rs.getBytes("parent_carton_id"), StandardCharsets.UTF_8);

                return new Unit(serial,null,null,parent);
            }
        }

        return null;
    }

    // ---------------------------------------------------
    // CARTON
    // ---------------------------------------------------

    public Carton verifyCarton(String hash) throws SQLException {

        String sql =
                "UPDATE cartons " +
                        "SET hash = NULL " +
                        "WHERE hash = ? " +
                        "RETURNING serial_id,parent_pallet_id";

        try(Connection conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hash));

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {

                String serial =
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8);

                String parent =
                        new String(rs.getBytes("parent_pallet_id"), StandardCharsets.UTF_8);

                return new Carton(serial,null,null,parent);
            }
        }

        return null;
    }

    // ---------------------------------------------------
    // PALLET
    // ---------------------------------------------------

    public Pallet verifyPallet(String hash) throws SQLException {

        String sql =
                "UPDATE pallets " +
                        "SET hash = NULL " +
                        "WHERE hash = ? " +
                        "RETURNING ssic";

        try(Connection conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hash));

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {

                String serial =
                        new String(rs.getBytes("ssic"), StandardCharsets.UTF_8);

                return new Pallet(serial,null,null,null);
            }
        }

        return null;
    }
}

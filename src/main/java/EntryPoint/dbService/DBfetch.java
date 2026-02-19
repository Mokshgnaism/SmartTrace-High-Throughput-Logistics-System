package EntryPoint.dbService;

import EntryPoint.model.Carton;
import EntryPoint.model.Pallet;
import EntryPoint.model.Unit;
import EntryPoint.service.GeneratorService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DBfetch {
    private final GeneratorService generatorService;
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    public DataSource ds;
//    public HikariDataSource ds;
    private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        out[i / 2] =
                (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                        + Character.digit(hex.charAt(i+1), 16));
    }
    return out;
}



    public static String toHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    DBfetch(DataSource ds, GeneratorService generatorService) {
        this.generatorService = generatorService;
        this.ds = ds;
    }

    public void getUnitsByJobId(byte[]jobIdBytes,List<Unit>units) throws SQLException {
//        List<Unit> units = new ArrayList<>();
        try (var conn = ds.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT serial_id,parent_carton_id,hash,hash_prefix FROM units WHERE job_id = ?"
            );
            ps.setBytes(1, jobIdBytes);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String serialId = new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8);
                String parentCarton = new String(rs.getBytes("parent_carton_id"),StandardCharsets.UTF_8);
                String hash = toHex(rs.getBytes("hash"));
                String hashPrefix = toHex(rs.getBytes("hash_prefix"));

                units.add(new Unit(serialId, hash, hashPrefix, parentCarton));
            }
//            return units;
        }catch (SQLException e){
            throw new RuntimeException("errror from unit generation controller"+e);
        }
    }

    public void getCartonsByJobId(byte[] jobIdBytes,List<Carton>cartons) throws SQLException {
//        List<Carton> cartons = new ArrayList<>();
        try (var conn = ds.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT serial_id,parent_pallet_id,hash,hash_prefix FROM cartons WHERE job_id = ?"
            );
            ps.setBytes(1, jobIdBytes);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String serialId = new String(rs.getBytes("serial_id"),StandardCharsets.UTF_8);
                String parentPallet = new String(rs.getBytes("parent_pallet_id"),StandardCharsets.UTF_8);
                String hash = toHex(rs.getBytes("hash"));
                String hashPrefix = toHex(rs.getBytes("hash_prefix"));

                cartons.add(new Carton(serialId, hash, hashPrefix, parentPallet));
            }
//            return cartons;
        } catch (Exception e) {
            throw new RuntimeException("error fetching cartons", e);
        }
    }


    public  void getPalletsByJobId(byte[] jobIdBytes,List<Pallet>pallets) throws SQLException {
//        List<Pallet> pallets = new ArrayList<>();
        try (var conn = ds.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT ssic,hash,hash_prefix FROM pallets WHERE job_id = ?"
            );

            ps.setBytes(1, jobIdBytes);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String serialId = new String(rs.getBytes("ssic"),StandardCharsets.UTF_8);
                String hash = new String(rs.getBytes("hash"),StandardCharsets.UTF_8);
                String hashPrefix = toHex(rs.getBytes("hash_prefix"));
                pallets.add(new Pallet(serialId, hash, hashPrefix, null));
            }
//            return pallets;
        } catch (Exception e) {
            throw new RuntimeException("error fetching pallets", e);
        }
    }


    public Carton getCartonByHashPrefix(String hashPrefix) throws SQLException {

        String sql =
                "SELECT serial_id,parent_pallet_id,hash,hash_prefix " +
                        "FROM cartons WHERE hash_prefix = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hashPrefix));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String serialId =
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8);

                String parent =
                        new String(rs.getBytes("parent_pallet_id"), StandardCharsets.UTF_8);

                String hash = toHex(rs.getBytes("hash"));
                String hashPrefixStr = toHex(rs.getBytes("hash_prefix"));

                return new Carton(serialId, hash, hashPrefixStr, parent);
            }
        }

        return null;
    }

    public Carton getCartonById(String cartonId) throws SQLException {

        String sql =
                "SELECT serial_id,parent_pallet_id,hash,hash_prefix " +
                        "FROM cartons WHERE serial_id = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, cartonId.getBytes(StandardCharsets.UTF_8));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String serialId =
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8);

                String parent =
                        new String(rs.getBytes("parent_pallet_id"), StandardCharsets.UTF_8);

                String hash = toHex(rs.getBytes("hash"));
                String hashPrefix = toHex(rs.getBytes("hash_prefix"));

                return new Carton(serialId, hash, hashPrefix, parent);
            }
        }

        return null;
    }


    public Carton getCartonByHash(String hash) throws SQLException {

        String sql =
                "SELECT serial_id,parent_pallet_id,hash,hash_prefix " +
                        "FROM cartons WHERE hash = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hash));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String serialId =
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8);

                String parent =
                        new String(rs.getBytes("parent_pallet_id"), StandardCharsets.UTF_8);

                String hashStr = toHex(rs.getBytes("hash"));
                String hashPrefix = toHex(rs.getBytes("hash_prefix"));

                return new Carton(serialId, hashStr, hashPrefix, parent);
            }
        }

        return null;
    }


    public Unit getUnitById(String unitId) throws SQLException {

        String sql = "SELECT * FROM units WHERE serial_id = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, unitId.getBytes(StandardCharsets.UTF_8));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new Unit(
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8),
                        toHex(rs.getBytes("hash")),
                        toHex(rs.getBytes("hash_prefix")),
                        new String(rs.getBytes("parent_carton_id"), StandardCharsets.UTF_8)
                );
            }
        }

        return null;
    }


    public Unit getUnitByHash(String hash) throws SQLException {

        String sql = "SELECT * FROM units WHERE hash = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hash));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new Unit(
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8),
                        toHex(rs.getBytes("hash")),
                        toHex(rs.getBytes("hash_prefix")),
                        new String(rs.getBytes("parent_carton_id"), StandardCharsets.UTF_8)
                );
            }
        }

        return null;
    }


    public Unit getUnitByHashPrefix(String hashPrefix) throws SQLException {

        String sql = "SELECT * FROM units WHERE hash_prefix = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hashPrefix));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new Unit(
                        new String(rs.getBytes("serial_id"), StandardCharsets.UTF_8),
                        toHex(rs.getBytes("hash")),
                        toHex(rs.getBytes("hash_prefix")),
                        new String(rs.getBytes("parent_carton_id"), StandardCharsets.UTF_8)
                );
            }
        }

        return null;
    }


    public Pallet getPalletById(String palletId) throws SQLException {

        String sql = "SELECT * FROM pallets WHERE ssic = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, palletId.getBytes(StandardCharsets.UTF_8));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new Pallet(
                        new String(rs.getBytes("ssic"), StandardCharsets.UTF_8),
                        toHex(rs.getBytes("hash")),
                        toHex(rs.getBytes("hash_prefix")),
                        null
                );
            }
        }

        return null;
    }


    public Pallet getPalletByHash(String hash) throws SQLException {

        String sql = "SELECT * FROM pallets WHERE hash = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hash));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new Pallet(
                        new String(rs.getBytes("ssic"), StandardCharsets.UTF_8),
                        toHex(rs.getBytes("hash")),
                        toHex(rs.getBytes("hash_prefix")),
                        null
                );
            }
        }

        return null;
    }


    public Pallet getPalletByHashPrefix(String hashPrefix) throws SQLException {

        String sql = "SELECT * FROM pallets WHERE hash_prefix = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hashPrefix));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                return new Pallet(
                        new String(rs.getBytes("ssic"), StandardCharsets.UTF_8),
                        toHex(rs.getBytes("hash")),
                        toHex(rs.getBytes("hash_prefix")),
                        null
                );
            }
        }

        return null;
    }


    public boolean isAlreadyScannedPallet(String id) throws SQLException {

        String sql =
                "SELECT is_scanned FROM pallets WHERE ssic = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, id.getBytes(StandardCharsets.UTF_8));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("is_scanned");
            }
        }

        return false;
    }


    public boolean isAlreadyScannedCarton(String id) throws SQLException {

        String sql =
                "SELECT is_scanned FROM cartons WHERE serial_id = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, id.getBytes(StandardCharsets.UTF_8));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("is_scanned");
            }
        }

        return false;
    }


    public void setScannedPallets(List<String> ids) throws SQLException {

        String sql =
                "UPDATE pallets SET is_scanned = TRUE WHERE ssic = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String id : ids) {
                ps.setBytes(1, id.getBytes(StandardCharsets.UTF_8));
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }


    public void setScannedCartons(List<String> ids) throws SQLException {

        String sql =
                "UPDATE cartons SET is_scanned = TRUE WHERE serial_id = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String id : ids) {
                ps.setBytes(1, id.getBytes(StandardCharsets.UTF_8));
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }


    public boolean isAlreadyScannedUnit(String id) throws SQLException {

        String sql =
                "SELECT is_scanned FROM units WHERE serial_id = ? LIMIT 1";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, id.getBytes(StandardCharsets.UTF_8));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("is_scanned");
            }
        }

        return false;
    }


    public void setScannedUnits(List<String> ids) throws SQLException {

        String sql =
                "UPDATE units SET is_scanned = TRUE WHERE serial_id = ?";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String id : ids) {
                ps.setBytes(1, id.getBytes(StandardCharsets.UTF_8));
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }


    public boolean scanUnitAtomic(String hashPrefix) throws SQLException {

        String sql =
                "UPDATE units u " +
                        "SET is_scanned = TRUE " +
                        "FROM cartons c, pallets p " +
                        "WHERE u.hash_prefix = ? " +
                        "AND u.parent_carton_id = c.serial_id " +
                        "AND c.parent_pallet_id = p.ssic " +
                        "AND c.is_scanned = TRUE " +
                        "AND u.is_scanned = FALSE " +
                        "RETURNING u.serial_id";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hashPrefix));

            ResultSet rs = ps.executeQuery();

            return rs.next(); // true = success scan
        }
    }


    public boolean scanCartonAtomic(String hashPrefix) throws SQLException {

        String sql =
                "UPDATE cartons c " +
                        "SET is_scanned = TRUE " +
                        "FROM pallets p " +
                        "WHERE c.hash_prefix = ? " +
                        "AND c.parent_pallet_id = p.ssic " +
                        "AND p.is_scanned = TRUE " +
                        "AND c.is_scanned = FALSE " +
                        "RETURNING c.serial_id";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hashPrefix));

            ResultSet rs = ps.executeQuery();

            return rs.next();
        }
    }


    public boolean scanPalletAtomic(String hashPrefix) throws SQLException {

        String sql =
                "UPDATE pallets " +
                        "SET is_scanned = TRUE " +
                        "WHERE hash_prefix = ? " +
                        "AND is_scanned = FALSE " +
                        "RETURNING ssic";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBytes(1, hexToBytes(hashPrefix));

            ResultSet rs = ps.executeQuery();

            return rs.next();
        }
    }


}

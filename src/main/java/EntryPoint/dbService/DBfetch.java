package EntryPoint.dbService;

import EntryPoint.model.Carton;
import EntryPoint.model.Pallet;
import EntryPoint.model.Unit;
import EntryPoint.service.GeneratorService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DBfetch {
    public HikariDataSource ds;
    private final GeneratorService generatorService;
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
    DBfetch(HikariDataSource ds, GeneratorService generatorService) {
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
    public void getUnitsByJobId(byte[]jobIdBytes,List<Unit>units) throws SQLException {
//        List<Unit> units = new ArrayList<>();
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

                String serialId = toHex(rs.getBytes("serial_id"));
                String parentPallet = toHex(rs.getBytes("parent_pallet_id"));
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
                String serialId = toHex(rs.getBytes("ssic"));
                String hash = toHex(rs.getBytes("hash"));
                String hashPrefix = toHex(rs.getBytes("hash_prefix"));
                pallets.add(new Pallet(serialId, hash, hashPrefix, null));
            }
//            return pallets;
        } catch (Exception e) {
            throw new RuntimeException("error fetching pallets", e);
        }
    }

}

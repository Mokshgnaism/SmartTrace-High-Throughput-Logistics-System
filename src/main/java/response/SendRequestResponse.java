package response;

import lombok.AllArgsConstructor;
import model.Carton;
import model.Pallet;
import model.Unit;

import java.util.List;
@AllArgsConstructor
public class SendRequestResponse {
    public List<Unit> units;
    public List<Carton> cartons;
    public List<Pallet> pallets;
}

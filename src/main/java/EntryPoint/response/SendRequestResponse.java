package EntryPoint.response;

import lombok.AllArgsConstructor;
import EntryPoint.model.Carton;
import EntryPoint.model.Pallet;
import EntryPoint.model.Unit;

import java.util.List;
@AllArgsConstructor
public class SendRequestResponse {
    public List<Unit> units;
    public List<Carton> cartons;
    public List<Pallet> pallets;
}

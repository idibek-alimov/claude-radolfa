package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.PutawayUseCase;
import tj.radolfa.application.ports.out.InventoryPlacementPort;
import tj.radolfa.application.ports.out.LoadWarehousePort;
import tj.radolfa.application.ports.out.RecordInventoryTransactionPort;
import tj.radolfa.domain.model.InventoryTransaction;
import tj.radolfa.domain.model.InventoryTransactionType;

import java.time.Instant;

@Service
@Transactional
public class PutawayService implements PutawayUseCase {

    private final InventoryPlacementPort          placementPort;
    private final RecordInventoryTransactionPort  ledgerPort;
    private final LoadWarehousePort               loadWarehousePort;

    public PutawayService(InventoryPlacementPort placementPort,
                          RecordInventoryTransactionPort ledgerPort,
                          LoadWarehousePort loadWarehousePort) {
        this.placementPort    = placementPort;
        this.ledgerPort       = ledgerPort;
        this.loadWarehousePort = loadWarehousePort;
    }

    @Override
    public void execute(Command cmd) {
        Long wh = loadWarehousePort.findDefault().id();
        placementPort.putaway(cmd.skuId(), wh, cmd.binId(), cmd.quantity());
        ledgerPort.record(new InventoryTransaction(
                null, cmd.skuId(), wh, 0,
                InventoryTransactionType.PUTAWAY,
                "BIN", cmd.binId(), cmd.actorUserId(),
                "inbound→bin:" + cmd.binId() + " qty:" + cmd.quantity(),
                Instant.now()));
    }
}

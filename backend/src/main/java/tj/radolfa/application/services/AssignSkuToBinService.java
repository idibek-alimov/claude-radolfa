package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.warehouse.AssignSkuToBinUseCase;
import tj.radolfa.application.ports.out.AssignSkuToBinPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.application.ports.out.LoadWarehouseLocationPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

@Service
@Transactional
public class AssignSkuToBinService implements AssignSkuToBinUseCase {

    private final AssignSkuToBinPort         assignSkuToBinPort;
    private final LoadSkuPort                loadSkuPort;
    private final LoadWarehouseLocationPort  loadWarehouseLocationPort;

    public AssignSkuToBinService(AssignSkuToBinPort assignSkuToBinPort,
                                 LoadSkuPort loadSkuPort,
                                 LoadWarehouseLocationPort loadWarehouseLocationPort) {
        this.assignSkuToBinPort        = assignSkuToBinPort;
        this.loadSkuPort               = loadSkuPort;
        this.loadWarehouseLocationPort = loadWarehouseLocationPort;
    }

    @Override
    public void execute(Long skuId, Long binId) {
        loadSkuPort.findSkuById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found: " + skuId));
        if (binId != null) {
            loadWarehouseLocationPort.findBinById(binId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bin not found: " + binId));
        }
        assignSkuToBinPort.assign(skuId, binId);
    }
}

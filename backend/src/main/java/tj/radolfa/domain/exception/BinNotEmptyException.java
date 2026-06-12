package tj.radolfa.domain.exception;

public class BinNotEmptyException extends RuntimeException {

    private final Long binId;

    public BinNotEmptyException(Long binId) {
        super("Bin id=" + binId + " holds placement rows — relocate all stock before deleting");
        this.binId = binId;
    }

    public Long getBinId() { return binId; }
}

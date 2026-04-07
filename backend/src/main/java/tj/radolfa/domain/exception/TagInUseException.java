package tj.radolfa.domain.exception;

public class TagInUseException extends RuntimeException {

    private final long variantCount;

    public TagInUseException(Long tagId, long variantCount) {
        super("Tag " + tagId + " cannot be deleted — " + variantCount + " variant(s) still reference it");
        this.variantCount = variantCount;
    }

    public long getVariantCount() {
        return variantCount;
    }
}

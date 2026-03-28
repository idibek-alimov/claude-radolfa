package tj.radolfa.application.ports.in;

public interface CreateProductTagUseCase {
    Long execute(String name, String colorHex);
}

package tj.radolfa.application.ports.out;

/**
 * Out-Port: persist color data.
 */
public interface SaveColorPort {

    LoadColorPort.ColorView save(String colorKey, String displayName, String hexCode);

    LoadColorPort.ColorView update(Long id, String displayName, String hexCode);
}

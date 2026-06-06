package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadHomeBannerPort;
import tj.radolfa.domain.model.BannerSlot;
import tj.radolfa.domain.model.HomeBanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GetActiveHomeBannersServiceTest {

    private FakeLoadHomeBannerPort fakePort;
    private GetActiveHomeBannersService service;

    @BeforeEach
    void setUp() {
        fakePort = new FakeLoadHomeBannerPort();
        service  = new GetActiveHomeBannersService(fakePort);
    }

    @Test
    void returnsOnlyActiveBanners() {
        fakePort.store(banner(1L, BannerSlot.MAIN,    true));
        fakePort.store(banner(2L, BannerSlot.WELCOME, true));
        fakePort.store(banner(3L, BannerSlot.MAIN,    false));

        List<HomeBanner> result = service.execute();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(HomeBanner::active);
    }

    @Test
    void returnsEmptyListWhenNoActiveBanners() {
        fakePort.store(banner(1L, BannerSlot.MAIN, false));

        assertThat(service.execute()).isEmpty();
    }

    @Test
    void returnsEmptyListWhenNoBannersAtAll() {
        assertThat(service.execute()).isEmpty();
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private HomeBanner banner(Long id, BannerSlot slot, boolean active) {
        return new HomeBanner(id, slot, "Title " + id, null, null, null, null, null, null, active);
    }

    static class FakeLoadHomeBannerPort implements LoadHomeBannerPort {
        private final List<HomeBanner> store = new ArrayList<>();

        void store(HomeBanner b) { store.add(b); }

        @Override public List<HomeBanner> findActive() {
            return store.stream().filter(HomeBanner::active).toList();
        }
        @Override public Optional<HomeBanner> findById(Long id) {
            return store.stream().filter(b -> b.id().equals(id)).findFirst();
        }
        @Override public List<HomeBanner> findAll() { return List.copyOf(store); }
    }
}

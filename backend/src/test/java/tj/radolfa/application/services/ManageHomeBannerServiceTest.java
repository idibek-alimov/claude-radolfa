package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.home.ManageHomeBannerUseCase.Command;
import tj.radolfa.application.ports.out.LoadHomeBannerPort;
import tj.radolfa.application.ports.out.SaveHomeBannerPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.BannerSlot;
import tj.radolfa.domain.model.HomeBanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ManageHomeBannerServiceTest {

    private FakeLoadHomeBannerPort fakeLoad;
    private FakeSaveHomeBannerPort fakeSave;
    private ManageHomeBannerService service;

    @BeforeEach
    void setUp() {
        fakeLoad = new FakeLoadHomeBannerPort();
        fakeSave = new FakeSaveHomeBannerPort(fakeLoad);
        service  = new ManageHomeBannerService(fakeLoad, fakeSave);
    }

    // ----------------------------------------------------------------
    //  create
    // ----------------------------------------------------------------

    @Test
    void createPersistsAndReturnsWithId() {
        HomeBanner result = service.create(command(BannerSlot.MAIN, "Summer Sale"));

        assertThat(result.id()).isNotNull();
        assertThat(result.slot()).isEqualTo(BannerSlot.MAIN);
        assertThat(result.title()).isEqualTo("Summer Sale");
    }

    @Test
    void createWithBlankTitleThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(command(BannerSlot.MAIN, "  ")));
    }

    @Test
    void createWithNullSlotThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(command(null, "Valid Title")));
    }

    // ----------------------------------------------------------------
    //  update
    // ----------------------------------------------------------------

    @Test
    void updateMutatesStoredBanner() {
        HomeBanner existing = service.create(command(BannerSlot.MAIN, "Old Title"));

        HomeBanner updated = service.update(existing.id(), command(BannerSlot.WELCOME, "New Title"));

        assertThat(updated.id()).isEqualTo(existing.id());
        assertThat(updated.slot()).isEqualTo(BannerSlot.WELCOME);
        assertThat(updated.title()).isEqualTo("New Title");
    }

    @Test
    void updateUnknownIdThrows() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(999L, command(BannerSlot.MAIN, "Title")));
    }

    @Test
    void updateWithBlankTitleThrows() {
        HomeBanner existing = service.create(command(BannerSlot.MAIN, "Original"));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(existing.id(), command(BannerSlot.MAIN, "")));
    }

    // ----------------------------------------------------------------
    //  delete
    // ----------------------------------------------------------------

    @Test
    void deleteRemovesBanner() {
        HomeBanner banner = service.create(command(BannerSlot.WELCOME, "Promo"));

        service.delete(banner.id());

        assertThat(fakeLoad.findById(banner.id())).isEmpty();
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private Command command(BannerSlot slot, String title) {
        return new Command(slot, title, null, null, null, null, null, null, true);
    }

    // ----------------------------------------------------------------
    //  Fakes
    // ----------------------------------------------------------------

    static class FakeLoadHomeBannerPort implements LoadHomeBannerPort {
        private final List<HomeBanner> store = new ArrayList<>();

        @Override public List<HomeBanner> findActive() {
            return store.stream().filter(HomeBanner::active).toList();
        }
        @Override public Optional<HomeBanner> findById(Long id) {
            return store.stream().filter(b -> b.id().equals(id)).findFirst();
        }
        @Override public List<HomeBanner> findAll() { return List.copyOf(store); }

        void removeById(Long id) { store.removeIf(b -> b.id().equals(id)); }
        void put(HomeBanner b)   { store.removeIf(x -> x.id().equals(b.id())); store.add(b); }
    }

    static class FakeSaveHomeBannerPort implements SaveHomeBannerPort {
        private final FakeLoadHomeBannerPort load;
        private final AtomicLong seq = new AtomicLong(1);

        FakeSaveHomeBannerPort(FakeLoadHomeBannerPort load) { this.load = load; }

        @Override
        public HomeBanner save(HomeBanner banner) {
            Long id = banner.id() != null ? banner.id() : seq.getAndIncrement();
            HomeBanner saved = new HomeBanner(id, banner.slot(), banner.title(), banner.subtitle(),
                    banner.badgeText(), banner.ctaLabel(), banner.ctaUrl(), banner.bgColorHex(),
                    banner.expiresAt(), banner.active());
            load.put(saved);
            return saved;
        }

        @Override
        public void delete(Long id) { load.removeById(id); }
    }
}

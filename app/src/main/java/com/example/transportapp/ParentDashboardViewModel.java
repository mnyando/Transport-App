package com.example.transportapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * State for {@link ParentLanding} (review section 4: a ViewModel that survives rotation and
 * holds LiveData, backed by a {@link ParentRepository}).
 *
 * <p>Children and alerts are loaded once per screen, not again on every rotation, and the live
 * alert listener is removed when the screen is finished for good.
 */
public class ParentDashboardViewModel extends ViewModel {

    static final int MAX_NOTIFICATIONS = 50;

    /** Children list plus whether it is still loading or failed. */
    public static final class ChildrenState {
        public final boolean loading;
        public final boolean failed;
        @NonNull public final List<Student> children;

        ChildrenState(boolean loading, boolean failed, @NonNull List<Student> children) {
            this.loading = loading;
            this.failed = failed;
            this.children = children;
        }
    }

    /** Result of "Where is the bus?". {@code openMap} is cleared once the map has been shown. */
    public static final class BusStatus {
        public enum Kind { CHECKING, ON_TRIP, NOT_ON_TRIP, UNAVAILABLE }

        public final Kind kind;
        public final String vehicle;
        @Nullable public final Double lat;
        @Nullable public final Double lng;
        /** Minutes since the last update, or -1 when unknown. */
        public final long minutesAgo;
        private boolean openMap;

        BusStatus(Kind kind, String vehicle, @Nullable Double lat, @Nullable Double lng,
                  long minutesAgo, boolean openMap) {
            this.kind = kind;
            this.vehicle = vehicle;
            this.lat = lat;
            this.lng = lng;
            this.minutesAgo = minutesAgo;
            this.openMap = openMap;
        }

        /** True the first time it is called for an ON_TRIP status, then false (survives rotation). */
        public boolean takeOpenMap() {
            boolean open = openMap;
            openMap = false;
            return open;
        }
    }

    private final ParentRepository repository;
    private final Clock clock;
    private final MutableLiveData<ChildrenState> children = new MutableLiveData<>();
    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notificationsFailed = new MutableLiveData<>(false);
    private final MutableLiveData<BusStatus> busStatus = new MutableLiveData<>();
    @Nullable private ParentRepository.Subscription notificationSubscription;
    @Nullable private String parentEmail;

    interface Clock {
        long now();
    }

    public ParentDashboardViewModel(ParentRepository repository) {
        this(repository, System::currentTimeMillis);
    }

    ParentDashboardViewModel(ParentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LiveData<ChildrenState> getChildren() {
        return children;
    }

    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }

    public LiveData<Boolean> getNotificationsFailed() {
        return notificationsFailed;
    }

    public LiveData<BusStatus> getBusStatus() {
        return busStatus;
    }

    /** Starts loading for this parent. Calling it again (e.g. after rotation) does nothing. */
    public void start(@NonNull String email) {
        if (email.equals(parentEmail)) return;
        parentEmail = email;
        refreshChildren();
        if (notificationSubscription != null) notificationSubscription.cancel();
        notificationSubscription = repository.listenToNotifications(email,
                new ParentRepository.Callback<List<Notification>>() {
                    @Override
                    public void onResult(List<Notification> value) {
                        notificationsFailed.setValue(false);
                        notifications.setValue(newestFirst(value, MAX_NOTIFICATIONS));
                    }

                    @Override
                    public void onError(Exception e) {
                        notificationsFailed.setValue(true);
                    }
                });
    }

    public void refreshChildren() {
        if (parentEmail == null) return;
        ChildrenState current = children.getValue();
        List<Student> shown = current != null ? current.children : Collections.emptyList();
        children.setValue(new ChildrenState(true, false, shown));
        repository.loadChildren(parentEmail, new ParentRepository.Callback<List<Student>>() {
            @Override
            public void onResult(List<Student> value) {
                children.setValue(new ChildrenState(false, false, value));
            }

            @Override
            public void onError(Exception e) {
                children.setValue(new ChildrenState(false, true, shown));
            }
        });
    }

    /** The distinct buses the parent's children ride, in the order the children are listed. */
    public List<String> getVehicles() {
        ChildrenState state = children.getValue();
        return vehiclesOf(state != null ? state.children : Collections.emptyList());
    }

    public void checkBus(@NonNull String vehicle) {
        busStatus.setValue(new BusStatus(BusStatus.Kind.CHECKING, vehicle, null, null, -1, false));
        repository.loadBusLocation(vehicle, new ParentRepository.Callback<ParentRepository.BusLocation>() {
            @Override
            public void onResult(ParentRepository.BusLocation location) {
                busStatus.setValue(toStatus(vehicle, location, clock.now()));
            }

            @Override
            public void onError(Exception e) {
                busStatus.setValue(new BusStatus(BusStatus.Kind.UNAVAILABLE, vehicle, null, null, -1, false));
            }
        });
    }

    @Override
    protected void onCleared() {
        if (notificationSubscription != null) notificationSubscription.cancel();
        notificationSubscription = null;
    }

    // ---- pure helpers (unit tested) ----

    static List<Notification> newestFirst(List<Notification> list, int max) {
        List<Notification> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return sorted.size() > max ? new ArrayList<>(sorted.subList(0, max)) : sorted;
    }

    static List<String> vehiclesOf(List<Student> children) {
        Set<String> vehicles = new LinkedHashSet<>();
        for (Student child : children) {
            String v = child.getVehicle();
            if (v != null && !v.trim().isEmpty()) vehicles.add(v);
        }
        return new ArrayList<>(vehicles);
    }

    static BusStatus toStatus(String vehicle, ParentRepository.BusLocation location, long now) {
        if (location == null || !location.isOnTrip()) {
            return new BusStatus(BusStatus.Kind.NOT_ON_TRIP, vehicle, null, null, -1, false);
        }
        long minutes = location.updatedAtMillis == null ? -1
                : Math.max(0, (now - location.updatedAtMillis) / 60000);
        return new BusStatus(BusStatus.Kind.ON_TRIP, vehicle, location.lat, location.lng, minutes, true);
    }

    /** Creates the ViewModel with the Firestore repository. */
    public static ViewModelProvider.Factory factory(ParentRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ParentDashboardViewModel(repository);
            }
        };
    }
}

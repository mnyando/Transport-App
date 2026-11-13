package com.example.transportapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * State and rules for one pick-up or drop-off run (review #12 and #17). Replaces the old
 * PickUp and DropOff screens, which were 600 lines of near-identical code.
 *
 * <ul>
 *   <li>Nothing is written until the driver taps <b>Start trip</b> (the old screens opened a trip
 *       as soon as they were shown and never closed it if the driver backed out).</li>
 *   <li>One trip at a time per bus: an unfinished trip of the same kind is resumed with the
 *       statuses already recorded; an unfinished trip of the other kind must be finished first.</li>
 *   <li>Every child starts unmarked. Completing the trip saves which children were carried
 *       ({@code studentIds}) and sends each parent the status actually recorded.</li>
 * </ul>
 */
public class TripViewModel extends ViewModel {

    public enum Phase {
        LOADING, LOAD_FAILED, NO_STUDENTS,
        /** An unfinished trip of the other kind blocks this one; see {@link TripState#otherTrip}. */
        OTHER_TRIP_OPEN,
        READY, STARTING, ACTIVE, COMPLETING, COMPLETED
    }

    public static final class TripState {
        public final Phase phase;
        @NonNull public final List<Student> students;
        @Nullable public final String tripId;
        @Nullable public final Long startMillis;
        @Nullable public final TripRepository.OpenTrip otherTrip;
        /** True when an unfinished trip was picked up again instead of starting a new one. */
        public final boolean resumed;
        /** After COMPLETED: children whose parents could not be told (no email on file). */
        public final int parentsWithoutEmail;

        TripState(Phase phase, @NonNull List<Student> students, @Nullable String tripId,
                  @Nullable Long startMillis, @Nullable TripRepository.OpenTrip otherTrip,
                  boolean resumed, int parentsWithoutEmail) {
            this.phase = phase;
            this.students = students;
            this.tripId = tripId;
            this.startMillis = startMillis;
            this.otherTrip = otherTrip;
            this.resumed = resumed;
            this.parentsWithoutEmail = parentsWithoutEmail;
        }

        TripState with(Phase newPhase) {
            return new TripState(newPhase, students, tripId, startMillis, otherTrip, resumed, parentsWithoutEmail);
        }
    }

    interface Clock {
        long now();
    }

    private final TripRepository repository;
    private final Clock clock;
    private final MutableLiveData<TripState> state = new MutableLiveData<>();
    private final MutableLiveData<Exception> error = new MutableLiveData<>();
    private final Map<String, Boolean> statuses = new HashMap<>();
    @Nullable private TripRepository.TripDetails details;

    public TripViewModel(TripRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LiveData<TripState> getState() {
        return state;
    }

    /** The latest failure, or null. Call {@link #clearError()} once it has been shown. */
    public LiveData<Exception> getError() {
        return error;
    }

    public void clearError() {
        error.setValue(null);
    }

    @Nullable
    public TripRepository.TripDetails getDetails() {
        return details;
    }

    /** Loads the children and looks for an unfinished trip. Safe to call again after rotation. */
    public void load(TripRepository.TripDetails tripDetails) {
        if (details != null && state.getValue() != null && state.getValue().phase != Phase.LOAD_FAILED) {
            return;
        }
        details = tripDetails;
        statuses.clear();
        setState(new TripState(Phase.LOADING, Collections.emptyList(), null, null, null, false, 0));

        repository.loadStudents(details.vehicle, new TripRepository.Callback<List<Student>>() {
            @Override
            public void onResult(List<Student> value) {
                List<Student> students = sortedByName(value);
                if (students.isEmpty()) {
                    setState(new TripState(Phase.NO_STUDENTS, students, null, null, null, false, 0));
                    return;
                }
                findOpenTrip(students);
            }

            @Override
            public void onError(Exception e) {
                fail(Phase.LOAD_FAILED, e);
            }
        });
    }

    private void findOpenTrip(List<Student> students) {
        repository.findOpenTrip(details.vehicle, details.driverId, new TripRepository.Callback<TripRepository.OpenTrip>() {
            @Override
            public void onResult(@Nullable TripRepository.OpenTrip open) {
                if (open == null) {
                    setState(new TripState(Phase.READY, students, null, null, null, false, 0));
                } else if (open.tripType.equals(details.tripType)) {
                    resume(students, open);
                } else {
                    setState(new TripState(Phase.OTHER_TRIP_OPEN, students, null, null, open, false, 0));
                }
            }

            @Override
            public void onError(Exception e) {
                fail(Phase.LOAD_FAILED, e);
            }
        });
    }

    private void resume(List<Student> students, TripRepository.OpenTrip open) {
        repository.loadStatuses(open.tripId, open.tripType, new TripRepository.Callback<Map<String, Boolean>>() {
            @Override
            public void onResult(Map<String, Boolean> saved) {
                statuses.clear();
                statuses.putAll(saved);
                setState(new TripState(Phase.ACTIVE, students, open.tripId, open.startMillis, null, true, 0));
            }

            @Override
            public void onError(Exception e) {
                fail(Phase.LOAD_FAILED, e);
            }
        });
    }

    public void startTrip() {
        TripState s = state.getValue();
        if (s == null || s.phase != Phase.READY || details == null) return;
        setState(s.with(Phase.STARTING));
        long start = clock.now();
        repository.createTrip(details, start, s.students.size(), new TripRepository.Callback<String>() {
            @Override
            public void onResult(String tripId) {
                repository.joinTrip(tripId, details.vehicle, s.students);
                setState(new TripState(Phase.ACTIVE, s.students, tripId, start, null, false, 0));
            }

            @Override
            public void onError(Exception e) {
                setState(s.with(Phase.READY));
                error.setValue(e);
            }
        });
    }

    /** TRUE = picked/dropped, FALSE = not picked/not dropped, null = not marked. */
    @Nullable
    public Boolean statusFor(Student student) {
        return student.getId() == null ? null : statuses.get(student.getId());
    }

    public boolean canMark() {
        TripState s = state.getValue();
        return s != null && s.phase == Phase.ACTIVE;
    }

    public void setStatus(Student student, @Nullable Boolean status) {
        TripState s = state.getValue();
        if (!canMark() || s.tripId == null || student.getId() == null || details == null) return;
        if (status == null) {
            // Unticked: back to "not marked". Nothing is written; the summary treats the child
            // as unconfirmed.
            statuses.remove(student.getId());
            return;
        }
        statuses.put(student.getId(), status);
        if (details.isPickup()) student.updatePickupStatus(status, s.tripId);
        repository.recordStatus(s.tripId, details.tripType, student, status, clock.now());
    }

    public int unmarkedCount() {
        TripState s = state.getValue();
        if (s == null) return 0;
        int unmarked = 0;
        for (Student student : s.students) {
            if (statusFor(student) == null) unmarked++;
        }
        return unmarked;
    }

    public void completeTrip() {
        TripState s = state.getValue();
        if (s == null || s.phase != Phase.ACTIVE || s.tripId == null || details == null) return;
        setState(s.with(Phase.COMPLETING));

        List<String> carried = new ArrayList<>();
        List<Student> leaveTrip = new ArrayList<>();
        for (Student student : s.students) {
            boolean done = Boolean.TRUE.equals(statusFor(student));
            if (done && student.getId() != null) carried.add(student.getId());
            // After pick-up every child leaves the trip record; after drop-off only those
            // confirmed dropped (anyone else keeps currentTripId so the school can see where
            // they were last).
            if (details.isPickup() || done) leaveTrip.add(student);
        }

        repository.completeTrip(s.tripId, s.startMillis, clock.now(), carried, leaveTrip,
                new TripRepository.Callback<Void>() {
                    @Override
                    public void onResult(Void value) {
                        int missing = repository.sendAlerts(details, s.tripId, s.students, new HashMap<>(statuses));
                        setState(new TripState(Phase.COMPLETED, s.students, s.tripId, s.startMillis,
                                null, s.resumed, missing));
                    }

                    @Override
                    public void onError(Exception e) {
                        setState(s.with(Phase.ACTIVE));
                        error.setValue(e);
                    }
                });
    }

    static List<Student> sortedByName(List<Student> students) {
        List<Student> sorted = new ArrayList<>(students);
        sorted.sort((a, b) -> name(a).compareTo(name(b)));
        return sorted;
    }

    private static String name(Student s) {
        return s.getName() == null ? "" : s.getName().toLowerCase(Locale.ROOT);
    }

    private void fail(Phase phase, Exception e) {
        TripState s = state.getValue();
        setState(new TripState(phase, s != null ? s.students : Collections.emptyList(), null, null, null, false, 0));
        error.setValue(e);
    }

    private void setState(TripState newState) {
        state.setValue(newState);
    }

    static ViewModelProvider.Factory factory(TripRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TripViewModel(repository, System::currentTimeMillis);
            }
        };
    }
}

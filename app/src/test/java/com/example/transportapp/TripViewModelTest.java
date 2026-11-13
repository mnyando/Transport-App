package com.example.transportapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Trip rules from review #12: explicit start, one trip at a time, resume, honest completion. */
public class TripViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTasks = new InstantTaskExecutorRule();

    private static final long NOW = 1_700_000_000_000L;

    /** Records every call and lets each test answer it. */
    static class FakeRepository implements TripRepository {
        Callback<List<Student>> studentsCallback;
        Callback<OpenTrip> openTripCallback;
        Callback<Map<String, Boolean>> statusesCallback;
        Callback<String> createCallback;
        Callback<Void> completeCallback;
        int createCalls;
        String joinedTrip;
        final List<String> recorded = new ArrayList<>();
        List<String> carried;
        List<Student> leftTrip;
        Map<String, Boolean> alertedStatuses;
        int parentsWithoutEmail;

        @Override public void loadStudents(String vehicle, Callback<List<Student>> c) { studentsCallback = c; }
        @Override public void findOpenTrip(String vehicle, String driverId, Callback<OpenTrip> c) { openTripCallback = c; }
        @Override public void loadStatuses(String tripId, String tripType, Callback<Map<String, Boolean>> c) { statusesCallback = c; }

        @Override
        public void createTrip(TripDetails details, long startMillis, int studentCount, Callback<String> c) {
            createCalls++;
            createCallback = c;
        }

        @Override
        public void recordStatus(String tripId, String tripType, Student student, boolean done, long nowMillis) {
            recorded.add(tripId + ":" + student.getId() + "=" + done);
        }

        @Override
        public void completeTrip(String tripId, @Nullable Long startMillis, long endMillis,
                                 List<String> carriedIds, List<Student> leaveTrip, Callback<Void> c) {
            carried = carriedIds;
            leftTrip = leaveTrip;
            completeCallback = c;
        }

        @Override public void joinTrip(String tripId, String vehicle, List<Student> students) { joinedTrip = tripId; }

        @Override
        public int sendAlerts(TripDetails details, String tripId, List<Student> students, Map<String, Boolean> statuses) {
            alertedStatuses = statuses;
            return parentsWithoutEmail;
        }
    }

    private FakeRepository repo;
    private TripViewModel vm;
    private Student amani, baraka, chiku;

    private static Student student(String id, String name) {
        Student s = new Student();
        s.setId(id);
        s.setName(name);
        s.setGrade("Grade 1");
        return s;
    }

    private static TripRepository.TripDetails details(String type) {
        return new TripRepository.TripDetails(type, "KBX 123", "LIC1", "Driver", "Route A");
    }

    private TripViewModel.Phase phase() {
        return vm.getState().getValue().phase;
    }

    @Before
    public void setUp() {
        repo = new FakeRepository();
        vm = new TripViewModel(repo, () -> NOW);
        amani = student("s1", "Amani");
        baraka = student("s2", "Baraka");
        chiku = student("s3", "Chiku");
    }

    /** Loads the bus with three children and no unfinished trip. */
    private void loadReady(String type) {
        vm.load(details(type));
        repo.studentsCallback.onResult(Arrays.asList(chiku, amani, baraka));
        repo.openTripCallback.onResult(null);
    }

    private void startActive(String type) {
        loadReady(type);
        vm.startTrip();
        repo.createCallback.onResult("trip1");
    }

    @Test
    public void loadsChildrenSortedByNameAndWaitsForStart() {
        loadReady(TripRepository.PICKUP);
        assertEquals(TripViewModel.Phase.READY, phase());
        List<Student> shown = vm.getState().getValue().students;
        assertEquals(Arrays.asList("Amani", "Baraka", "Chiku"),
                Arrays.asList(shown.get(0).getName(), shown.get(1).getName(), shown.get(2).getName()));
        assertEquals("no trip is created just by opening the screen", 0, repo.createCalls);
        assertFalse(vm.canMark());
    }

    @Test
    public void marksAreIgnoredBeforeTheTripStarts() {
        loadReady(TripRepository.PICKUP);
        vm.setStatus(amani, true);
        assertTrue(repo.recorded.isEmpty());
        assertNull(vm.statusFor(amani));
    }

    @Test
    public void busWithNoChildren() {
        vm.load(details(TripRepository.PICKUP));
        repo.studentsCallback.onResult(Collections.emptyList());
        assertEquals(TripViewModel.Phase.NO_STUDENTS, phase());
    }

    @Test
    public void startTripCreatesOneTripAndJoinsTheChildren() {
        startActive(TripRepository.PICKUP);
        assertEquals(TripViewModel.Phase.ACTIVE, phase());
        assertEquals(1, repo.createCalls);
        assertEquals("trip1", repo.joinedTrip);
        assertEquals(Long.valueOf(NOW), vm.getState().getValue().startMillis);
        vm.startTrip(); // a second tap does nothing
        assertEquals(1, repo.createCalls);
    }

    @Test
    public void failedStartGoesBackToReadyWithAnError() {
        loadReady(TripRepository.PICKUP);
        vm.startTrip();
        repo.createCallback.onError(new RuntimeException("offline"));
        assertEquals(TripViewModel.Phase.READY, phase());
        assertNotNull(vm.getError().getValue());
    }

    @Test
    public void unfinishedTripOfTheSameKindIsResumedWithItsMarks() {
        vm.load(details(TripRepository.PICKUP));
        repo.studentsCallback.onResult(Arrays.asList(amani, baraka));
        repo.openTripCallback.onResult(new TripRepository.OpenTrip("old", TripRepository.PICKUP, NOW - 60_000));
        Map<String, Boolean> saved = new HashMap<>();
        saved.put("s1", true);
        repo.statusesCallback.onResult(saved);

        TripViewModel.TripState s = vm.getState().getValue();
        assertEquals(TripViewModel.Phase.ACTIVE, s.phase);
        assertTrue(s.resumed);
        assertEquals("old", s.tripId);
        assertEquals(Boolean.TRUE, vm.statusFor(amani));
        assertNull(vm.statusFor(baraka));
        assertEquals(0, repo.createCalls);
    }

    @Test
    public void unfinishedTripOfTheOtherKindBlocksANewOne() {
        vm.load(details(TripRepository.DROPOFF));
        repo.studentsCallback.onResult(Arrays.asList(amani));
        repo.openTripCallback.onResult(new TripRepository.OpenTrip("morning", TripRepository.PICKUP, NOW));
        assertEquals(TripViewModel.Phase.OTHER_TRIP_OPEN, phase());
        assertEquals("morning", vm.getState().getValue().otherTrip.tripId);
        vm.startTrip();
        assertEquals(0, repo.createCalls);
    }

    @Test
    public void marksAreSavedAndUntickingIsNotWritten() {
        startActive(TripRepository.PICKUP);
        vm.setStatus(amani, true);
        vm.setStatus(baraka, false);
        vm.setStatus(baraka, null);
        assertEquals(Arrays.asList("trip1:s1=true", "trip1:s2=false"), repo.recorded);
        assertNull(vm.statusFor(baraka));
        assertEquals(2, vm.unmarkedCount());
    }

    @Test
    public void completingAPickupSavesWhoWasCarriedAndAlertsWithRealStatuses() {
        startActive(TripRepository.PICKUP);
        vm.setStatus(amani, true);
        vm.setStatus(baraka, false);
        repo.parentsWithoutEmail = 1;
        vm.completeTrip();
        assertEquals(TripViewModel.Phase.COMPLETING, phase());

        assertEquals(Collections.singletonList("s1"), repo.carried);
        assertEquals("after pick-up every child leaves the trip record", 3, repo.leftTrip.size());
        repo.completeCallback.onResult(null);

        assertEquals(TripViewModel.Phase.COMPLETED, phase());
        assertEquals(Boolean.TRUE, repo.alertedStatuses.get("s1"));
        assertEquals(Boolean.FALSE, repo.alertedStatuses.get("s2"));
        assertFalse("unmarked children are sent as unconfirmed, not as done",
                repo.alertedStatuses.containsKey("s3"));
        assertEquals(1, vm.getState().getValue().parentsWithoutEmail);
    }

    @Test
    public void completingADropOffOnlyClearsChildrenConfirmedDropped() {
        startActive(TripRepository.DROPOFF);
        vm.setStatus(amani, true);
        vm.setStatus(baraka, false);
        vm.completeTrip();
        assertEquals(1, repo.leftTrip.size());
        assertEquals("s1", repo.leftTrip.get(0).getId());
    }

    @Test
    public void failedCompletionKeepsTheTripOpen() {
        startActive(TripRepository.PICKUP);
        vm.completeTrip();
        repo.completeCallback.onError(new RuntimeException("offline"));
        assertEquals(TripViewModel.Phase.ACTIVE, phase());
        assertNotNull(vm.getError().getValue());
        assertNull("no alerts until the trip is saved", repo.alertedStatuses);
    }

    @Test
    public void loadFailureCanBeRetried() {
        vm.load(details(TripRepository.PICKUP));
        repo.studentsCallback.onError(new RuntimeException("offline"));
        assertEquals(TripViewModel.Phase.LOAD_FAILED, phase());

        vm.load(details(TripRepository.PICKUP));
        assertEquals(TripViewModel.Phase.LOADING, phase());
        repo.studentsCallback.onResult(Arrays.asList(amani));
        repo.openTripCallback.onResult(null);
        assertEquals(TripViewModel.Phase.READY, phase());
    }

    @Test
    public void loadIsNotRepeatedAfterRotation() {
        loadReady(TripRepository.PICKUP);
        Object before = repo.studentsCallback;
        vm.load(details(TripRepository.PICKUP));
        assertTrue(before == repo.studentsCallback);
        assertEquals(TripViewModel.Phase.READY, phase());
    }
}

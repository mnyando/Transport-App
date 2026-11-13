package com.example.transportapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ParentDashboardViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTasks = new InstantTaskExecutorRule();

    /** Records requests and lets each test answer them. */
    static class FakeRepository implements ParentRepository {
        final List<String> childRequests = new ArrayList<>();
        Callback<List<Student>> childCallback;
        Callback<List<Notification>> notificationCallback;
        Callback<BusLocation> busCallback;
        int subscriptions;
        int cancelled;

        @Override
        public void loadChildren(String parentEmail, Callback<List<Student>> callback) {
            childRequests.add(parentEmail);
            childCallback = callback;
        }

        @Override
        public Subscription listenToNotifications(String parentEmail, Callback<List<Notification>> callback) {
            subscriptions++;
            notificationCallback = callback;
            return () -> cancelled++;
        }

        @Override
        public void loadBusLocation(String vehicle, Callback<BusLocation> callback) {
            busCallback = callback;
        }
    }

    private FakeRepository repo;
    private ParentDashboardViewModel vm;
    private static final long NOW = 1_000_000_000L;

    @Before
    public void setUp() {
        repo = new FakeRepository();
        vm = new ParentDashboardViewModel(repo, () -> NOW);
    }

    private static Student child(String name, String vehicle) {
        Student s = new Student();
        s.setName(name);
        s.setVehicle(vehicle);
        return s;
    }

    private static Notification note(long timestamp) {
        Notification n = new Notification();
        n.setTimestamp(timestamp);
        return n;
    }

    @Test
    public void startLoadsChildrenForTheParentEmailOnlyOnce() {
        vm.start("parent@example.com");
        vm.start("parent@example.com"); // e.g. after rotation
        assertEquals(Collections.singletonList("parent@example.com"), repo.childRequests);
        assertEquals(1, repo.subscriptions);
        assertTrue(vm.getChildren().getValue().loading);
    }

    @Test
    public void childrenArrive() {
        vm.start("parent@example.com");
        repo.childCallback.onResult(Arrays.asList(child("Amy", "BUS 1"), child("Ben", "BUS 2")));
        ParentDashboardViewModel.ChildrenState state = vm.getChildren().getValue();
        assertFalse(state.loading);
        assertFalse(state.failed);
        assertEquals(2, state.children.size());
    }

    @Test
    public void childrenFailureKeepsLastKnownList() {
        vm.start("parent@example.com");
        repo.childCallback.onResult(Collections.singletonList(child("Amy", "BUS 1")));
        vm.refreshChildren();
        repo.childCallback.onError(new Exception("offline"));
        ParentDashboardViewModel.ChildrenState state = vm.getChildren().getValue();
        assertTrue(state.failed);
        assertEquals(1, state.children.size());
    }

    @Test
    public void notificationsAreNewestFirstAndCapped() {
        vm.start("parent@example.com");
        List<Notification> many = new ArrayList<>();
        for (int i = 0; i < ParentDashboardViewModel.MAX_NOTIFICATIONS + 10; i++) many.add(note(i));
        repo.notificationCallback.onResult(many);
        List<Notification> shown = vm.getNotifications().getValue();
        assertEquals(ParentDashboardViewModel.MAX_NOTIFICATIONS, shown.size());
        assertEquals(ParentDashboardViewModel.MAX_NOTIFICATIONS + 9, shown.get(0).getTimestamp());
    }

    @Test
    public void notificationErrorIsReported() {
        vm.start("parent@example.com");
        repo.notificationCallback.onError(new Exception("denied"));
        assertTrue(vm.getNotificationsFailed().getValue());
    }

    @Test
    public void listenerIsRemovedWhenScreenCloses() {
        vm.start("parent@example.com");
        vm.onCleared();
        assertEquals(1, repo.cancelled);
    }

    @Test
    public void vehiclesAreDistinctAndSkipBlanks() {
        List<String> vehicles = ParentDashboardViewModel.vehiclesOf(Arrays.asList(
                child("Amy", "BUS 1"), child("Ben", "BUS 1"), child("Cal", " "), child("Dee", null),
                child("Eve", "BUS 2")));
        assertEquals(Arrays.asList("BUS 1", "BUS 2"), vehicles);
    }

    @Test
    public void busOnTripOpensMapOnce() {
        vm.checkBus("BUS 1");
        assertEquals(ParentDashboardViewModel.BusStatus.Kind.CHECKING, vm.getBusStatus().getValue().kind);
        repo.busCallback.onResult(new ParentRepository.BusLocation(1.0, 2.0, true, NOW - 3 * 60_000));
        ParentDashboardViewModel.BusStatus status = vm.getBusStatus().getValue();
        assertNotNull(status);
        assertEquals(ParentDashboardViewModel.BusStatus.Kind.ON_TRIP, status.kind);
        assertEquals(3, status.minutesAgo);
        assertTrue(status.takeOpenMap());
        assertFalse("map must not reopen after rotation", status.takeOpenMap());
    }

    @Test
    public void busWithoutLocationIsNotOnTrip() {
        // After a trip ends the app deletes lat/lng, so an inactive bus has no position.
        ParentDashboardViewModel.BusStatus status = ParentDashboardViewModel.toStatus("BUS 1",
                new ParentRepository.BusLocation(null, null, false, NOW), NOW);
        assertEquals(ParentDashboardViewModel.BusStatus.Kind.NOT_ON_TRIP, status.kind);
        assertFalse(status.takeOpenMap());
    }

    @Test
    public void busMarkedActiveButWithoutCoordinatesIsNotOnTrip() {
        ParentDashboardViewModel.BusStatus status = ParentDashboardViewModel.toStatus("BUS 1",
                new ParentRepository.BusLocation(null, 2.0, true, NOW), NOW);
        assertEquals(ParentDashboardViewModel.BusStatus.Kind.NOT_ON_TRIP, status.kind);
    }

    @Test
    public void busLookupErrorIsUnavailable() {
        vm.checkBus("BUS 1");
        repo.busCallback.onError(new Exception("permission denied"));
        assertEquals(ParentDashboardViewModel.BusStatus.Kind.UNAVAILABLE, vm.getBusStatus().getValue().kind);
    }
}

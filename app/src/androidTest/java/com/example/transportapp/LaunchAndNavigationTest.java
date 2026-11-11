package com.example.transportapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Guards the app's entry flow: MainActivity must launch, and "Get Started" must open the
 * Navigation role picker. Catches regressions such as an activity missing from the manifest
 * (ActivityNotFoundException) or a layout that fails to inflate.
 */
@RunWith(AndroidJUnit4.class)
public class LaunchAndNavigationTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void mainActivity_showsGetStartedButton() {
        onView(withId(R.id.get_started)).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test
    public void getStarted_opensRolePicker() {
        onView(withId(R.id.get_started)).perform(scrollTo(), click());

        // The role picker is a ScrollView, so scroll each button into view before asserting.
        onView(withId(R.id.school_button)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.parent_button)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.driver_button)).perform(scrollTo()).check(matches(isDisplayed()));
    }
}

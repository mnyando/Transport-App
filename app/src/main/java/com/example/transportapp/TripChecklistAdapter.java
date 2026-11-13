package com.example.transportapp;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * The driver's checklist: each child gets two large buttons, "Picked up" / "Not picked"
 * (or "Dropped off" / "Not dropped"). Tapping the selected button again clears it.
 *
 * The adapter holds no status of its own: it reads from and reports to the
 * {@link TripViewModel}. Rows are recycled while scrolling, so the listener is attached once and
 * always resolves the child from the row's current position, and changes made while a row is
 * being filled in are ignored. A recycled row can never record one child's status for another.
 */
public class TripChecklistAdapter extends RecyclerView.Adapter<TripChecklistAdapter.Row> {

    public interface Host {
        @Nullable Boolean statusFor(Student student);

        boolean canMark();

        void onStatusChosen(Student student, @Nullable Boolean status);
    }

    private final List<Student> students = new ArrayList<>();
    private final Host host;
    private final boolean pickup;

    public TripChecklistAdapter(Host host, boolean pickup) {
        this.host = host;
        this.pickup = pickup;
    }

    public void submit(List<Student> list) {
        students.clear();
        students.addAll(list);
        notifyDataSetChanged();
    }

    /** Redraws enabled/disabled state, e.g. once the trip has started. */
    public void refresh() {
        notifyItemRangeChanged(0, students.size());
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_status, parent, false);
        Row row = new Row(view);
        row.done.setText(pickup ? R.string.status_picked_up : R.string.status_dropped_off);
        row.notDone.setText(pickup ? R.string.status_not_picked : R.string.status_not_dropped);
        tint(row.done, R.color.green);
        tint(row.notDone, R.color.red);

        row.group.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (row.binding) return;
            int position = row.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION || position >= students.size()) return;
            // A switch fires "unchecked" for the old button, then "checked" for the new one;
            // only the final state matters.
            int selected = group.getCheckedButtonId();
            if (!isChecked && selected != View.NO_ID) return;
            Boolean status = selected == R.id.doneButton ? Boolean.TRUE
                    : selected == R.id.notDoneButton ? Boolean.FALSE : null;
            host.onStatusChosen(students.get(position), status);
        });
        return row;
    }

    /** Selected buttons fill with the colour; unselected ones show it as an outline. */
    private static void tint(MaterialButton button, int colorRes) {
        int color = ContextCompat.getColor(button.getContext(), colorRes);
        int white = ContextCompat.getColor(button.getContext(), R.color.white);
        int text = ContextCompat.getColor(button.getContext(), R.color.text_primary);
        int[][] states = {{android.R.attr.state_checked}, {}};
        button.setBackgroundTintList(new ColorStateList(states, new int[]{color, 0x00000000}));
        button.setTextColor(new ColorStateList(states, new int[]{white, text}));
        button.setStrokeColor(ColorStateList.valueOf(color));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        Student student = students.get(position);
        row.name.setText(student.getName());
        String grade = student.getGrade();
        row.detail.setVisibility(grade == null || grade.isEmpty() ? View.GONE : View.VISIBLE);
        row.detail.setText(grade);

        row.binding = true;
        Boolean status = host.statusFor(student);
        if (Boolean.TRUE.equals(status)) row.group.check(R.id.doneButton);
        else if (Boolean.FALSE.equals(status)) row.group.check(R.id.notDoneButton);
        else row.group.clearChecked();
        boolean enabled = host.canMark();
        row.done.setEnabled(enabled);
        row.notDone.setEnabled(enabled);
        row.binding = false;
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class Row extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView detail;
        final MaterialButtonToggleGroup group;
        final MaterialButton done;
        final MaterialButton notDone;
        boolean binding;

        Row(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.studentNameTextView);
            detail = itemView.findViewById(R.id.studentDetailTextView);
            group = itemView.findViewById(R.id.statusGroup);
            done = itemView.findViewById(R.id.doneButton);
            notDone = itemView.findViewById(R.id.notDoneButton);
        }
    }
}

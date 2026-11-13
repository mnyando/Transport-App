package com.example.transportapp;

import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;

/** The eye button next to a password field: shows or hides the password, and says which. */
final class PasswordToggle {

    private PasswordToggle() {}

    static void attach(ImageView toggle, EditText password) {
        toggle.setClickable(true);
        toggle.setFocusable(true);
        toggle.setContentDescription(toggle.getContext().getString(R.string.show_password));
        toggle.setOnClickListener(v -> {
            boolean visible = (password.getInputType() & InputType.TYPE_MASK_VARIATION)
                    == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            password.setInputType(InputType.TYPE_CLASS_TEXT | (visible
                    ? InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
            password.setSelection(password.getText().length());
            toggle.setContentDescription(toggle.getContext().getString(
                    visible ? R.string.show_password : R.string.hide_password));
        });
    }
}

package com.example.guysapp;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.LayoutInflater;
public class EditProfileDialog {

    public interface OnSaveClickListener {
        void onSave(String firstName, String lastName);
    }

    public interface OnImageClickListener {
        void onImageClick();
    }

    private Context context;
    private AlertDialog dialog;

    private EditText etFirstName;
    private EditText etLastName;
    private ImageView imageView;

    private OnSaveClickListener saveListener;
    private OnImageClickListener imageClickListener;

    public EditProfileDialog(
            Context context,
            String fullName,
            Drawable currentImage,
            OnSaveClickListener saveListener,
            OnImageClickListener imageClickListener
    ) {

        this.context = context;
        this.saveListener = saveListener;
        this.imageClickListener = imageClickListener;

        buildDialog(fullName, currentImage);
    }

    private void buildDialog(String fullName, Drawable currentImage) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_edit_profile, null);

        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        imageView = view.findViewById(R.id.imgProfile);

        if (currentImage != null) {
            imageView.setImageDrawable(currentImage);
        }

        imageView.setOnClickListener(v -> {
            if (imageClickListener != null) {
                imageClickListener.onImageClick();
            }
        });

        fillName(fullName);

        builder.setView(view);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel",
                (d, w) -> d.dismiss());

        dialog = builder.create();

        dialog.setOnShowListener(d -> {

            Button btnSave =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnSave.setOnClickListener(v -> {

                String first =
                        etFirstName.getText().toString().trim();

                String last =
                        etLastName.getText().toString().trim();

                if (first.isEmpty() || last.isEmpty()) {
                    Toast.makeText(context,
                            "Please enter full name",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (saveListener != null) {
                    saveListener.onSave(first, last);
                }
            });
        });
    }
    private void fillName(String fullName) {

        if (fullName == null || fullName.trim().isEmpty()) {
            return;
        }

        String[] parts = fullName.trim().split(" ");

        if (parts.length > 0) {
            etFirstName.setText(parts[0]);
        }

        if (parts.length > 1) {

            StringBuilder lastNameBuilder =
                    new StringBuilder();

            for (int i = 1; i < parts.length; i++) {

                lastNameBuilder
                        .append(parts[i])
                        .append(" ");
            }

            etLastName.setText(
                    lastNameBuilder.toString().trim()
            );
        }
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public ImageView getImageView() {
        return imageView;
    }

    public Button getSaveButton() {
        return dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    }
}
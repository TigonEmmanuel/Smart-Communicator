package com.example.smartcommunicator.ui.list;

// REMOVED: Unnecessary imports are gone (Manifest, Context, PackageManager)
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcommunicator.R;
import com.example.smartcommunicator.model.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ContactViewHolder> {

    private final List<Contact> contactList;
    private final int[] avatarColors;
    private final Random random = new Random();

    private boolean isMultiSelectMode = false;
    private final List<Contact> selectedItems = new ArrayList<>();
    private final MultiSelectListener multiSelectListener;

    // --- UPDATED INTERFACE: Added a new method for call requests ---
    public interface MultiSelectListener {
        void onMultiSelectStateChanged(boolean isEnabled);
        void onItemSelectionChanged(int selectedCount);
        void onCallRequested(Contact contact); // NEW METHOD
    }

    public ContactListAdapter(List<Contact> contactList, int[] avatarColors, MultiSelectListener listener) {
        this.contactList = contactList;
        this.avatarColors = avatarColors;
        this.multiSelectListener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contactList != null ? contactList.size() : 0;
    }

    // (Methods like startMultiSelectMode, stopMultiSelectMode, etc. are unchanged)
    public void startMultiSelectMode() {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true;
            multiSelectListener.onMultiSelectStateChanged(true);
        }
    }

    public void stopMultiSelectMode() {
        isMultiSelectMode = false;
        selectedItems.clear();
        multiSelectListener.onMultiSelectStateChanged(false);
        notifyDataSetChanged();
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public List<Contact> getSelectedItems() {
        return selectedItems;
    }


    public class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView contactInitial;
        TextView contactName;
        ImageButton callButton;
        ImageButton messageButton;
        View itemView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            contactInitial = itemView.findViewById(R.id.contact_initial);
            contactName = itemView.findViewById(R.id.contact_name);
            callButton = itemView.findViewById(R.id.button_call);
            messageButton = itemView.findViewById(R.id.button_message);
        }

        public void bind(Contact contact) {
            contactName.setText(contact.getName());

            if (contact.getName() != null && !contact.getName().isEmpty()) {
                contactInitial.setText(String.valueOf(contact.getName().charAt(0)).toUpperCase());
            } else {
                contactInitial.setText("?");
            }

            int randomColor = avatarColors[random.nextInt(avatarColors.length)];
            GradientDrawable background = (GradientDrawable) contactInitial.getBackground();
            background.setColor(randomColor);

            itemView.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    toggleSelection(contact);
                } else {
                    Toast.makeText(v.getContext(), "Clicked on " + contact.getName(), Toast.LENGTH_SHORT).show();
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (!isMultiSelectMode) {
                    startMultiSelectMode();
                    toggleSelection(contact);
                }
                return true;
            });

            if (selectedItems.contains(contact)) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.selected_item_background));
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            // --- SIMPLIFIED CALL BUTTON LOGIC ---
            callButton.setOnClickListener(v -> {
                // The adapter's only job is to tell the fragment that the button was clicked.
                // It passes the specific contact to the listener.
                multiSelectListener.onCallRequested(contact);
            });

            // --- Message Button Logic (Unchanged) ---
            messageButton.setOnClickListener(v -> {
                String phoneNumber = contact.getNumber();
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                    v.getContext().startActivity(smsIntent);
                } else {
                    Toast.makeText(v.getContext(), "No phone number available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void toggleSelection(Contact contact) {
            if (selectedItems.contains(contact)) {
                selectedItems.remove(contact);
            } else {
                selectedItems.add(contact);
            }
            multiSelectListener.onItemSelectionChanged(selectedItems.size());
            notifyItemChanged(getAdapterPosition());
        }
    }
}

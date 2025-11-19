package com.example.smartcommunicator.ui.list;

import android.content.Intent;
import android.graphics.Color; // Import Color
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcommunicator.R;
import com.example.smartcommunicator.model.Contact;

import java.util.ArrayList; // Import ArrayList
import java.util.List;
import java.util.Random;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ContactViewHolder> {

    private final List<Contact> contactList;
    private final int[] avatarColors;
    private final Random random = new Random();

    // --- NEW: VARIABLES FOR SELECTION ---
    private boolean isMultiSelectMode = false;
    private final List<Contact> selectedItems = new ArrayList<>();
    private final MultiSelectListener multiSelectListener;

    // --- NEW: INTERFACE TO COMMUNICATE WITH FRAGMENT ---
    public interface MultiSelectListener {
        void onMultiSelectStateChanged(boolean isEnabled);
        void onItemSelectionChanged(int selectedCount);
    }

    public ContactListAdapter(List<Contact> contactList, int[] avatarColors, MultiSelectListener listener) {
        this.contactList = contactList;
        this.avatarColors = avatarColors;
        this.multiSelectListener = listener; // Store the listener
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

    // --- NEW: METHODS TO MANAGE SELECTION MODE ---

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
        notifyDataSetChanged(); // Refresh all items to remove highlights
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
        View itemView; // Reference to the whole item view for background changes

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView; // Store the view
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

            // --- UPDATED: HANDLE CLICKS DIFFERENTLY IN SELECT MODE ---
            itemView.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    toggleSelection(contact);
                } else {
                    // Normal click action (optional, can navigate to contact details later)
                    Toast.makeText(v.getContext(), "Clicked on " + contact.getName(), Toast.LENGTH_SHORT).show();
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (!isMultiSelectMode) {
                    startMultiSelectMode();
                    toggleSelection(contact);
                }
                return true; // Consume the long click
            });

            // Set the background highlight if the item is selected
            if (selectedItems.contains(contact)) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.selected_item_background));
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT); // Default transparent background
            }

            // --- Button Listeners (No change here) ---
            callButton.setOnClickListener(v -> {
                String phoneNumber = contact.getNumber();
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:" + phoneNumber));
                    v.getContext().startActivity(dialIntent);
                } else {
                    Toast.makeText(v.getContext(), "No phone number available", Toast.LENGTH_SHORT).show();
                }
            });

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
            // Update the title of the action mode
            multiSelectListener.onItemSelectionChanged(selectedItems.size());
            // Redraw the item to show/hide highlight
            notifyItemChanged(getAdapterPosition());
        }
    }
}

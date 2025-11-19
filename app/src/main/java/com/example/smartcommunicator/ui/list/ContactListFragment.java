package com.example.smartcommunicator.ui.list;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode; // Import ActionMode
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcommunicator.R;
import com.example.smartcommunicator.model.Contact;

import java.util.ArrayList;
import java.util.List;

// --- IMPLEMENT THE NEW LISTENER INTERFACE ---
public class ContactListFragment extends Fragment implements ContactListAdapter.MultiSelectListener {

    private RecyclerView recyclerView;
    private ContactListAdapter adapter;
    private final List<Contact> displayedContactList = new ArrayList<>();
    private final List<Contact> fullContactList = new ArrayList<>();
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private TextView textViewNotFound;

    // --- NEW: VARIABLES FOR ACTION MODE ---
    private ActionMode actionMode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted && getContext() != null) {
                loadContacts();
            } else if (getContext() != null) {
                Toast.makeText(getContext(), "Permission to read contacts denied.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contact_list, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar_contact_list);
        recyclerView = root.findViewById(R.id.recycler_view_contacts);
        textViewNotFound = root.findViewById(R.id.text_view_not_found);

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        }

        setupRecyclerView(); // The adapter is created here
        checkPermissionAndLoadContacts();
        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.contact_list_toolbar_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterList(query);
                        return false;
                    }
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterList(newText);
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_add) {
            NavHostFragment.findNavController(this).navigate(R.id.navigation_contact);
            return true;
        } else if (itemId == R.id.action_delete) {
            // Start the multi-select mode when user clicks "Delete"
            adapter.startMultiSelectMode();
            return true;
        } else if (itemId == R.id.action_feedback) {
            sendFeedbackEmail();
            return true;
        } else if (itemId == R.id.action_share) {
            shareTheApp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --- IMPLEMENTATION OF THE LISTENER INTERFACE ---
    @Override
    public void onMultiSelectStateChanged(boolean isEnabled) {
        if (isEnabled) {
            if (actionMode == null) {
                // Start the action mode
                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
            }
        } else {
            if (actionMode != null) {
                actionMode.finish(); // This will call onDestroyActionMode
            }
        }
    }

    @Override
    public void onItemSelectionChanged(int selectedCount) {
        if (actionMode != null) {
            // Update the title to show the count
            actionMode.setTitle(selectedCount + " selected");
        }
    }

    // --- NEW: ACTION MODE CALLBACK ---
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the new contextual menu
            mode.getMenuInflater().inflate(R.menu.contextual_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete_items) {
                // Show a confirmation dialog before deleting
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Contacts")
                        .setMessage("Are you sure you want to delete the selected contacts? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteSelectedContacts();
                            mode.finish(); // Close the action mode
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // When action mode is closed (e.g., by back button), tell the adapter to stop
            adapter.stopMultiSelectMode();
            actionMode = null;
        }
    };


    private void setupRecyclerView() {
        // --- Pass 'this' as the listener to the adapter ---
        adapter = new ContactListAdapter(displayedContactList, requireContext().getResources().getIntArray(R.array.avatar_colors), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    // --- NEW: METHODS FOR THE ACTIONS ---

    private void deleteSelectedContacts() {
        // IMPORTANT: This is a simulation. It removes from the list but not the phone's actual contacts database.
        // Deleting from the phone's database is much more complex and can be destructive.
        List<Contact> selected = adapter.getSelectedItems();
        fullContactList.removeAll(selected);
        filterList(""); // Refresh the list
        Toast.makeText(getContext(), selected.size() + " contacts deleted.", Toast.LENGTH_SHORT).show();
    }

    private void sendFeedbackEmail() {
        // *** PUT YOUR EMAIL HERE ***
        String myEmailAddress = "communicatorsmart@gmail.com";
        String subject = "Feedback for Smart Communicator App";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // Only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{myEmailAddress});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        try {
            startActivity(Intent.createChooser(intent, "Send Feedback..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareTheApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Check out this cool app: [Your App's Google Play Store Link Here]";
        String shareSub = "Smart Communicator App";
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share using"));
    }


    // --- UNCHANGED METHODS ---

    private void checkPermissionAndLoadContacts() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadContacts();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void loadContacts() {
        if (getContext() == null) return;
        fullContactList.clear();
        String[] projection = { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER };
        try (Cursor cursor = requireContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    String number = cursor.getString(numberIndex);
                    if (name != null && number != null) {
                        fullContactList.add(new Contact(name, number));
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        filterList("");
    }

    private void filterList(String query) {
        displayedContactList.clear();
        if (query == null || query.isEmpty()) {
            displayedContactList.addAll(fullContactList);
            if (textViewNotFound != null) {
                textViewNotFound.setVisibility(View.GONE);
            }
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Contact contact : fullContactList) {
                if ((contact.getName() != null && contact.getName().toLowerCase().contains(lowerCaseQuery)) ||
                        (contact.getNumber() != null && contact.getNumber().replaceAll("\\s", "").contains(lowerCaseQuery))) {
                    displayedContactList.add(contact);
                }
            }
            if (textViewNotFound != null) {
                textViewNotFound.setVisibility(displayedContactList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}

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
import androidx.appcompat.view.ActionMode;
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
import java.util.Map;

public class ContactListFragment extends Fragment implements ContactListAdapter.MultiSelectListener {

    private RecyclerView recyclerView;
    private ContactListAdapter adapter;
    private final List<Contact> displayedContactList = new ArrayList<>();
    private final List<Contact> fullContactList = new ArrayList<>();
    private ActivityResultLauncher<String[]> requestContactPermissionsLauncher;
    private TextView textViewNotFound;
    private ActionMode actionMode;

    private ActivityResultLauncher<String> requestCallPermissionLauncher;
    private Contact pendingCallContact;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        requestContactPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean readGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_CONTACTS));
                    if (readGranted) {
                        loadContacts();
                    } else {
                        Toast.makeText(getContext(), "Permission to read contacts denied.", Toast.LENGTH_LONG).show();
                    }
                    boolean writeGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.WRITE_CONTACTS));
                    if (!writeGranted) {
                        Toast.makeText(getContext(), "Delete functionality is disabled.", Toast.LENGTH_SHORT).show();
                    }
                });

        requestCallPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (pendingCallContact != null) {
                            initiatePhoneCall(pendingCallContact);
                        }
                    } else {
                        Toast.makeText(getContext(), "Call permission denied.", Toast.LENGTH_SHORT).show();
                    }
                    pendingCallContact = null;
                });
    }

    @Override
    public void onCallRequested(Contact contact) {
        initiatePhoneCall(contact);
    }

    private void initiatePhoneCall(Contact contact) {
        if (getContext() == null || contact == null || contact.getNumber() == null || contact.getNumber().trim().isEmpty()) {
            Toast.makeText(getContext(), "No phone number available.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + contact.getNumber()));
            try {
                startActivity(callIntent);
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Error placing call.", Toast.LENGTH_SHORT).show();
            }
        } else {
            pendingCallContact = contact;
            requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
        }
    }

    private void checkPermissionAndLoadContacts() {
        if (getContext() == null) return;
        boolean hasReadPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        boolean hasWritePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        if (hasReadPermission) {
            loadContacts();
        }

        if (!hasReadPermission || !hasWritePermission) {
            requestContactPermissionsLauncher.launch(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS});
        }
    }

    // =======================================================================
    // ==     THE ONLY CHANGE IS IN THE `onOptionsItemSelected` METHOD      ==
    // =======================================================================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // --- THIS IS THE FIX ---
        // I have restored the navigation logic that was here before.
        if (itemId == R.id.action_add) {
            NavHostFragment.findNavController(this).navigate(R.id.navigation_contact);
            return true;
        } else if (itemId == R.id.action_delete) {
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

    // =======================================================================
    // ==     THE REST OF THE FILE REMAINS THE SAME, NO CHANGES BELOW       ==
    // =======================================================================

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
        setupRecyclerView();
        checkPermissionAndLoadContacts();
        return root;
    }

    private void deleteSelectedContacts() {
        List<Contact> selected = adapter.getSelectedItems();
        if (selected.isEmpty() || getContext() == null) { return; }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Permission to write contacts is required to delete.", Toast.LENGTH_LONG).show();
            return;
        }
        int deletedCount = 0;
        for (Contact contact : selected) {
            if (contact.getLookupKey() == null) continue;
            Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contact.getLookupKey());
            try {
                int rowsDeleted = requireContext().getContentResolver().delete(contactUri, null, null);
                if (rowsDeleted > 0) {
                    deletedCount++;
                }
            } catch (SecurityException e) {
                Toast.makeText(getContext(), "Could not delete contact: " + contact.getName(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        fullContactList.removeAll(selected);
        filterList("");
        Toast.makeText(getContext(), deletedCount + " contacts permanently deleted.", Toast.LENGTH_SHORT).show();
    }

    private void loadContacts() {
        if (getContext() == null) return;
        fullContactList.clear();
        String[] projection = { ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER };
        try (Cursor cursor = requireContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cursor != null) {
                int lookupKeyIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (cursor.moveToNext()) {
                    String lookupKey = cursor.getString(lookupKeyIndex);
                    String name = cursor.getString(nameIndex);
                    String number = cursor.getString(numberIndex);
                    if (lookupKey != null && name != null && number != null) {
                        fullContactList.add(new Contact(lookupKey, name, number));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load contacts.", Toast.LENGTH_SHORT).show();
        }
        filterList("");
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
    public void onMultiSelectStateChanged(boolean isEnabled) {
        if (isEnabled) {
            if (actionMode == null) {
                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
            }
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    @Override
    public void onItemSelectionChanged(int selectedCount) {
        if (actionMode != null) {
            actionMode.setTitle(selectedCount + " selected");
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_action_menu, menu);
            return true;
        }
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete_items) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Contacts")
                        .setMessage("Are you sure you want to permanently delete the selected contacts? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteSelectedContacts();
                            mode.finish();
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
            adapter.stopMultiSelectMode();
            actionMode = null;
        }
    };

    private void setupRecyclerView() {
        adapter = new ContactListAdapter(displayedContactList, requireContext().getResources().getIntArray(R.array.avatar_colors), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void sendFeedbackEmail() {
        String myEmailAddress = "communicatorsmart@gmail.com";
        String subject = "Feedback for Smart Communicator App";
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
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
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share using"));
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

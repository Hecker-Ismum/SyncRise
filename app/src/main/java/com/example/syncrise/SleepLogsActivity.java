package com.example.syncrise;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.syncrise.databinding.ActivitySleepLogsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SleepLogsActivity extends AppCompatActivity {

    private static final String TAG = "SleepLogsActivity";
    private ActivitySleepLogsBinding binding;
    private SleepLogAdapter adapter;
    private List<SleepLogModel> sleepLogsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySleepLogsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        binding.rvSleepLogs.setLayoutManager(new LinearLayoutManager(this));
        sleepLogsList = new ArrayList<>();
        adapter = new SleepLogAdapter(sleepLogsList);
        binding.rvSleepLogs.setAdapter(adapter);

        fetchSleepLogs();
    }

    private void fetchSleepLogs() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            binding.progressBarLogs.setVisibility(View.GONE);
            binding.tvNoLogs.setVisibility(View.VISIBLE);
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("sleepLogs")
                .orderBy("endTimeMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBarLogs.setVisibility(View.GONE);
                    sleepLogsList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        binding.tvNoLogs.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvNoLogs.setVisibility(View.GONE);
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            SleepLogModel log = document.toObject(SleepLogModel.class);
                            sleepLogsList.add(log);
                        }
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBarLogs.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching sleep logs", e);
                    Toast.makeText(SleepLogsActivity.this, "Failed to load sleep history.", Toast.LENGTH_SHORT).show();
                });
    }
}
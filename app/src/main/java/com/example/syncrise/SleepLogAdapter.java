package com.example.syncrise;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncrise.databinding.ItemSleepLogBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SleepLogAdapter extends RecyclerView.Adapter<SleepLogAdapter.SleepLogViewHolder> {

    private final List<SleepLogModel> sleepLogs;

    public SleepLogAdapter(List<SleepLogModel> sleepLogs) {
        this.sleepLogs = sleepLogs;
    }

    @NonNull
    @Override
    public SleepLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSleepLogBinding binding = ItemSleepLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SleepLogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SleepLogViewHolder holder, int position) {
        holder.bind(sleepLogs.get(position));
    }

    @Override
    public int getItemCount() {
        return sleepLogs != null ? sleepLogs.size() : 0;
    }

    public static class SleepLogViewHolder extends RecyclerView.ViewHolder {
        private final ItemSleepLogBinding binding;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

        public SleepLogViewHolder(ItemSleepLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SleepLogModel log) {
            binding.tvDate.setText(dateFormat.format(new Date(log.getEndTimeMillis())));
            binding.tvCycles.setText("Cycles Completed: " + log.getCyclesCompleted());

            long durationMinutes = log.getDurationMillis() / (1000 * 60);
            long hours = durationMinutes / 60;
            long mins = durationMinutes % 60;
            binding.tvDuration.setText(hours + "h " + mins + "m");
            
            itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(itemView.getContext(), SleepQualityActivity.class);
                intent.putExtra("DURATION_MILLIS", log.getDurationMillis());
                itemView.getContext().startActivity(intent);
            });
        }
    }
}
package com.example.syncrise;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncrise.databinding.ItemAlarmBinding;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    // Define listener interface for toggle events
    public interface OnAlarmToggleListener {
        void onToggle(AlarmModel alarm, boolean isChecked);
    }

    private final List<AlarmModel> alarmList;
    private final OnAlarmToggleListener listener;

    public AlarmAdapter(List<AlarmModel> alarmList, OnAlarmToggleListener listener) {
        this.alarmList = alarmList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlarmBinding binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AlarmViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmModel alarm = alarmList.get(position);
        holder.bind(alarm, listener);
    }

    @Override
    public int getItemCount() {
        return alarmList != null ? alarmList.size() : 0;
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        private final ItemAlarmBinding binding;

        public AlarmViewHolder(ItemAlarmBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AlarmModel alarm, OnAlarmToggleListener listener) {
            binding.tvAlarmTime.setText(alarm.getTime());
            if (alarm.getLabel() != null && !alarm.getLabel().isEmpty()) {
                binding.tvMissionType.setText(alarm.getLabel() + " • Mission: " + alarm.getMissionType());
            } else {
                binding.tvMissionType.setText("Mission: " + alarm.getMissionType());
            }

            // Update badge depending on alarm type
            if (alarm.isCycleAlarm()) {
                binding.tvAlarmTypeBadge.setText("CYCLE: " + alarm.getCycleDurationHours() + "h");
                binding.tvAlarmTypeBadge.setBackgroundColor(Color.parseColor("#4A90E2")); // Sleepy Blue
            } else {
                binding.tvAlarmTypeBadge.setText("REGULAR");
                binding.tvAlarmTypeBadge.setBackgroundColor(Color.parseColor("#9E9E9E")); // Grey
            }

            // Temporarily remove listener to prevent unwanted triggers during binding
            binding.switchAlarm.setOnCheckedChangeListener(null);
            binding.switchAlarm.setChecked(alarm.isEnabled());

            // Handle toggle changes
            binding.switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                alarm.setEnabled(isChecked);
                if (listener != null) {
                    listener.onToggle(alarm, isChecked);
                }
            });
        }
    }
}
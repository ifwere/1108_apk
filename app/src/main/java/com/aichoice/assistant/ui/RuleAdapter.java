package com.aichoice.assistant.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aichoice.assistant.R;
import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.Target;
import com.aichoice.assistant.databinding.ItemRuleBinding;
import com.aichoice.assistant.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the ordered rule list, with enable toggle, reordering, edit and delete.
 */
public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.RuleViewHolder> {

    public interface Listener {
        void onRuleEnabledChanged(Rule rule, boolean enabled);

        void onRuleEdit(Rule rule);

        void onRuleDelete(Rule rule);

        void onRuleMove(Rule rule, int delta);
    }

    private final List<Rule> items = new ArrayList<>();
    private final Context context;
    private Listener listener;

    public RuleAdapter(Context context) {
        this.context = context;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Rule> rules) {
        items.clear();
        if (rules != null) {
            items.addAll(rules);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRuleBinding binding = ItemRuleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RuleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        Rule rule = items.get(position);
        ItemRuleBinding binding = holder.binding;
        boolean enabled = rule.isEnabled();
        boolean deepSeek = rule.getTarget() == Target.DEEPSEEK;

        binding.tvBadge.setText(rule.getTarget().shortName());
        // Mirror the main screen: the same brand gradient and text color as the
        // recommendation card for each target.
        binding.tvBadge.setBackgroundResource(deepSeek
                ? R.drawable.bg_badge
                : R.drawable.bg_badge_workbuddy);
        binding.tvBadge.setTextColor(ContextCompat.getColor(context,
                deepSeek ? R.color.white : R.color.on_workbuddy));

        String name = rule.getName().trim();
        binding.tvName.setText(name.isEmpty()
                ? context.getString(R.string.rule_untitled)
                : name);
        binding.tvSummary.setText(buildSummary(rule));
        binding.tvPriority.setText(
                context.getString(R.string.rule_priority, position + 1));

        binding.tvName.setAlpha(enabled ? 1f : 0.45f);
        binding.tvSummary.setAlpha(enabled ? 1f : 0.45f);
        binding.tvBadge.setAlpha(enabled ? 1f : 0.45f);

        binding.switchEnabled.setOnCheckedChangeListener(null);
        binding.switchEnabled.setChecked(enabled);
        binding.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onRuleEnabledChanged(rule, isChecked);
            }
        });

        boolean canMoveUp = position > 0;
        boolean canMoveDown = position < items.size() - 1;
        binding.btnMoveUp.setEnabled(canMoveUp);
        binding.btnMoveUp.setAlpha(canMoveUp ? 1f : 0.25f);
        binding.btnMoveDown.setEnabled(canMoveDown);
        binding.btnMoveDown.setAlpha(canMoveDown ? 1f : 0.25f);

        binding.getRoot().setOnClickListener(view -> {
            if (listener != null) {
                listener.onRuleEdit(rule);
            }
        });
        binding.btnEdit.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRuleEdit(rule);
            }
        });
        binding.btnDelete.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRuleDelete(rule);
            }
        });
        binding.btnMoveUp.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRuleMove(rule, -1);
            }
        });
        binding.btnMoveDown.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRuleMove(rule, 1);
            }
        });
    }

    private String buildSummary(Rule rule) {
        StringBuilder summary = new StringBuilder();
        summary.append(TimeUtils.formatDays(rule.getDays()));
        summary.append(" · ");
        summary.append(TimeUtils.formatRange(rule.getStartMinute(), rule.getEndMinute()));
        if (rule.isSkipHolidays()) {
            summary.append(" · ");
            summary.append(context.getString(R.string.rule_summary_skip_holidays));
        }
        return summary.toString();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class RuleViewHolder extends RecyclerView.ViewHolder {
        final ItemRuleBinding binding;

        RuleViewHolder(ItemRuleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

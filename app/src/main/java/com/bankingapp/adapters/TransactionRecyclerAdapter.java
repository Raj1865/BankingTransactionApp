package com.bankingapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bankingapp.R;
import com.bankingapp.models.Transaction;
import com.bankingapp.views.TransactionCardView;

import java.util.ArrayList;
import java.util.List;

public class TransactionRecyclerAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── View mode enum ────────────────────────────────────────────────────
    public enum ViewMode { LIST, GRID }

    // ── Item type constants ───────────────────────────────────────────────
    private static final int TYPE_LIST = 0;
    private static final int TYPE_GRID = 1;

    // ── Click callback ────────────────────────────────────────────────────
    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    private List<Transaction>   items    = new ArrayList<>();
    private ViewMode            viewMode = ViewMode.LIST;
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    // ── Switch view mode ──────────────────────────────────────────────────
    public void setViewMode(ViewMode mode) {
        if (this.viewMode == mode) return;
        this.viewMode = mode;
        notifyDataSetChanged();
    }

    public ViewMode getViewMode() { return viewMode; }

    // ── Submit new list using DiffUtil ────────────────────────────────────
    public void submitList(List<Transaction> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int o, int n) {
                return items.get(o).getId() == newList.get(n).getId();
            }

            @Override
            public boolean areContentsTheSame(int o, int n) {
                return items.get(o).getFormattedAmount()
                        .equals(newList.get(n).getFormattedAmount());
            }
        });
        items = new ArrayList<>(newList);
        diff.dispatchUpdatesTo(this);
    }

    // ── RecyclerView.Adapter overrides ────────────────────────────────────
    @Override
    public int getItemViewType(int position) {
        return viewMode == ViewMode.LIST ? TYPE_LIST : TYPE_GRID;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LIST) {
            View v = inf.inflate(R.layout.item_transaction_list, parent, false);
            return new ListViewHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_transaction_grid, parent, false);
            return new GridViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Transaction txn = items.get(position);

        if (holder instanceof ListViewHolder) {
            ((ListViewHolder) holder).bind(txn);
        } else {
            ((GridViewHolder) holder).bind(txn);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(txn);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── List ViewHolder ───────────────────────────────────────────────────
    static class ListViewHolder extends RecyclerView.ViewHolder {
        TransactionCardView cardView;

        ListViewHolder(View v) {
            super(v);
            cardView = v.findViewById(R.id.txnCardView);
        }

        void bind(Transaction txn) {
            cardView.setTransaction(txn);
        }
    }

    // ── Grid ViewHolder ───────────────────────────────────────────────────
    static class GridViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvDesc, tvAmount, tvDate;

        GridViewHolder(View v) {
            super(v);
            tvIcon   = v.findViewById(R.id.tvGridIcon);
            tvDesc   = v.findViewById(R.id.tvGridDesc);
            tvAmount = v.findViewById(R.id.tvGridAmount);
            tvDate   = v.findViewById(R.id.tvGridDate);
        }

        void bind(Transaction txn) {
            String icon;
            int    color;

            switch (txn.getType()) {
                case Transaction.TYPE_RECEIVED:
                    icon = "R"; color = Color.parseColor("#43A047"); break;
                case Transaction.TYPE_BILL_PAYMENT:
                    icon = "B"; color = Color.parseColor("#FB8C00"); break;
                default:
                    icon = "S"; color = Color.parseColor("#E53935"); break;
            }

            tvIcon.setText(icon);
            tvIcon.setBackgroundColor(color);
            tvDesc.setText(txn.getDescription());
            tvAmount.setText(txn.getFormattedAmount());
            tvAmount.setTextColor(color);
            tvDate.setText(txn.getDisplayDate());
        }
    }
}
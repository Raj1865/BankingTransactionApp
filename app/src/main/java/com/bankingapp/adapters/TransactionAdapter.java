package com.bankingapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.bankingapp.R;
import com.bankingapp.models.Transaction;
import com.bankingapp.views.TransactionCardView;

import java.util.List;

public class TransactionAdapter extends ArrayAdapter<Transaction> {

    private final Context           context;
    private final List<Transaction> transactions;

    private static class ViewHolder {
        TransactionCardView cardView;
    }

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        super(context, R.layout.item_transaction_card, transactions);
        this.context      = context;
        this.transactions = transactions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView     = LayoutInflater.from(context)
                    .inflate(R.layout.item_transaction_card, parent, false);
            holder          = new ViewHolder();
            holder.cardView = convertView.findViewById(R.id.txnCardView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.cardView.setTransaction(transactions.get(position));
        return convertView;
    }
}
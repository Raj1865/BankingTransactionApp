package com.bankingapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bankingapp.R;
import com.bankingapp.models.Transaction;

import java.util.List;

public class TransactionAdapter extends ArrayAdapter<Transaction> {

    private final Context            context;
    private final List<Transaction>  transactions;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        super(context, R.layout.item_transaction, transactions);
        this.context      = context;
        this.transactions = transactions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Inflate layout only if not already inflated (View recycling)
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_transaction, parent, false);
        }

        // Get the transaction for this row
        Transaction txn = transactions.get(position);

        // Bind Views
        TextView tvTypeIcon    = convertView.findViewById(R.id.tvTxnTypeIcon);
        TextView tvDescription = convertView.findViewById(R.id.tvTxnDescription);
        TextView tvDate        = convertView.findViewById(R.id.tvTxnDate);
        TextView tvAmount      = convertView.findViewById(R.id.tvTxnAmount);

        // Set description and date
        tvDescription.setText(txn.getDescription());
        tvDate.setText(txn.getDisplayDate());

        // Set amount with color
        tvAmount.setText(txn.getFormattedAmount());

        // Colour-code based on transaction type
        switch (txn.getType()) {
            case Transaction.TYPE_RECEIVED:
                tvTypeIcon.setText("R");
                tvTypeIcon.setBackgroundColor(Color.parseColor("#43A047"));
                tvAmount.setTextColor(Color.parseColor("#43A047"));
                break;
            case Transaction.TYPE_BILL_PAYMENT:
                tvTypeIcon.setText("B");
                tvTypeIcon.setBackgroundColor(Color.parseColor("#FB8C00"));
                tvAmount.setTextColor(Color.parseColor("#E53935"));
                break;
            default: // TYPE_SENT
                tvTypeIcon.setText("S");
                tvTypeIcon.setBackgroundColor(Color.parseColor("#E53935"));
                tvAmount.setTextColor(Color.parseColor("#E53935"));
                break;
        }

        return convertView;
    }
}

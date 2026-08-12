package com.example.apartmanyonetim.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apartmanyonetim.R;
import com.example.apartmanyonetim.models.Transaction; // Assuming Transaction model exists
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private List<Transaction> expenseList;

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Transaction expense);
    }

    public ExpenseAdapter(List<Transaction> expenseList, OnItemClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction expense = expenseList.get(position);
        
        holder.tvTitle.setText(expense.getDescription());
        // Localize Category Name
        // Fix: If saved category is in Dutch (e.g. "Vaste Lasten") but app is in TR, or vice versa
        String category = expense.getCategory();
        android.content.Context context = holder.itemView.getContext();
        
        // Map Dutch to Resource ID
        if ("Vaste Lasten".equalsIgnoreCase(category)) {
            category = context.getString(R.string.expense_type_fixed);
        } else if ("Extra".equalsIgnoreCase(category)) {
             category = context.getString(R.string.expense_type_extra);
        } else if ("Demirbaş".equalsIgnoreCase(category) || "Vaste Apparatuur".equalsIgnoreCase(category)) {
             category = context.getString(R.string.expense_type_fixture);
        } else if ("Diğer".equalsIgnoreCase(category) || "Overige".equalsIgnoreCase(category)) {
             category = context.getString(R.string.expense_type_other);
        }
        
        // Map Turkish to Resource ID (if in NL mode)
        if ("Sabit Gider".equalsIgnoreCase(category)) {
            category = context.getString(R.string.expense_type_fixed);
        } else if ("Ekstra".equalsIgnoreCase(category)) {
             category = context.getString(R.string.expense_type_extra);
        } 
        
        holder.tvType.setText(category);
        
        String currencySymbol = context.getString(R.string.currency_symbol);
        // Use Locale.getDefault() to respect user's choice (or forced locale if we implemented that, but here default is fine)
        holder.tvAmount.setText(String.format(java.util.Locale.getDefault(), "%.2f %s", expense.getAmount(), currencySymbol));
        holder.tvDate.setText(expense.getDate());
        
        holder.itemView.setOnClickListener(v -> listener.onItemClick(expense));
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvAmount;
        TextView tvDate;
        TextView tvType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvExpenseTitle);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            tvType = itemView.findViewById(R.id.tvExpenseType);
        }
    }
}

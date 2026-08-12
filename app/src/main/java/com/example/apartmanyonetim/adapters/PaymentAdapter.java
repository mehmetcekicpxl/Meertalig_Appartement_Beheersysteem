package com.example.apartmanyonetim.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apartmanyonetim.R;
import com.example.apartmanyonetim.models.Apartment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.List;
import java.util.Set;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {

    private List<Apartment> apartmentList;
    private Set<Integer> paidRentApartmentIds;
    private Set<Integer> paidAidatApartmentIds;
    private OnPaymentToggleListener listener;

    public interface OnPaymentToggleListener {
        void onToggle(Apartment apartment, String type, boolean isPaid);
    }

    public PaymentAdapter(List<Apartment> apartmentList, 
                          Set<Integer> paidRentApartmentIds, 
                          Set<Integer> paidAidatApartmentIds, 
                          OnPaymentToggleListener listener) {
        this.apartmentList = apartmentList;
        this.paidRentApartmentIds = paidRentApartmentIds;
        this.paidAidatApartmentIds = paidAidatApartmentIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_apartment, parent, false);
        return new ViewHolder(view);
    }

    private String filterMode = "ALL"; // ALL, RENT, AIDAT
    private double euroRate = 0;

    public void setFilterMode(String mode) {
        this.filterMode = mode;
        notifyDataSetChanged();
    }
    
    public void setEuroRate(double rate) {
        this.euroRate = rate;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Apartment apartment = apartmentList.get(position);
        boolean isRentPaid = paidRentApartmentIds.contains(apartment.getId());
        boolean isAidatPaid = paidAidatApartmentIds.contains(apartment.getId());

        // Header Info
        android.content.Context context = holder.itemView.getContext();
        holder.tvDoorNumber.setText(String.format(context.getString(R.string.apartment_door_number_prefix), apartment.getDoorNumber()));
        
        if (apartment.getTenantName() != null && !apartment.getTenantName().isEmpty()) {
            holder.tvTenantName.setText(String.format(context.getString(R.string.tenant_label_prefix), apartment.getTenantName()));
        } else {
            holder.tvTenantName.setText(context.getString(R.string.label_empty));
        }

        // Determine Locale for Display Order
        // Logic: Treat as Turkish (Dual Currency) unless explicitly Flemish/Dutch
        boolean isFlemish = java.util.Locale.getDefault().getLanguage().equals("nl");
        String currencySymbol = context.getString(R.string.currency_symbol);

        // Rent Row
        if ("AIDAT".equals(filterMode)) {
            holder.layoutRent.setVisibility(View.GONE);
            holder.divider.setVisibility(View.GONE);
        } else {
            holder.layoutRent.setVisibility(View.VISIBLE);
            holder.divider.setVisibility(View.VISIBLE);
            
            if (!isFlemish) {
                // Turkish Mode (also default for English): TL is base, Euro is optional
                if (euroRate > 0) {
                    double euroVal = apartment.getRentAmount() / euroRate;
                    holder.tvRentAmount.setText(String.format(java.util.Locale.US, "%.0f TL\n(%.2f €)", apartment.getRentAmount(), euroVal));
                } else {
                    holder.tvRentAmount.setText(String.format(java.util.Locale.US, "%.0f TL", apartment.getRentAmount()));
                }
            } else {
                // Flemish Mode: Base value is ALREADY in Euro (or local currency)
                // Show directly with localized symbol
                holder.tvRentAmount.setText(String.format(java.util.Locale.getDefault(), "%s %.2f", currencySymbol, apartment.getRentAmount()));
            }
            
            holder.switchRent.setOnCheckedChangeListener(null);
            holder.switchRent.setChecked(isRentPaid);
            holder.switchRent.setOnCheckedChangeListener((buttonView, isChecked) -> {
                 if (isChecked) paidRentApartmentIds.add(apartment.getId());
                 else paidRentApartmentIds.remove(apartment.getId());
                 listener.onToggle(apartment, "RENT", isChecked);
            });
        }

        // Aidat Row
        if ("RENT".equals(filterMode)) {
            holder.layoutAidat.setVisibility(View.GONE);
            holder.divider.setVisibility(View.GONE);
        } else {
            holder.layoutAidat.setVisibility(View.VISIBLE);
            if ("ALL".equals(filterMode)) holder.divider.setVisibility(View.VISIBLE);
            
            if (!isFlemish) {
                // Turkish Mode (also default for English): TL is base, Euro is optional
                if (euroRate > 0) {
                    double euroVal = apartment.getAidatAmount() / euroRate;
                    holder.tvAidatAmount.setText(String.format(java.util.Locale.US, "%.0f TL\n(%.2f €)", apartment.getAidatAmount(), euroVal));
                } else {
                    holder.tvAidatAmount.setText(String.format(java.util.Locale.US, "%.0f TL", apartment.getAidatAmount()));
                }
            } else {
                // Flemish Mode: Base value is ALREADY in Euro
                holder.tvAidatAmount.setText(String.format(java.util.Locale.getDefault(), "%s %.2f", currencySymbol, apartment.getAidatAmount()));
            }
            
            holder.switchAidat.setOnCheckedChangeListener(null);
            holder.switchAidat.setChecked(isAidatPaid);
            holder.switchAidat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                 if (isChecked) paidAidatApartmentIds.add(apartment.getId());
                 else paidAidatApartmentIds.remove(apartment.getId());
                 listener.onToggle(apartment, "AIDAT", isChecked);
            });
        }
    }

    @Override
    public int getItemCount() {
        return apartmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoorNumber;
        TextView tvTenantName;
        
        View layoutRent; // Need to reference the row layout
        TextView tvRentAmount;
        SwitchMaterial switchRent;
        
        View layoutAidat; // Need to reference the row layout
        TextView tvAidatAmount;
        SwitchMaterial switchAidat;
        
        View divider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoorNumber = itemView.findViewById(R.id.tvPaymentDoorNumber);
            tvTenantName = itemView.findViewById(R.id.tvPaymentTenantName);
            
            // Assuming the rows are LinearLayouts. I need to get them by child index or ID. 
            // Better to add IDs to item_payment_apartment.xml first.
            // But since I recently rewrote it, I know the structure.
            // Row 1 (Rent) is the 3rd child of the root LinearLayout (index 2), after header (0) and divider (1).
            // Row 2 (Aidat) is the 4th child (index 3).
            
            android.widget.LinearLayout root = (android.widget.LinearLayout) ((androidx.cardview.widget.CardView) itemView).getChildAt(0); // CardView -> Linear
            // No, the root of item is MaterialCardView.
            // Wait, I upgraded to MaterialCardView.
            // Let's use findViewById if I can add IDs. Adding IDs is safer.
            
            // For now, let's use IDs. I will update item_payment_apartment.xml to add IDs to rows.
            // Check item_payment_apartment.xml content again in my memory... I didn't add IDs to rows.
            
            // I will implement findViewById here assuming I WILL add IDs in the next step.
            layoutRent = itemView.findViewById(R.id.layoutRentRow);
            tvRentAmount = itemView.findViewById(R.id.tvRentAmount);
            switchRent = itemView.findViewById(R.id.switchRentStatus);
            
            layoutAidat = itemView.findViewById(R.id.layoutAidatRow);
            tvAidatAmount = itemView.findViewById(R.id.tvAidatAmount);
            switchAidat = itemView.findViewById(R.id.switchAidatStatus);
            
            divider = itemView.findViewById(R.id.dividerRow);
        }
    }
}

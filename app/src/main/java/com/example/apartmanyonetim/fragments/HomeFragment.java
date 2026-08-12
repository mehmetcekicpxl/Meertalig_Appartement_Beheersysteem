package com.example.apartmanyonetim.fragments;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apartmanyonetim.DatabaseHelper;
import com.example.apartmanyonetim.R;
import com.example.apartmanyonetim.adapters.PaymentAdapter;
import com.example.apartmanyonetim.models.Apartment;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Fragment voor het startscherm (Ödemeler)
public class HomeFragment extends Fragment {

    private RecyclerView rvPaymentStatus;
    private TextView tvTotalRent;
    private TextView tvTotalAidat;
    private TextView tvRentCount;
    private TextView tvAidatCount;
    private DatabaseHelper dbHelper;
    private List<Apartment> apartmentList;
    private Set<Integer> paidRentApartmentIds;
    private Set<Integer> paidAidatApartmentIds;

    private Calendar selectedDate = Calendar.getInstance();
    private double euroRate = 0; // Euro wisselkoers

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        dbHelper = new DatabaseHelper(requireContext());
        rvPaymentStatus = view.findViewById(R.id.rvPaymentStatus);
        tvTotalRent = view.findViewById(R.id.tvTotalRent);
        tvTotalAidat = view.findViewById(R.id.tvTotalAidat);
        tvRentCount = view.findViewById(R.id.tvRentCount);
        tvAidatCount = view.findViewById(R.id.tvAidatCount);
        
        rvPaymentStatus.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialiseer data indien nodig (voor eerste start)
        initialiseerDataIndienLeeg();
        
        // Datum header en navigatie
        TextView tvDateHeader = view.findViewById(R.id.tvDateHeader);
        updateDateHeader(tvDateHeader);

        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, -1);
            updateDateHeader(tvDateHeader);
            loadPaymentStatus();
        });

        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, 1);
            updateDateHeader(tvDateHeader);
            loadPaymentStatus();
        });
        // Currency Config (Visible ONLY for Turkish)
        View btnCurrencyConfig = view.findViewById(R.id.btnCurrencyConfig);
        // Currency Config (Visible ONLY if NOT Flemish)
        // User wants it visible in Turkish mode (which includes default English locale fallback)
        // So we only hide it if language is explicitly Dutch/Flemish
        boolean isFlemish = Locale.getDefault().getLanguage().equals("nl");
        if (!isFlemish) {
            btnCurrencyConfig.setVisibility(View.VISIBLE);
            btnCurrencyConfig.setOnClickListener(v -> showCurrencyDialog());
        } else {
            btnCurrencyConfig.setVisibility(View.GONE);
        }
            
        // Load saved rate
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
        float savedRate = prefs.getFloat("EuroRate", 0);
        euroRate = savedRate;
        
        // Filter Logic
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroup = 
            view.findViewById(R.id.toggleGroupFilter);
            
        View layoutTotalRent = view.findViewById(R.id.layoutTotalRent);
        View layoutTotalAidat = view.findViewById(R.id.layoutTotalAidat);
        View dividerTotal = view.findViewById(R.id.dividerTotal);
        
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return; 

            if (rvPaymentStatus.getAdapter() instanceof PaymentAdapter) {
                PaymentAdapter adapter = (PaymentAdapter) rvPaymentStatus.getAdapter();
                if (checkedId == R.id.btnFilterAll) {
                    adapter.setFilterMode("ALL");
                    layoutTotalRent.setVisibility(View.VISIBLE);
                    layoutTotalAidat.setVisibility(View.VISIBLE);
                    dividerTotal.setVisibility(View.VISIBLE);
                } else if (checkedId == R.id.btnFilterRent) {
                    adapter.setFilterMode("RENT");
                    layoutTotalRent.setVisibility(View.VISIBLE);
                    layoutTotalAidat.setVisibility(View.GONE);
                    dividerTotal.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnFilterAidat) {
                    adapter.setFilterMode("AIDAT");
                    layoutTotalRent.setVisibility(View.GONE);
                    layoutTotalAidat.setVisibility(View.VISIBLE);
                    dividerTotal.setVisibility(View.GONE);
                }
            }
        });
        
        // Default selection
        toggleGroup.check(R.id.btnFilterAll);
        
        return view;
    }

    private void updateDateHeader(TextView tvDateHeader) {
        // Use Turkish as default unless device is explicitly in Dutch
        boolean isFlemish = Locale.getDefault().getLanguage().equals("nl");
        Locale displayLocale = isFlemish ? new Locale("nl") : new Locale("tr");
        
        String currentMonthYear = new SimpleDateFormat("MMMM yyyy", displayLocale).format(selectedDate.getTime());
        currentMonthYear = currentMonthYear.substring(0, 1).toUpperCase(displayLocale) + currentMonthYear.substring(1);
        tvDateHeader.setText(currentMonthYear);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPaymentStatus(); // Refresh to catch locale changes
    }

    // Laad appartementen en hun betaalstatus voor deze maand
    private void loadPaymentStatus() {
        apartmentList = new ArrayList<>();
        paidRentApartmentIds = new HashSet<>();
        paidAidatApartmentIds = new HashSet<>();
        double totalRentCollected = 0;
        double totalAidatCollected = 0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // 1. Alle appartementen ophalen
        Cursor cursor = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_APPARTEMENT_ID);
                int doorIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_DEURNUMMER);
                int floorIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_VERDIEPING);
                int balanceIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_HUIDIG_SALDO);
                int tenantIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_HUURDER_NAAM);
                int rentIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_HUUR_BEDRAG);
                int aidatIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_AIDAT_BEDRAG);

                if (idIndex != -1) {
                    String tenantName = (tenantIndex != -1) ? cursor.getString(tenantIndex) : "";
                    double rentAmount = (rentIndex != -1) ? cursor.getDouble(rentIndex) : 0;
                    double aidatAmount = (aidatIndex != -1) ? cursor.getDouble(aidatIndex) : 0;
                    
                    Apartment apt = new Apartment(
                            cursor.getInt(idIndex),
                            cursor.getInt(doorIndex),
                            cursor.getInt(floorIndex),
                            cursor.getDouble(balanceIndex),
                            tenantName,
                            rentAmount,
                            0, "", aidatAmount
                    );
                    apartmentList.add(apt);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // 2. Betaalstatus controleren (Transacties van type INKOMSTEN voor geselecteerde maand)
        // Format query date based on selectedDate. DB stores dates like dd-MM-yyyy.
        // We filter by -MM-yyyy to get all for that month.
        String queryMonth = new SimpleDateFormat("-MM-yyyy", Locale.getDefault()).format(selectedDate.getTime());
        
        Cursor tCursor = db.query(DatabaseHelper.TABEL_TRANSACTIES, null, 
                DatabaseHelper.KOLOM_TYPE + " = ? AND " + DatabaseHelper.KOLOM_DATUM + " LIKE ?", 
                new String[]{"INKOMSTEN", "%" + queryMonth + "%"}, null, null, null);

        if (tCursor.moveToFirst()) {
            int appIdIndex = tCursor.getColumnIndex(DatabaseHelper.KOLOM_TRANSACTIE_APP_ID);
            int amountIndex = tCursor.getColumnIndex(DatabaseHelper.KOLOM_BEDRAG);
            int catIndex = tCursor.getColumnIndex(DatabaseHelper.KOLOM_CATEGORIE);
            
            do {
                int appId = tCursor.getInt(appIdIndex);
                String category = tCursor.getString(catIndex);
                double amount = tCursor.getDouble(amountIndex);

                if ("Kira".equals(category)) {
                    paidRentApartmentIds.add(appId);
                    totalRentCollected += amount;
                } else if ("Aidat".equals(category)) {
                    paidAidatApartmentIds.add(appId);
                    totalAidatCollected += amount;
                }
            } while (tCursor.moveToNext());
        }
        tCursor.close();

        // Update UI with Amounts and Counts using localized currency symbol
        String currencySymbol = getString(R.string.currency_symbol);
        tvTotalRent.setText(String.format("%s%.0f", currencySymbol, totalRentCollected));
        tvTotalAidat.setText(String.format("%s%.0f", currencySymbol, totalAidatCollected));
        
        String paidFormat = getString(R.string.paid_status_format);
        tvRentCount.setText(String.format(Locale.getDefault(), paidFormat, paidRentApartmentIds.size(), apartmentList.size()));
        tvAidatCount.setText(String.format(Locale.getDefault(), paidFormat, paidAidatApartmentIds.size(), apartmentList.size()));
        
        PaymentAdapter adapter = new PaymentAdapter(apartmentList, 
                                                  paidRentApartmentIds, 
                                                  paidAidatApartmentIds, 
                                                  this::togglePayment);
        
        // Pass Euro Rate if TR
        // Pass Euro Rate (ALWAYS if set)
        if (euroRate > 0) {
            adapter.setEuroRate(euroRate);
        }
        
        // Preserve current filter mode if exists
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroup = 
            getView().findViewById(R.id.toggleGroupFilter);
        int checkedId = toggleGroup.getCheckedButtonId();
        if (checkedId == R.id.btnFilterRent) adapter.setFilterMode("RENT");
        else if (checkedId == R.id.btnFilterAidat) adapter.setFilterMode("AIDAT");
        else adapter.setFilterMode("ALL");

        rvPaymentStatus.setAdapter(adapter);
    }

    // Betaling status wijzigen
    private void togglePayment(Apartment apartment, String type, boolean isPaid) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String currentMonthName = new SimpleDateFormat("MMMM yyyy", new Locale("tr", "TR")).format(selectedDate.getTime());
        
        // Use 15th of the selected month as default payment date to avoid timezone/start-of-month edge cases
        Calendar paymentDateCal = (Calendar) selectedDate.clone();
        paymentDateCal.set(Calendar.DAY_OF_MONTH, 15);
        paymentDateCal.set(Calendar.HOUR_OF_DAY, 12);
        String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(paymentDateCal.getTime());
        
        String category = type.equals("RENT") ? "Kira" : "Aidat";
        double amount = type.equals("RENT") ? apartment.getRentAmount() : apartment.getAidatAmount();
        String description = (type.equals("RENT") ? "Kira Ödemesi - " : "Aidat Ödemesi - ") + currentMonthName;

        if (isPaid) {
            // Transactie toevoegen
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.KOLOM_TRANSACTIE_APP_ID, apartment.getId());
            values.put(DatabaseHelper.KOLOM_TYPE, "INKOMSTEN");
            values.put(DatabaseHelper.KOLOM_CATEGORIE, category);
            values.put(DatabaseHelper.KOLOM_BEDRAG, amount > 0 ? amount : 0);
            values.put(DatabaseHelper.KOLOM_DATUM, currentDate);
            values.put(DatabaseHelper.KOLOM_IS_BETAALD, 1);
            values.put(DatabaseHelper.KOLOM_OMSCHRIJVING, description);
            
            db.insert(DatabaseHelper.TABEL_TRANSACTIES, null, values);
            
            // Saldo updaten
            updateApartmentBalance(apartment.getId(), amount);
            
        } else {
            // Transactie verwijderen
            String delWhere = DatabaseHelper.KOLOM_TRANSACTIE_APP_ID + " = ? AND " + 
                              DatabaseHelper.KOLOM_TYPE + " = ? AND " +
                              DatabaseHelper.KOLOM_CATEGORIE + " = ? AND " +
                              DatabaseHelper.KOLOM_OMSCHRIJVING + " LIKE ?";
            String[] delArgs = new String[]{
                String.valueOf(apartment.getId()), 
                "INKOMSTEN", 
                category,
                "%" + description + "%"
            };
            
            int deleted = db.delete(DatabaseHelper.TABEL_TRANSACTIES, delWhere, delArgs);
            
            if (deleted > 0) {
                 // Saldo terugdraaien
                 updateApartmentBalance(apartment.getId(), -amount);
            }
        }
        
        // UI verversen
        loadPaymentStatus();
    }
    
    // Saldo van appartement bijwerken
    private void updateApartmentBalance(int apartmentId, double amountChange) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Eerst huidig saldo ophalen
        Cursor c = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, new String[]{DatabaseHelper.KOLOM_HUIDIG_SALDO}, 
                DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)}, null, null, null);
        
        if (c.moveToFirst()) {
            double currentBalance = c.getDouble(0);
            double newBalance = currentBalance + amountChange;
            
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.KOLOM_HUIDIG_SALDO, newBalance);
            db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)});
        }
        c.close();
    }

    // Dummy data initialiseren als DB leeg is
    private void initialiseerDataIndienLeeg() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT count(*) FROM " + DatabaseHelper.TABEL_APPARTEMENTEN, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        if (count == 0) {
            db = dbHelper.getWritableDatabase();
            for (int i = 1; i <= 10; i++) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.KOLOM_DEURNUMMER, i);
                values.put(DatabaseHelper.KOLOM_VERDIEPING, (i - 1) / 2 + 1);
                values.put(DatabaseHelper.KOLOM_HUIDIG_SALDO, 0);
                values.put(DatabaseHelper.KOLOM_HUURDER_NAAM, "");
                values.put(DatabaseHelper.KOLOM_HUUR_BEDRAG, 12000.0);
                values.put(DatabaseHelper.KOLOM_AIDAT_BEDRAG, 1500.0);
                db.insert(DatabaseHelper.TABEL_APPARTEMENTEN, null, values);
            }
        }
    }

    private void showCurrencyDialog() {
        android.widget.EditText etRate = new android.widget.EditText(requireContext());
        etRate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etRate.setHint(getString(R.string.dialog_currency_hint));
        if (euroRate > 0) etRate.setText(String.valueOf(euroRate));

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_currency_title))
            .setMessage(getString(R.string.dialog_currency_message))
            .setView(etRate)
            .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                String val = etRate.getText().toString();
                if (!val.isEmpty()) {
                    try {
                         euroRate = Double.parseDouble(val);
                         // Save
                         android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
                         prefs.edit().putFloat("EuroRate", (float)euroRate).apply();
                         loadPaymentStatus(); // Refresh list
                    } catch (Exception e) {}
                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

}

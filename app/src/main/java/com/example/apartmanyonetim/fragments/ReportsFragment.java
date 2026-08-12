package com.example.apartmanyonetim.fragments;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.apartmanyonetim.DatabaseHelper;
import com.example.apartmanyonetim.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private BarChart chartYearly, chartMonthly;
    private Spinner spinnerMonth, spinnerYear;
    private TextView tvYearlyTitle, tvYearlyIncome, tvYearlyExpense, tvYearlyProfit;
    private TextView tvMonthlyIncome, tvMonthlyExpense, tvMonthlyProfit;
    private TextView tvPrediction;
    private TextInputEditText etInflationRate;
    private ToggleButton toggleApplyInflation;
    
    // Aidat UI Components
    private TextView tvAidatPrediction;
    private TextInputEditText etAidatInflationRate;
    private ToggleButton toggleApplyAidatInflation;
    
    private DatabaseHelper dbHelper;
    private float appliedInflationRate = 0; // Opslaan welke rate is toegepast
    private float appliedAidatInflationRate = 0; // Opslaan welke aidat rate is toegepast

    private String filterMode = "ALL"; // ALL, RENT, AIDAT

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);
        
        dbHelper = new DatabaseHelper(requireContext());
        
        // UI Binding
        chartYearly = view.findViewById(R.id.chartYearly);
        chartMonthly = view.findViewById(R.id.chartMonthly);
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        
        tvYearlyTitle = view.findViewById(R.id.tvYearlyTitle);
        tvYearlyIncome = view.findViewById(R.id.tvYearlyIncome);
        tvYearlyExpense = view.findViewById(R.id.tvYearlyExpense);
        tvYearlyProfit = view.findViewById(R.id.tvYearlyProfit);
        
        tvMonthlyIncome = view.findViewById(R.id.tvMonthlyIncome);
        tvMonthlyExpense = view.findViewById(R.id.tvMonthlyExpense);
        tvMonthlyProfit = view.findViewById(R.id.tvMonthlyProfit);
        
        tvPrediction = view.findViewById(R.id.tvPrediction);
        etInflationRate = view.findViewById(R.id.etInflationRate);
        toggleApplyInflation = view.findViewById(R.id.toggleApplyInflation);
        
        tvAidatPrediction = view.findViewById(R.id.tvAidatPrediction);
        etAidatInflationRate = view.findViewById(R.id.etAidatInflationRate);
        toggleApplyAidatInflation = view.findViewById(R.id.toggleApplyAidatInflation);

        // Toggle Group Listener
        com.google.android.material.button.MaterialButtonToggleGroup toggleGroup = 
            view.findViewById(R.id.toggleGroupFilterReports);
            
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnFilterAllRep) filterMode = "ALL";
            else if (checkedId == R.id.btnFilterRentRep) filterMode = "RENT";
            else if (checkedId == R.id.btnFilterAidatRep) filterMode = "AIDAT";
            loadReportData();
        });

        setupCharts();
        setupSpinners();
        setupRentSimulator();
        setupAidatSimulator();
        
        return view;
    }

    private void setupCharts() {
        // Init Yearly Chart
        chartYearly.getDescription().setEnabled(false);
        chartYearly.setPinchZoom(false);
        chartYearly.setDrawGridBackground(false);
        XAxis xAxis = chartYearly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"", "Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"}));


        // Init Monthly Chart
        chartMonthly.getDescription().setEnabled(false);
        chartMonthly.setPinchZoom(false);
        chartMonthly.setDrawGridBackground(false);
        XAxis xAxisM = chartMonthly.getXAxis();
        xAxisM.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisM.setGranularity(1f); // Dagen
    }

    private void setupSpinners() {
        // Maanden (Localized - Flemish forced)
        // Maanden (Localized - Flemish if NL, else Turkish)
        boolean isFlemish = Locale.getDefault().getLanguage().equals("nl");
        Locale displayLocale = isFlemish ? new Locale("nl") : new Locale("tr");
        
        java.text.DateFormatSymbols symbols = new java.text.DateFormatSymbols(displayLocale);
        String[] months = symbols.getMonths();
        // DateFormatSymbols returns 13 elements (index 12 is empty string), we need first 12
        String[] displayMonths = new String[12];
        System.arraycopy(months, 0, displayMonths, 0, 12);
        
        // Capitalize first letter
        for (int i=0; i<12; i++) {
            if (displayMonths[i].length() > 0) {
               displayMonths[i] = displayMonths[i].substring(0, 1).toUpperCase(displayLocale) + displayMonths[i].substring(1);
            }
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, displayMonths);
        spinnerMonth.setAdapter(monthAdapter);
        
        // Jaren
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = currentYear - 2; i <= currentYear + 2; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, years);
        spinnerYear.setAdapter(yearAdapter);
        
        // Defaults
        spinnerMonth.setSelection(cal.get(Calendar.MONTH));
        spinnerYear.setSelection(2); // Index van currentYear

        // Listeners
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadReportData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        
        spinnerMonth.setOnItemSelectedListener(listener);
        spinnerYear.setOnItemSelectedListener(listener);
    }
    
    private void setupRentSimulator() {
        etInflationRate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculatePrediction();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        toggleApplyInflation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Apply Inflation
                String rateStr = etInflationRate.getText().toString();
                if (!rateStr.isEmpty()) {
                    float rate = Float.parseFloat(rateStr);
                    applyInflation(rate);
                    etInflationRate.setEnabled(false);
                }
            } else {
                // Revert
                revertInflation();
                etInflationRate.setEnabled(true);
            }
            // Update UI
            calculatePrediction();
        });
    }

    public void onResume() {
        super.onResume();
        loadReportData();
        calculatePrediction();
        calculateAidatPrediction();
    }

    private void loadReportData() {
        String yearStr = spinnerYear.getSelectedItem().toString();
        int monthIndex = spinnerMonth.getSelectedItemPosition() + 1;
        
        tvYearlyTitle.setText(String.format(getString(R.string.financial_summary_title), yearStr));
        loadYearlyData(yearStr);
        loadMonthlyData(monthIndex, yearStr);
    }

    // Yıllık Veri ve Grafik
    private void loadYearlyData(String yearStr) {
        double[] monthlyIncome = new double[13]; // 1-12
        double[] monthlyExpense = new double[13];
        double totalInc = 0, totalExp = 0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABEL_TRANSACTIES, null, 
                DatabaseHelper.KOLOM_DATUM + " LIKE ?", new String[]{"%-" + yearStr + "%"}, null, null, null);

        if (cursor.moveToFirst()) {
            int dateIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_DATUM);
            int typeIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_TYPE);
            int amountIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_BEDRAG);
            int catIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_CATEGORIE);

            do {
                String date = cursor.getString(dateIndex);
                String type = cursor.getString(typeIndex);
                double amount = cursor.getDouble(amountIndex);
                String category = cursor.getString(catIndex);
                
                // Filtering (Global Filter Logic)
                boolean include = true;
                if ("RENT".equals(filterMode)) {
                    if ("INKOMSTEN".equals(type) && !"Kira".equals(category)) include = false;
                    if ("UITGAVEN".equals(type)) include = false; // Show only rent income? Or expense related to... no, expense is general
                    // Let's assume Filter Rent means ONLY Rent Income (Expenses don't have this category usually)
                    // If user wants Net Profit of Rent, we might include expenses, but for now strict.
                } else if ("AIDAT".equals(filterMode)) {
                    if ("INKOMSTEN".equals(type) && !"Aidat".equals(category)) include = false;
                }

                if (include) {
                    // Extract Month
                    // Date format: dd-MM-yyyy HH:mm:ss OR -02-2026 (legacy?)
                    // Let's look for -MM-yyyy part
                    try {
                        String[] parts = date.split("-");
                        if (parts.length >= 2) {
                             int month = Integer.parseInt(parts[1]);
                             if (month >= 1 && month <= 12) {
                                 if ("INKOMSTEN".equals(type)) {
                                     monthlyIncome[month] += amount;
                                     totalInc += amount;
                                 } else {
                                     monthlyExpense[month] += amount;
                                     totalExp += amount;
                                 }
                             }
                        }
                    } catch (Exception e) {}
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Update Text
        String currencySymbol = getString(R.string.currency_symbol);
        tvYearlyIncome.setText(String.format("%s:\n%s%.2f", getString(R.string.label_income), currencySymbol, totalInc));
        tvYearlyExpense.setText(String.format("%s:\n%s%.2f", getString(R.string.label_expense), currencySymbol, totalExp));
        tvYearlyProfit.setText(String.format("%s:\n%s%.2f", getString(R.string.label_net_profit), currencySymbol, totalInc - totalExp));

        // Update Chart
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            entries.add(new BarEntry(i, new float[]{(float)monthlyIncome[i], (float)-monthlyExpense[i]})); // Income pos, Expense neg
        }

        BarDataSet set = new BarDataSet(entries, getString(R.string.chart_label_yearly));
        set.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"));
        set.setStackLabels(new String[]{getString(R.string.label_income), getString(R.string.label_expense)});
        
        BarData data = new BarData(set);
        data.setBarWidth(0.8f);
        chartYearly.setData(data);
        
        // Localized Months for X-Axis
        // Localized Months for X-Axis
        boolean isFlemish = Locale.getDefault().getLanguage().equals("nl");
        Locale displayLocale = isFlemish ? new Locale("nl") : new Locale("tr");

        java.text.DateFormatSymbols symbols = new java.text.DateFormatSymbols(displayLocale);
        String[] monthsFull = symbols.getShortMonths(); // Use short months for chart (Jan, Feb...)
        final String[] chartMonths = new String[13]; // 1-based index for chart
        chartMonths[0] = "";
        for(int i=0; i<12; i++) {
             chartMonths[i+1] = monthsFull[i]; 
        }

        chartYearly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(chartMonths));
        chartYearly.invalidate();
    }

    // Maandelijkse Veri (Inkomen vs Uitgaven vergelijking)
    private void loadMonthlyData(int month, String year) {
        double totalInc = 0, totalExp = 0;
        
        String filter = String.format(Locale.getDefault(), "-%02d-%s", month, year);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABEL_TRANSACTIES, null, 
                DatabaseHelper.KOLOM_DATUM + " LIKE ?", new String[]{"%" + filter + "%"}, null, null, null);

        if (cursor.moveToFirst()) {
            int typeIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_TYPE);
            int amountIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_BEDRAG);
            int catIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_CATEGORIE);

            do {
                String type = cursor.getString(typeIndex);
                double amount = cursor.getDouble(amountIndex);
                String category = cursor.getString(catIndex);
                
                // Filtering
                boolean include = true;
                if ("RENT".equals(filterMode)) {
                    if ("INKOMSTEN".equals(type) && !"Kira".equals(category)) include = false;
                    if ("UITGAVEN".equals(type)) include = false;
                } else if ("AIDAT".equals(filterMode)) {
                    if ("INKOMSTEN".equals(type) && !"Aidat".equals(category)) include = false;
                }

                if (include) {
                    if ("INKOMSTEN".equals(type)) {
                        totalInc += amount;
                    } else {
                        totalExp += amount;
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        String currencySymbol = getString(R.string.currency_symbol);
        tvMonthlyIncome.setText(String.format("%s:\n%s%.2f", getString(R.string.label_income), currencySymbol, totalInc));
        tvMonthlyExpense.setText(String.format("%s:\n%s%.2f", getString(R.string.label_expense), currencySymbol, totalExp));
        tvMonthlyProfit.setText(String.format("%s:\n%s%.2f", getString(R.string.label_net_profit), currencySymbol, totalInc - totalExp));
        
        // Chart: Simple Income vs Expense
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) totalInc));
        entries.add(new BarEntry(1, (float) totalExp));
        
        BarDataSet set = new BarDataSet(entries, getString(R.string.chart_label_monthly));
        set.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"));
        
        BarData data = new BarData(set);
        data.setBarWidth(0.5f);
        chartMonthly.setData(data);
        
        // X-As labels updaten
        chartMonthly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{getString(R.string.label_income), getString(R.string.label_expense)}));
        chartMonthly.getXAxis().setGranularity(1f);
        chartMonthly.getXAxis().setLabelCount(2);
        
        chartMonthly.invalidate();
    }

    private void calculatePrediction() {
        String rateStr = etInflationRate.getText().toString();
        float rate = 0;
        if (!rateStr.isEmpty()) {
            try { rate = Float.parseFloat(rateStr); } catch (Exception e) {}
        }
        
        // Huidige totale inkomsten (o.b.v. huur) ophalen
        double currentTotalRent = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, new String[]{DatabaseHelper.KOLOM_HUUR_BEDRAG}, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                currentTotalRent += c.getDouble(0);
            } while (c.moveToNext());
        }
        c.close();
        
        double predictedNextYearMonthly = currentTotalRent;
        // Als toggle UIT staat, tonen we wat het ZOU zijn. Als AAN staat, is currentTotalRent al verhoogd.
        if (!toggleApplyInflation.isChecked()) {
             predictedNextYearMonthly = currentTotalRent * (1 + rate / 100);
        }
        
        tvPrediction.setText(String.format(Locale.getDefault(), 
            getString(R.string.prediction_text_format), 
            predictedNextYearMonthly, rate));
    }

    private void applyInflation(float rate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        appliedInflationRate = rate;
        
        // We lezen en updaten in transactie? Nee, één voor één
        Cursor c = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, 
                new String[]{DatabaseHelper.KOLOM_APPARTEMENT_ID, DatabaseHelper.KOLOM_HUUR_BEDRAG}, 
                null, null, null, null, null);
                
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                double currentRent = c.getDouble(1);
                double newRent = currentRent * (1 + rate / 100);
                
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.KOLOM_HUUR_BEDRAG, Math.round(newRent)); // Afronden op heel getal? Nee, double.
                
                db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(id)});
            } while (c.moveToNext());
        }
        c.close();
        Toast.makeText(getContext(), String.format(getString(R.string.toast_inflation_applied), rate), Toast.LENGTH_SHORT).show();
    }
    
    private void revertInflation() {
        if (appliedInflationRate == 0) return;
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        Cursor c = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, 
                new String[]{DatabaseHelper.KOLOM_APPARTEMENT_ID, DatabaseHelper.KOLOM_HUUR_BEDRAG}, 
                null, null, null, null, null);
                
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                double currentRent = c.getDouble(1);
                double originalRent = currentRent / (1 + appliedInflationRate / 100);
                
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.KOLOM_HUUR_BEDRAG, Math.round(originalRent)); // Terug naar origineel (ongeveer)
                
                db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(id)});
            } while (c.moveToNext());
        }
        c.close();
        Toast.makeText(getContext(), getString(R.string.toast_inflation_reverted), Toast.LENGTH_SHORT).show();
        appliedInflationRate = 0;
    }
    private void setupAidatSimulator() {
        etAidatInflationRate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateAidatPrediction();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        toggleApplyAidatInflation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String rateStr = etAidatInflationRate.getText().toString();
                if (!rateStr.isEmpty()) {
                    float rate = Float.parseFloat(rateStr);
                    applyAidatInflation(rate);
                    etAidatInflationRate.setEnabled(false);
                }
            } else {
                revertAidatInflation();
                etAidatInflationRate.setEnabled(true);
            }
            calculateAidatPrediction();
        });
    }

    private void calculateAidatPrediction() {
        String rateStr = etAidatInflationRate.getText().toString();
        float rate = 0;
        if (!rateStr.isEmpty()) {
            try { rate = Float.parseFloat(rateStr); } catch (Exception e) {}
        }
        
        // Huidige totale aidat ophalen
        double currentTotalAidat = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, new String[]{DatabaseHelper.KOLOM_AIDAT_BEDRAG}, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                currentTotalAidat += c.getDouble(0);
            } while (c.moveToNext());
        }
        c.close();
        
        double predictedNextYearMonthly = currentTotalAidat;
        if (!toggleApplyAidatInflation.isChecked()) {
             predictedNextYearMonthly = currentTotalAidat * (1 + rate / 100);
        }
        
        tvAidatPrediction.setText(String.format(Locale.getDefault(), 
            getString(R.string.aidat_prediction_result_format), 
            predictedNextYearMonthly, rate));
    }

    private void applyAidatInflation(float rate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        appliedAidatInflationRate = rate;
        
        Cursor c = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, 
                new String[]{DatabaseHelper.KOLOM_APPARTEMENT_ID, DatabaseHelper.KOLOM_AIDAT_BEDRAG}, 
                null, null, null, null, null);
                
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                double currentAidat = c.getDouble(1);
                double newAidat = currentAidat * (1 + rate / 100);
                
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.KOLOM_AIDAT_BEDRAG, Math.round(newAidat)); 
                
                db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(id)});
            } while (c.moveToNext());
        }
        c.close();
        Toast.makeText(getContext(), String.format(getString(R.string.toast_aidat_inflation_applied), rate), Toast.LENGTH_SHORT).show();
    }

    private void revertAidatInflation() {
        if (appliedAidatInflationRate == 0) return;
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        Cursor c = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, 
                new String[]{DatabaseHelper.KOLOM_APPARTEMENT_ID, DatabaseHelper.KOLOM_AIDAT_BEDRAG}, 
                null, null, null, null, null);
                
        if (c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                double currentAidat = c.getDouble(1);
                double originalAidat = currentAidat / (1 + appliedAidatInflationRate / 100);
                
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.KOLOM_AIDAT_BEDRAG, Math.round(originalAidat)); 
                
                db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(id)});
            } while (c.moveToNext());
        }
        c.close();
        Toast.makeText(getContext(), getString(R.string.toast_aidat_inflation_reverted), Toast.LENGTH_SHORT).show();
        appliedAidatInflationRate = 0;
    }
}

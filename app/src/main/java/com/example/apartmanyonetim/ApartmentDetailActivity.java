package com.example.apartmanyonetim;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apartmanyonetim.adapters.FileAdapter;
import com.example.apartmanyonetim.models.FileItem;
import java.util.ArrayList;
import java.util.List;

// Activiteit voor appartementdetails
public class ApartmentDetailActivity extends AppCompatActivity {

    private int apartmentId;
    private DatabaseHelper dbHelper;
    private TextView tvDetailTitle, tvDetailTenant, tvDetailResidentCount, tvDetailAddress, tvDetailAidat;
    private EditText etNotes;
    private RecyclerView rvFiles, rvNotes;
    private FileAdapter fileAdapter;
    private List<FileItem> fileList;

    private static final int PICK_FILE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        dbHelper = new DatabaseHelper(this);
        
        // ID ophalen uit Intent
        apartmentId = getIntent().getIntExtra("apartment_id", -1);

        if (apartmentId == -1) {
            finish(); // Fout, ga terug
            return;
        }

        // UI binden
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailTenant = findViewById(R.id.tvDetailTenant);
        tvDetailResidentCount = findViewById(R.id.tvDetailResidentCount);
        tvDetailAddress = findViewById(R.id.tvDetailAddress);
        tvDetailAidat = findViewById(R.id.tvDetailAidat);
        
        etNotes = findViewById(R.id.etNotes);
        rvFiles = findViewById(R.id.rvFiles);
        rvNotes = findViewById(R.id.rvNotes);
        
        Button btnSaveNote = findViewById(R.id.btnSaveNote);
        Button btnUploadFile = findViewById(R.id.btnUploadFile);
        Button btnEditTenant = findViewById(R.id.btnEditTenant);
        Button btnEditRent = findViewById(R.id.btnEditRent);
        Button btnEditResidentCount = findViewById(R.id.btnEditResidentCount);
        Button btnEditAddress = findViewById(R.id.btnEditAddress);
        Button btnEditAidat = findViewById(R.id.btnEditAidat);

        // Gegevens laden
        loadApartmentDetails();
        loadNotes();
        loadFiles();

        // Listeners
        btnSaveNote.setOnClickListener(v -> saveNote());
        btnUploadFile.setOnClickListener(v -> openFilePicker());
        btnEditTenant.setOnClickListener(v -> showEditTenantDialog());
        btnEditRent.setOnClickListener(v -> showEditRentDialog());
        btnEditResidentCount.setOnClickListener(v -> showEditResidentCountDialog());
        btnEditAddress.setOnClickListener(v -> showEditAddressDialog());
        btnEditAidat.setOnClickListener(v -> showEditAidatDialog());
    }

    // Appartementgegevens en huurderinfo laden
    private void loadApartmentDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Appartement ophalen
        Cursor cursor = db.query(DatabaseHelper.TABEL_APPARTEMENTEN, null, 
                DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)}, 
                null, null, null);

        if (cursor.moveToFirst()) {
            int doorIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_DEURNUMMER);
            int tenantIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_HUURDER_NAAM);
            int rentIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_HUUR_BEDRAG);
            int residentIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_AANTAL_BEWONERS);
            int addressIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_ADRES);
            int aidatIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_AIDAT_BEDRAG);

            if (doorIndex != -1) {
                tvDetailTitle.setText(getString(R.string.apartment_door_number_prefix, cursor.getInt(doorIndex)));
            }

            // Toon huurdersnaam
            if (tenantIndex != -1) {
                String tenantName = cursor.getString(tenantIndex);
                String displayTenant = (tenantName != null && !tenantName.isEmpty()) ? tenantName : getString(R.string.tenant_not_added);
                tvDetailTenant.setText(getString(R.string.apartment_tenant_prefix, displayTenant));
            }
            
            // Toon huurbedrag
            if (rentIndex != -1) {
                double rent = cursor.getDouble(rentIndex);
                TextView tvRent = findViewById(R.id.tvDetailRent);
                tvRent.setText(getString(R.string.rent_amount_format, rent));
            }

            // Toon aantal bewoners
            if (residentIndex != -1) {
                int count = cursor.getInt(residentIndex);
                if (count > 0) {
                    tvDetailResidentCount.setText(getString(R.string.resident_count_prefix, count));
                } else {
                    tvDetailResidentCount.setText(getString(R.string.resident_count_empty));
                }
            }

            // Toon adres
            if (addressIndex != -1) {
                String addr = cursor.getString(addressIndex);
                if (addr != null && !addr.isEmpty()) {
                    tvDetailAddress.setText(getString(R.string.address_prefix, addr));
                } else {
                    tvDetailAddress.setText(getString(R.string.address_empty));
                }
            }


            // Toon aidat
            if (aidatIndex != -1) {
                double aidat = cursor.getDouble(aidatIndex);
                tvDetailAidat.setText(getString(R.string.aidat_amount_format, aidat));
            }
        }
        cursor.close();
    }

    // ... (Dialoog methoden blijven hetzelfde, we korten ze hier niet in om de file compleet te houden)
    private void showEditResidentCountDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_resident_count_title));
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) updateResidentCount(Integer.parseInt(val));
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void showEditAddressDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_address_title));
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        String currentText = tvDetailAddress.getText().toString().replace("Adres: ", "").replace("-", "").trim();
        input.setText(currentText);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            String val = input.getText().toString();
            updateAddress(val);
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void updateResidentCount(int count) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KOLOM_AANTAL_BEWONERS, count);
        db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)});
        loadApartmentDetails();
        Toast.makeText(this, getString(R.string.toast_resident_count_updated), Toast.LENGTH_SHORT).show();
    }

    private void updateAddress(String address) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KOLOM_ADRES, address);
        db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)});
        loadApartmentDetails();
        Toast.makeText(this, getString(R.string.toast_address_updated), Toast.LENGTH_SHORT).show();
    }

    private void showEditTenantDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_tenant_title));
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        String currentText = tvDetailTenant.getText().toString().replace("Kiracı: ", "").replace("(Henüz eklenmedi)", "").trim();
        if (!currentText.equals("-")) input.setText(currentText);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            updateTenantName(input.getText().toString());
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    private void showEditRentDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_rent_title));
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("0.00");
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) updateRentAmount(Double.parseDouble(val));
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void updateTenantName(String newName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KOLOM_HUURDER_NAAM, newName);
        db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)});
        loadApartmentDetails();
        Toast.makeText(this, getString(R.string.toast_tenant_name_updated), Toast.LENGTH_SHORT).show();
    }
    
    private void updateRentAmount(double newAmount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KOLOM_HUUR_BEDRAG, newAmount);
        db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)});
        loadApartmentDetails(); 
        Toast.makeText(this, getString(R.string.toast_rent_updated), Toast.LENGTH_SHORT).show();
    }

    private void showEditAidatDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_aidat_title));
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("0.00");
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) updateAidatAmount(Double.parseDouble(val));
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void updateAidatAmount(double newAmount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KOLOM_AIDAT_BEDRAG, newAmount);
        db.update(DatabaseHelper.TABEL_APPARTEMENTEN, values, DatabaseHelper.KOLOM_APPARTEMENT_ID + " = ?", new String[]{String.valueOf(apartmentId)});
        loadApartmentDetails(); 
        Toast.makeText(this, getString(R.string.toast_aidat_updated), Toast.LENGTH_SHORT).show();
    }

    // Notities laden
    private void loadNotes() {
        List<com.example.apartmanyonetim.models.Note> noteList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABEL_NOTITIES, null, 
                DatabaseHelper.KOLOM_NOTITIE_APP_ID + " = ?", new String[]{String.valueOf(apartmentId)}, 
                null, null, DatabaseHelper.KOLOM_AANGEMAAKT_OP + " DESC");

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_NOTITIE_ID);
            int contentIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_INHOUD);
            int dateIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_AANGEMAAKT_OP);
            
            do {
                if (idIndex != -1) {
                    noteList.add(new com.example.apartmanyonetim.models.Note(
                        cursor.getInt(idIndex),
                        apartmentId,
                        cursor.getString(contentIndex),
                        cursor.getString(dateIndex)
                    ));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        com.example.apartmanyonetim.adapters.NoteAdapter adapter = new com.example.apartmanyonetim.adapters.NoteAdapter(noteList, this::deleteNote);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setAdapter(adapter);
    }
    
    private void deleteNote(com.example.apartmanyonetim.models.Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABEL_NOTITIES, DatabaseHelper.KOLOM_NOTITIE_ID + " = ?", new String[]{String.valueOf(note.getId())});
        loadNotes();
        Toast.makeText(this, getString(R.string.toast_note_deleted), Toast.LENGTH_SHORT).show();
    }

    private void saveNote() {
        String content = etNotes.getText().toString();
        if (content.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KOLOM_NOTITIE_APP_ID, apartmentId);
        values.put(DatabaseHelper.KOLOM_INHOUD, content);
        values.put(DatabaseHelper.KOLOM_AANGEMAAKT_OP, String.valueOf(System.currentTimeMillis()));
        db.insert(DatabaseHelper.TABEL_NOTITIES, null, values);
        Toast.makeText(this, getString(R.string.toast_note_saved), Toast.LENGTH_SHORT).show();
        etNotes.setText("");
        loadNotes();
    }

    // Bestandskeuze openen
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*", "application/msword"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Persistent permissions aanvragen
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                try {
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                } catch (SecurityException e) {
                    e.printStackTrace();
                    Toast.makeText(this, getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show();
                }
                saveFileUri(uri);
            }
        }
    }

    // Bestand URI opslaan in DB
    private void saveFileUri(Uri uri) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KOLOM_BESTAND_APP_ID, apartmentId);
        values.put(DatabaseHelper.KOLOM_URI_PAD, uri.toString());
        values.put(DatabaseHelper.KOLOM_BESTANDSTYPE, "file"); // Type wordt runtime bepaald

        db.insert(DatabaseHelper.TABEL_BESTANDEN, null, values);
        loadFiles();
    }

    // Bestandenlijst laden
    private void loadFiles() {
        fileList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABEL_BESTANDEN, null,
                DatabaseHelper.KOLOM_BESTAND_APP_ID + " = ?", new String[]{String.valueOf(apartmentId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_BESTAND_ID);
                int uriIndex = cursor.getColumnIndex(DatabaseHelper.KOLOM_URI_PAD);
                if (uriIndex != -1) {
                    int id = cursor.getInt(idIndex);
                    String uriStr = cursor.getString(uriIndex);
                    Uri uri = Uri.parse(uriStr);
                    
                    // Naam en type ophalen
                    String name = getFileName(uri);
                    String type = getContentResolver().getType(uri);
                    
                    fileList.add(new FileItem(id, uriStr, name, type));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Update adapter
        fileAdapter = new FileAdapter(fileList, this::openFile, this::deleteFileConfirm);
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setAdapter(fileAdapter);
    }
    
    // Helper om bestandsnaam op te halen
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void openFile(FileItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(item.getUri()), item.getType());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_file_open, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteFileConfirm(FileItem item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_file_title))
            .setMessage(getString(R.string.dialog_delete_file_message))
            .setPositiveButton(getString(R.string.yes), (dialog, which) -> deleteFile(item))
            .setNegativeButton(getString(R.string.no), null)
            .show();
    }
    
    private void deleteFile(FileItem item) {
        // 1. DB'den sil
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABEL_BESTANDEN, DatabaseHelper.KOLOM_BESTAND_ID + " = ?", new String[]{String.valueOf(item.getId())});
        
        // 2. Permission release (optioneel, maar netjes)
        try {
            getContentResolver().releasePersistableUriPermission(Uri.parse(item.getUri()), Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            // Kan falen als permission al weg is, negeer
        }
        
        Toast.makeText(this, getString(R.string.toast_file_deleted), Toast.LENGTH_SHORT).show();
        loadFiles();
    }
}

package com.example.gallimart.shopkeeper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gallimart.InventoryItem;
import com.example.gallimart.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.*;

public class AddItemFragment extends Fragment {

    private static final int PICK_IMAGE = 100;
    private EditText etName, etPrice, etQuantity, etDescription;
    private ImageView ivPreview;
    private Uri selectedImage;
    private DatabaseReference firebaseRef;

    private String supabaseUrl = "https://yxzgowwvyhugzgjiypbk.supabase.co";
    private String bucketName = "uploads";
    private String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl4emdvd3d2eWh1Z3pnaml5cGJrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1ODc5NjYsImV4cCI6MjA3MzE2Mzk2Nn0.K694SUdB5djvZvn9QdAr1ZwfgpxBudyDekXCouz4_Y0";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_item, container, false);

        etName = view.findViewById(R.id.etName);
        etPrice = view.findViewById(R.id.etPrice);
        etQuantity = view.findViewById(R.id.etQuantity);
        etDescription = view.findViewById(R.id.etDescription);
        ivPreview = view.findViewById(R.id.ivPreview);

        firebaseRef = FirebaseDatabase.getInstance().getReference("items");

        Button btnChooseImage = view.findViewById(R.id.btnChooseImage);
        btnChooseImage.setOnClickListener(v -> pickImage());

        Button btnSubmit = view.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> uploadItem());

        return view;
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImage = data.getData();
            ivPreview.setImageURI(selectedImage);
        }
    }

    private void uploadItem() {
        String name = etName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty() || selectedImage == null) {
            Toast.makeText(getContext(), "Fill all fields & choose image", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int quantity = Integer.parseInt(qtyStr);

        // upload image to Supabase
        uploadImageToSupabase(name, price, quantity, desc);
    }

    private void uploadImageToSupabase(String name, double price, int quantity, String desc) {
        try {
            String fileName = UUID.randomUUID() + ".jpg";
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImage);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
            String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

            Request request = new Request.Builder()
                    .url(url)
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String imageUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;

                        String id = firebaseRef.push().getKey();
                        InventoryItem item = new InventoryItem(id, name, price, quantity, desc, imageUrl);
                        firebaseRef.child(id).setValue(item);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Item added!", Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().popBackStack();
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            String body = response.body().string();
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Upload error: " + body, Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

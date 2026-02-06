package com.ritesh.hoppeconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationBarView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ritesh.hoppeconnect.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private MaterialButton btnAllCases, btnMissing, btnFound, btnChildren, btnAdults, btnElderly;
    private MaterialButton selectedFilterButton;
    private BottomNavigationView bottomNav;
    private EditText searchBar;
    private Button btnReadMore;
    private TextView txtMissingCases;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initializeViews();

        // Setup components
        setupFilterButtons();
        setupBottomNavigation();
        setupSearchBar();
        setupPromoCard();
    }

    private void initializeViews() {
        // Filter buttons
        btnAllCases = findViewById(R.id.btnAllCases);
        btnMissing = findViewById(R.id.btnMissing);
        btnFound = findViewById(R.id.btnFound);
        btnChildren = findViewById(R.id.btnChildren);
        btnAdults = findViewById(R.id.btnAdults);
        btnElderly = findViewById(R.id.btnElderly);

        // Other components
        bottomNav = findViewById(R.id.bottomNav);
        searchBar = findViewById(R.id.searchBar);
        btnReadMore = findViewById(R.id.btnReadMore);
        txtMissingCases = findViewById(R.id.txtMissingCases);

        // Set initial selected button
        selectedFilterButton = btnAllCases;
    }

    private void setupFilterButtons() {
        // Create list of all filter buttons
        final List<MaterialButton> filterButtons = new ArrayList<>();
        filterButtons.add(btnAllCases);
        filterButtons.add(btnMissing);
        filterButtons.add(btnFound);
        filterButtons.add(btnChildren);
        filterButtons.add(btnAdults);
        filterButtons.add(btnElderly);

        // Set click listeners for each button
        btnAllCases.setOnClickListener(v -> {
            selectFilterButton(btnAllCases, filterButtons);
            filterAllCases();
        });

        btnMissing.setOnClickListener(v -> {
            selectFilterButton(btnMissing, filterButtons);
            filterByStatus("Missing");
        });

        btnFound.setOnClickListener(v -> {
            selectFilterButton(btnFound, filterButtons);
            filterByStatus("Found");
        });

        btnChildren.setOnClickListener(v -> {
            selectFilterButton(btnChildren, filterButtons);
            filterByAgeGroup("Children");
        });

        btnAdults.setOnClickListener(v -> {
            selectFilterButton(btnAdults, filterButtons);
            filterByAgeGroup("Adults");
        });

        btnElderly.setOnClickListener(v -> {
            selectFilterButton(btnElderly, filterButtons);
            filterByAgeGroup("Elderly");
        });
    }

    private void selectFilterButton(MaterialButton selectedButton, List<MaterialButton> allButtons) {
        // Reset all buttons to unselected state
        for (MaterialButton button : allButtons) {
            button.setBackgroundColor(Color.WHITE);
            button.setTextColor(ContextCompat.getColor(this, R.color.filter_text_unselected));
            button.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.filter_border)));
            button.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.filter_icon_unselected)));
        }

        // Set selected button state
        selectedButton.setBackgroundColor(
                ContextCompat.getColor(this, R.color.filter_bg_selected));
        selectedButton.setTextColor(
                ContextCompat.getColor(this, R.color.filter_text_selected));
        selectedButton.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));

        // Special handling for "All Cases" button - keep fire icon orange
        if (selectedButton.getId() == R.id.btnAllCases) {
            selectedButton.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.fire_orange)));
        } else {
            selectedButton.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.filter_icon_selected)));
        }

        selectedFilterButton = selectedButton;
    }

    private void filterAllCases() {
        // Update header text
        txtMissingCases.setText("Recent Cases");

        // TODO: Implement logic to show all cases
        // Example: Load all cases from database/API
        // caseAdapter.updateList(allCasesList);
    }

    private void filterByStatus(String status) {
        // Update header text
        txtMissingCases.setText("Recent " + status + " Cases");

        // TODO: Implement logic to filter cases by status (Missing/Found)
        // Example: Load filtered cases from database
        // List<Case> filteredCases = database.getCasesByStatus(status);
        // caseAdapter.updateList(filteredCases);
    }

    private void filterByAgeGroup(String ageGroup) {
        // Update header text
        txtMissingCases.setText("Recent " + ageGroup + " Cases");

        // TODO: Implement logic to filter cases by age group
        // Example: Load filtered cases from database
        // List<Case> filteredCases = database.getCasesByAgeGroup(ageGroup);
        // caseAdapter.updateList(filteredCases);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // Already on home
                    return true;
                } else if (itemId == R.id.nav_explore) {
                    // TODO: Navigate to explore screen
                    // Intent intent = new Intent(MainActivity.this, ExploreActivity.class);
                    // startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_new_report) {
                    // TODO: Navigate to new report screen
                    // Intent intent = new Intent(MainActivity.this, NewReportActivity.class);
                    // startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_chat) {
                    // TODO: Navigate to chat screen
                    // Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    // startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // TODO: Navigate to profile screen
                    // Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    // startActivity(intent);
                    return true;
                }
                return false;
            }
        });
    }

    private void setupSearchBar() {
        searchBar.setOnClickListener(v -> {
            // TODO: Open search activity or expand search
            // Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            // startActivity(intent);
        });
    }

    private void setupPromoCard() {
        btnReadMore.setOnClickListener(v -> {
            // TODO: Open detailed information about HopeConnect
            // Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            // startActivity(intent);
        });
    }
}
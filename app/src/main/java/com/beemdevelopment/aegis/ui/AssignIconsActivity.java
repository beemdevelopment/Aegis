package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.MetricsHelper;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.dialogs.IconPickerDialog;
import com.beemdevelopment.aegis.ui.glide.GlideHelper;
import com.beemdevelopment.aegis.ui.models.AssignIconEntry;
import com.beemdevelopment.aegis.ui.views.AssignIconAdapter;
import com.beemdevelopment.aegis.ui.views.IconAdapter;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.helpers.ViewHelper;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultEntryIcon;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AssignIconsActivity extends AegisActivity implements AssignIconAdapter.Listener {
    private AssignIconAdapter _adapter;
    private ArrayList<AssignIconEntry> _entries = new ArrayList<>();
    private RecyclerView _entriesView;
    private AssignIconsActivity.BackPressHandler _backPressHandler;
    private ViewPreloadSizeProvider<AssignIconEntry> _preloadSizeProvider;
    private IconPack _favoriteIconPack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }

        setContentView(R.layout.activity_assign_icons);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewHelper.setupAppBarInsets(findViewById(R.id.app_bar_layout));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ArrayList<UUID> assignIconEntriesIds = (ArrayList<UUID>) getIntent().getSerializableExtra("entries");
        for (UUID entryId: assignIconEntriesIds) {
            VaultEntry vaultEntry = _vaultManager.getVault().getEntryByUUID(entryId);
            _entries.add(new AssignIconEntry(vaultEntry));
        }

        _backPressHandler = new AssignIconsActivity.BackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _backPressHandler);

        IconPreloadProvider modelProvider1 = new IconPreloadProvider();
        EntryIconPreloadProvider modelProvider2 = new EntryIconPreloadProvider();
        _preloadSizeProvider = new ViewPreloadSizeProvider<>();
        RecyclerViewPreloader<IconPack.Icon> preloader1 = new RecyclerViewPreloader(this, modelProvider1, _preloadSizeProvider, 10);
        RecyclerViewPreloader<VaultEntry> preloader2 = new RecyclerViewPreloader(this, modelProvider2, _preloadSizeProvider, 10);

        _adapter = new AssignIconAdapter(this);
        _entriesView = findViewById(R.id.list_assign_icons);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        _entriesView.setLayoutManager(layoutManager);
        _entriesView.setAdapter(_adapter);
        _entriesView.setNestedScrollingEnabled(false);
        _entriesView.addItemDecoration(new SpacesItemDecoration(8));
        _entriesView.addOnScrollListener(preloader1);
        _entriesView.addOnScrollListener(preloader2);

        Optional<IconPack> favoriteIconPack = _iconPackManager.getIconPacks().stream()
                .sorted(Comparator.comparing(IconPack::getName))
                .findFirst();

        if (!favoriteIconPack.isPresent()) {
            throw new RuntimeException(String.format("Started %s without any icon packs present", AssignIconsActivity.class.getName()));
        }

        _favoriteIconPack = favoriteIconPack.get();

        for (AssignIconEntry entry : _entries) {
            IconPack.Icon suggestedIcon = findSuggestedIcon(entry);
            if (suggestedIcon != null) {
                entry.setNewIcon(suggestedIcon);
            }
        }

        _adapter.addEntries(_entries);
    }

    private IconPack.Icon findSuggestedIcon(AssignIconEntry entry) {
        List<IconPack.Icon> suggestedIcons = _favoriteIconPack.getSuggestedIcons(entry.getEntry().getIssuer());
        if (suggestedIcons.size() > 0) {
            return suggestedIcons.get(0);
        }

        return null;
    }

    private void saveAndFinish() throws IOException {
        ArrayList<UUID> uuids = new ArrayList<>();
        for (AssignIconEntry selectedEntry : _entries) {
            VaultEntry entry = selectedEntry.getEntry();
            if (selectedEntry.getNewIcon() != null) {
                byte[] iconBytes;
                try (FileInputStream inStream = new FileInputStream(selectedEntry.getNewIcon().getFile())){
                    iconBytes = IOUtils.readFile(inStream);
                }

                VaultEntryIcon icon = new VaultEntryIcon(iconBytes, selectedEntry.getNewIcon().getIconType());
                entry.setIcon(icon);
                uuids.add(entry.getUUID());

                _vaultManager.getVault().replaceEntry(entry);
            }
        }

        Intent intent = new Intent();
        intent.putExtra("entryUUIDs", uuids);

        if (saveAndBackupVault()) {
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void discardAndFinish() {
        Dialogs.showDiscardDialog(this,
                (dialog, which) -> {
                    try {
                        saveAndFinish();
                    } catch (IOException e) {
                        Toast.makeText(this, R.string.saving_assign_icons_error, Toast.LENGTH_SHORT).show();
                    }
                },
                (dialog, which) -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_assign_icons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            discardAndFinish();
        } else if (itemId == R.id.action_save) {
            try {
                saveAndFinish();
            } catch (IOException e) {
                Toast.makeText(this, R.string.saving_assign_icons_error, Toast.LENGTH_SHORT).show();
            }
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onAssignIconEntryClick(AssignIconEntry entry) {
        List<IconPack> iconPacks = _iconPackManager.getIconPacks().stream()
                .sorted(Comparator.comparing(IconPack::getName))
                .collect(Collectors.toList());

        BottomSheetDialog dialog = IconPickerDialog.create(this, iconPacks, entry.getEntry().getIssuer(), false, new IconAdapter.Listener() {
            @Override
            public void onIconSelected(IconPack.Icon icon) {
                entry.setNewIcon(icon);
            }

            @Override
            public void onCustomSelected() { }
        });
        Dialogs.showSecureDialog(dialog);
    }

    @Override
    public void onSetPreloadView(View view) {
        _preloadSizeProvider.setView(view);
    }

    private class BackPressHandler extends OnBackPressedCallback {
        public BackPressHandler() {
            super(false);
        }

        @Override
        public void handleOnBackPressed() {
            discardAndFinish();
        }
    }

    private class EntryIconPreloadProvider implements ListPreloader.PreloadModelProvider<VaultEntry> {
        @NonNull
        @Override
        public List<VaultEntry> getPreloadItems(int position) {
            VaultEntry entry = _entries.get(position).getEntry();
            if (entry.hasIcon()) {
                return Collections.singletonList(entry);
            }
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull VaultEntry entry) {
            RequestBuilder<Drawable> rb = Glide.with(AssignIconsActivity.this)
                    .load(entry.getIcon());
            return GlideHelper.setCommonOptions(rb, entry.getIcon().getType());
        }
    }

    private class IconPreloadProvider implements ListPreloader.PreloadModelProvider<IconPack.Icon> {
        @NonNull
        @Override
        public List<IconPack.Icon> getPreloadItems(int position) {
            AssignIconEntry entry = _entries.get(position);
            if (entry.getNewIcon() != null) {
                return Collections.singletonList(entry.getNewIcon());
            }
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull IconPack.Icon icon) {
            RequestBuilder<Drawable> rb = Glide.with(AssignIconsActivity.this)
                    .load(icon.getFile());
            return GlideHelper.setCommonOptions(rb, icon.getIconType());
        }
    }

    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int _space;

        public SpacesItemDecoration(int dpSpace) {

            this._space = MetricsHelper.convertDpToPixels(AssignIconsActivity.this, dpSpace);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = _space;
            outRect.right = _space;
            outRect.bottom = _space;

            if (parent.getChildLayoutPosition(view) == 0) {
                outRect.top = _space;
            }
        }
    }
}
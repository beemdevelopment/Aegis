package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.AnimationsHelper;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.icons.IconPackException;
import com.beemdevelopment.aegis.icons.IconPackExistsException;
import com.beemdevelopment.aegis.icons.IconPackManager;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.tasks.ImportIconPackTask;
import com.beemdevelopment.aegis.ui.views.IconPackAdapter;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IconPacksManagerFragment extends Fragment implements IconPackAdapter.Listener {
    @Inject
    IconPackManager _iconPackManager;

    @Inject
    VaultManager _vaultManager;

    private RecyclerView _iconPacksRecyclerView;
    private IconPackAdapter _adapter;
    private LinearLayout _noIconPacksView;
    private FabScrollHelper _fabScrollHelper;

    private final ActivityResultLauncher<Intent> importResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                Intent data = activityResult.getData();
                if (activityResult.getResultCode() == Activity.RESULT_OK && data != null && data.getData() != null) {
                    importIconPack(data.getData());
                }
            });

    public IconPacksManagerFragment() {
        super(R.layout.fragment_icon_packs);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> startImportIconPack());
        _fabScrollHelper = new FabScrollHelper(fab);

        final MarginLayoutParams fabInitialMargin = (MarginLayoutParams) fab.getLayoutParams();
        ViewCompat.setOnApplyWindowInsetsListener(fab, (targetView, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            MarginLayoutParams marginParams = (MarginLayoutParams) targetView.getLayoutParams();
            marginParams.leftMargin = fabInitialMargin.leftMargin + insets.left;
            marginParams.bottomMargin = fabInitialMargin.bottomMargin + insets.bottom;
            marginParams.rightMargin = fabInitialMargin.rightMargin + insets.right;
            targetView.setLayoutParams(marginParams);

            return WindowInsetsCompat.CONSUMED;
        });

        _noIconPacksView = view.findViewById(R.id.vEmptyList);
        ((TextView) view.findViewById(R.id.txt_no_icon_packs)).setMovementMethod(LinkMovementMethod.getInstance());
        _adapter = new IconPackAdapter(this);
        _iconPacksRecyclerView = view.findViewById(R.id.list_icon_packs);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        _iconPacksRecyclerView.setLayoutManager(layoutManager);
        _iconPacksRecyclerView.setAdapter(_adapter);
        _iconPacksRecyclerView.setNestedScrollingEnabled(false);
        _iconPacksRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                _fabScrollHelper.onScroll(dx, dy);
            }
        });

        for (IconPack pack : _iconPackManager.getIconPacks()) {
            _adapter.addIconPack(pack);
        }

        updateEmptyState();
    }

    @Override
    @Nullable
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (nextAnim != 0) {
            return AnimationsHelper.loadScaledAnimation(requireContext(), nextAnim, AnimationsHelper.Scale.TRANSITION);
        }

        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Override
    public void onRemoveIconPack(IconPack pack) {
        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(R.string.remove_icon_pack)
                .setMessage(R.string.remove_icon_pack_description)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    removeIconPack(pack);
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    private void importIconPack(Uri uri) {
        ImportIconPackTask task = new ImportIconPackTask(requireContext(), result -> {
            Exception e = result.getException();
            if (e instanceof IconPackExistsException) {
                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                        .setTitle(R.string.error_occurred)
                        .setMessage(R.string.icon_pack_import_exists_error)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            if (removeIconPack(((IconPackExistsException) e).getIconPack())) {
                                importIconPack(uri);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .create());
            } else if (e != null) {
                Dialogs.showErrorDialog(requireContext(), R.string.icon_pack_import_error, e);
            } else {
                _adapter.addIconPack(result.getIconPack());
                updateEmptyState();
            }
        });
        task.execute(getLifecycle(), new ImportIconPackTask.Params(_iconPackManager, uri));
    }

    private boolean removeIconPack(IconPack pack) {
        try {
            _iconPackManager.removeIconPack(pack);
        } catch (IconPackException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(requireContext(), R.string.icon_pack_delete_error, e);
            return false;
        }

        _adapter.removeIconPack(pack);
        updateEmptyState();
        return true;
    }

    private void startImportIconPack() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        _vaultManager.fireIntentLauncher(this, intent, importResultLauncher);
    }

    private void updateEmptyState() {
        if (_adapter.getItemCount() > 0) {
            _iconPacksRecyclerView.setVisibility(View.VISIBLE);
            _noIconPacksView.setVisibility(View.GONE);
        } else {
            _iconPacksRecyclerView.setVisibility(View.GONE);
            _noIconPacksView.setVisibility(View.VISIBLE);
        }
    }
}

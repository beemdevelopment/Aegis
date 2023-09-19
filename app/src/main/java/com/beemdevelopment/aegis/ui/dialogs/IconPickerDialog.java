package com.beemdevelopment.aegis.ui.dialogs;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.GridLayoutManager;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.ui.glide.GlideHelper;
import com.beemdevelopment.aegis.ui.views.IconAdapter;
import com.beemdevelopment.aegis.ui.views.IconRecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IconPickerDialog {
    private IconPickerDialog() {

    }

    public static BottomSheetDialog create(Activity activity, List<IconPack> iconPacks, String issuer, boolean showAddCustom, IconAdapter.Listener listener) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_icon_picker, null);
        TextView textIconPack = view.findViewById(R.id.text_icon_pack);

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(view);
        dialog.create();

        FrameLayout rootView = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        rootView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;

        IconAdapter adapter = new IconAdapter(dialog.getContext(), issuer, new IconAdapter.Listener() {
            @Override
            public void onIconSelected(IconPack.Icon icon) {
                dialog.dismiss();
                listener.onIconSelected(icon);
            }

            @Override
            public void onCustomSelected() {
                dialog.dismiss();
                listener.onCustomSelected();
            }
        });

        class IconPreloadProvider implements ListPreloader.PreloadModelProvider<IconPack.Icon> {
            @NonNull
            @Override
            public List<IconPack.Icon> getPreloadItems(int position) {
                IconPack.Icon icon = adapter.getIconAt(position);
                return Collections.singletonList(icon);
            }

            @Nullable
            @Override
            public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull IconPack.Icon icon) {
                RequestBuilder<Drawable> rb = Glide.with(dialog.getContext())
                        .load(icon.getFile());
                return GlideHelper.setCommonOptions(rb, icon.getIconType());
            }
        }

        TextInputEditText iconSearch = view.findViewById(R.id.text_search_icon);
        iconSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(rootView);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        iconSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                adapter.setQuery(query.isEmpty() ? null : query);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        ViewPreloadSizeProvider<IconPack.Icon> preloadSizeProvider = new ViewPreloadSizeProvider<>();
        IconPreloadProvider modelProvider = new IconPreloadProvider();
        RecyclerViewPreloader<IconPack.Icon> preloader = new RecyclerViewPreloader<>(activity, modelProvider, preloadSizeProvider, 10);
        IconRecyclerView recyclerView = view.findViewById(R.id.list_icons);
        GridLayoutManager layoutManager = recyclerView.getGridLayoutManager();
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == R.layout.card_icon) {
                    return 1;
                }

                return recyclerView.getSpanCount();
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(preloader);
        adapter.loadIcons(iconPacks.get(0), showAddCustom);
        textIconPack.setText(iconPacks.get(0).getName());

        view.findViewById(R.id.btn_icon_pack).setOnClickListener(v -> {
            List<String> iconPackNames = iconPacks.stream()
                    .map(IconPack::getName)
                    .collect(Collectors.toList());

            PopupMenu popupMenu = new PopupMenu(activity, v);
            popupMenu.setOnMenuItemClickListener(item -> {
                IconPack pack = iconPacks.get(iconPackNames.indexOf(item.getTitle().toString()));
                adapter.loadIcons(pack, showAddCustom);

                String query = iconSearch.getText().toString();
                if (!query.isEmpty()) {
                    adapter.setQuery(query);
                }

                textIconPack.setText(pack.getName());
                return true;
            });
            Menu menu = popupMenu.getMenu();
            for (String name : iconPackNames) {
                menu.add(name);
            }

            popupMenu.show();
        });

        return dialog;
    }
}

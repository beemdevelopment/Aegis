package com.beemdevelopment.aegis.ui.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.icons.IconType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class IconAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context _context;
    private final String _issuer;
    private final Listener _listener;
    private IconPack _pack;
    private List<IconPack.Icon> _icons;
    private final List<CategoryHeader> _categories;
    private String _query;

    public IconAdapter(@NonNull Context context, String issuer, @NonNull Listener listener) {
        _context = context;
        _issuer = issuer;
        _listener = listener;
        _icons = new ArrayList<>();
        _categories = new ArrayList<>();
    }

    /**
     * Loads all icons from the given icon pack into this adapter. Any icons added before this call will be overwritten.
     */
    public void loadIcons(IconPack pack, boolean showAddCustom) {
        _pack = pack;
        _query = null;
        _icons = new ArrayList<>(_pack.getIcons());
        _categories.clear();

        Comparator<IconPack.Icon> iconCategoryComparator = (i1, i2) -> {
            String c1 = getCategoryString(i1.getCategory());
            String c2 = getCategoryString(i2.getCategory());
            return c1.compareTo(c2);
        };
        Collections.sort(_icons, iconCategoryComparator.thenComparing(IconPack.Icon::getName));

        long categoryCount = _icons.stream()
                .map(IconPack.Icon::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        List<IconPack.Icon> suggested = pack.getSuggestedIcons(_issuer);

        if (showAddCustom) {
            suggested.add(0, new DummyIcon(_context.getString(R.string.icon_custom)));
        }

        if (suggested.size() > 0) {
            CategoryHeader category = new CategoryHeader(_context.getString(R.string.suggested));
            category.setIsCollapsed(false);
            category.getIcons().addAll(suggested);
            _categories.add(category);
        }

        CategoryHeader category = null;
        for (IconPack.Icon icon : _icons) {
            String iconCategory = getCategoryString(icon.getCategory());
            if (category == null || !getCategoryString(category.getCategory()).equals(iconCategory)) {
                boolean collapsed = !(categoryCount == 0 && category == null);
                category = new CategoryHeader(iconCategory);
                category.setIsCollapsed(collapsed);
                _categories.add(category);
            }

            category.getIcons().add(icon);
        }

        _icons.addAll(0, suggested);
        updateCategoryPositions();
        notifyDataSetChanged();
    }

    public void setQuery(@Nullable String query) {
        _query = query;

        if (_query == null) {
            loadIcons(_pack, false);
        } else {
            _icons = _pack.getSuggestedIcons(query);
            notifyDataSetChanged();
        }
    }

    public IconPack.Icon getIconAt(int position) {
        if (isQueryActive()) {
            return _icons.get(position);
        }

        position = translateIconPosition(position);
        return _icons.get(position);
    }

    public CategoryHeader getCategoryAt(int position) {
        return _categories.stream()
                .filter(c -> c.getPosition() == position)
                .findFirst()
                .orElse(null);
    }

    private String getCategoryString(String category) {
        return category == null ? _context.getString(R.string.uncategorized) : category;
    }

    private boolean isCategoryPosition(int position) {
        if (isQueryActive()) {
            return false;
        }

        return getCategoryAt(position) != null;
    }

    private int translateIconPosition(int position) {
        int offset = 0;
        for (CategoryHeader category : _categories) {
            if (category.getPosition() < position) {
                offset++;
                if (category.isCollapsed()) {
                    offset -= category.getIcons().size();
                }
            }
        }

        return position - offset;
    }

    private void updateCategoryPositions() {
        int i = 0;
        for (CategoryHeader category : _categories) {
            category.setPosition(i);

            int icons = 0;
            if (!category.isCollapsed()) {
                icons = category.getIcons().size();
            }

            i += 1 + icons;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return viewType == R.layout.card_icon ? new IconHolder(view) : new IconCategoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!isCategoryPosition(position)) {
            IconHolder iconHolder = (IconHolder) holder;
            IconPack.Icon icon = getIconAt(position);
            iconHolder.setData(icon);
            iconHolder.loadIcon(_context);
            iconHolder.itemView.setOnClickListener(v -> {
                if (icon instanceof DummyIcon) {
                    _listener.onCustomSelected();
                } else {
                    _listener.onIconSelected(icon);
                }
            });
        } else {
            IconCategoryHolder categoryHolder = (IconCategoryHolder) holder;
            CategoryHeader category = getCategoryAt(position);
            categoryHolder.setData(category);
            categoryHolder.itemView.setOnClickListener(v -> {
                boolean collapsed = !category.isCollapsed();
                categoryHolder.setIsCollapsed(collapsed);
                category.setIsCollapsed(collapsed);

                int startPosition = category.getPosition() + 1;
                if (category.isCollapsed()) {
                    notifyItemRangeRemoved(startPosition, category.getIcons().size());
                } else {
                    notifyItemRangeInserted(startPosition, category.getIcons().size());
                }

                updateCategoryPositions();
            });
        }
    }

    @Override
    public int getItemCount() {
        if (isQueryActive()) {
            return _icons.size();
        }

        int items = _categories.stream()
                .filter(c -> !c.isCollapsed())
                .mapToInt(c -> c.getIcons().size())
                .sum();

        return items + _categories.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (isCategoryPosition(position)) {
            return R.layout.card_icon_category;
        }

        return R.layout.card_icon;
    }

    private boolean isQueryActive() {
        return _query != null;
    }

    public interface Listener {
        void onIconSelected(IconPack.Icon icon);
        void onCustomSelected();
    }

    public static class DummyIcon extends IconPack.Icon {
        protected DummyIcon(String name) {
            super(name, null, null, null);
        }

        @Override
        public IconType getIconType() {
            return null;
        }
    }

    public static class CategoryHeader {
        private final String _category;
        private int _position = -1;
        private final List<IconPack.Icon> _icons;
        private boolean _collapsed = true;

        public CategoryHeader(String category) {
            _category = category;
            _icons = new ArrayList<>();
        }

        public String getCategory() {
            return _category;
        }

        public int getPosition() {
            return _position;
        }

        public void setPosition(int position) {
            _position = position;
        }

        public List<IconPack.Icon> getIcons() {
            return _icons;
        }

        public boolean isCollapsed() {
            return _collapsed;
        }

        public void setIsCollapsed(boolean collapsed) {
            _collapsed = collapsed;
        }
    }
}

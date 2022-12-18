package com.beemdevelopment.aegis.ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.PluralsRes;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.beemdevelopment.aegis.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DropdownCheckBoxes extends AppCompatAutoCompleteTextView {
    private @PluralsRes int _selectedCountPlural = R.plurals.dropdown_checkboxes_default_count;

    private boolean _allowFiltering = false;

    private final List<String> _items = new ArrayList<>();
    private List<String> _visibleItems = new ArrayList<>();
    private final Set<String> _checkedItems = new TreeSet<>();

    private CheckboxAdapter _adapter;

    public DropdownCheckBoxes(Context context) {
        super(context);
        initialise(context, null);
    }

    public DropdownCheckBoxes(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise(context, attrs);
    }

    public DropdownCheckBoxes(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialise(context, attrs);
    }

    private void initialise(Context context, AttributeSet attrs) {
        _adapter = new CheckboxAdapter();
        setAdapter(_adapter);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.DropdownCheckBoxes,
                    0, 0);

            _allowFiltering = a.getBoolean(R.styleable.DropdownCheckBoxes_allow_filtering, false);
            a.recycle();
        }

        if (!_allowFiltering) {
            setInputType(0);
        } else {
            setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    public void addItems(List<String> items, boolean startChecked) {
        _items.addAll(items);
        _visibleItems.addAll(items);

        if (startChecked) {
            _checkedItems.addAll(items);
        }

        updateCheckedItemsCountText();
        _adapter.notifyDataSetChanged();
    }

    private void updateCheckedItemsCountText() {
        if (_allowFiltering) {
            return;
        }

        int count = _checkedItems.size();
        String countString = getResources().getQuantityString(_selectedCountPlural, count, count);

        setText(countString, false);
    }

    public void setCheckedItemsCountTextRes(@PluralsRes int resId) {
        _selectedCountPlural = resId;
    }

    public Set<String> getCheckedItems() {
        return _checkedItems;
    }

    private class CheckboxAdapter extends BaseAdapter implements Filterable {

        @Override
        public int getCount() {
            return _visibleItems.size();
        }

        @Override
        public String getItem(int i) {
            return _visibleItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.dropdown_checkbox, viewGroup, false);
            }

            String item = _visibleItems.get(i);

            CheckBox checkBox = convertView.findViewById(R.id.checkbox_in_dropdown);
            checkBox.setText(item);
            checkBox.setChecked(_checkedItems.contains(item));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String label = buttonView.getText().toString();

                if (isChecked) {
                    _checkedItems.add(label);
                } else {
                    _checkedItems.remove(label);
                }

                updateCheckedItemsCountText();
            });

            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence query) {
                    FilterResults results = new FilterResults();
                    results.values = (query == null || query.toString().isEmpty())
                                   ? _items
                                   : _items.stream().filter(str -> {
                                                        String q = query.toString().toLowerCase();
                                                        String strLower = str.toLowerCase();

                                                        return strLower.contains(q);
                                                    })
                                                    .collect(Collectors.toList());

                    return results;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    _visibleItems = (List<String>) filterResults.values;
                    notifyDataSetChanged();
                }
            };
        }
    }
}

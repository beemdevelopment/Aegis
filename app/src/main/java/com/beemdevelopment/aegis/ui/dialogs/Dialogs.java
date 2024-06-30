package com.beemdevelopment.aegis.ui.dialogs;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.PasswordStrengthHelper;
import com.beemdevelopment.aegis.helpers.SimpleTextWatcher;
import com.beemdevelopment.aegis.importers.DatabaseImporter;
import com.beemdevelopment.aegis.ui.tasks.KeyDerivationTask;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.crypto.Cipher;

public class Dialogs {
    private Dialogs() {

    }

    public static void secureDialog(Dialog dialog) {
        if (new Preferences(dialog.getContext()).isSecureScreenEnabled()) {
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public static void showSecureDialog(Dialog dialog) {
        secureDialog(dialog);
        dialog.show();
    }

    public static void showDeleteEntriesDialog(Context context, List<VaultEntry> services, DialogInterface.OnClickListener onDelete) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_entry, null);
        TextView textMessage = view.findViewById(R.id.text_message);
        TextView textExplanation = view.findViewById(R.id.text_explanation);
        String entries = services.stream()
                .map(entry -> String.format("• %s", getVaultEntryName(context, entry)))
                .collect(Collectors.joining("\n"));
        textExplanation.setText(context.getString(R.string.delete_entry_explanation, entries));

        String title, message;
        if (services.size() > 1) {
            title = context.getString(R.string.delete_entries);
            message = context.getResources().getQuantityString(R.plurals.delete_entries_description, services.size(), services.size());
        } else {
            title = context.getString(R.string.delete_entry);
            message = context.getString(R.string.delete_entry_description);
        }
        textMessage.setText(message);

        showSecureDialog(new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(title)
                .setView(view)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.yes, onDelete)
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    private static String getVaultEntryName(Context context, VaultEntry entry) {
        if (!entry.getIssuer().isEmpty() && !entry.getName().isEmpty()) {
            return String.format("%s (%s)", entry.getIssuer(), entry.getName());
        } else if (entry.getIssuer().isEmpty() && entry.getName().isEmpty()) {
            return context.getString(R.string.unknown_issuer);
        } else if (entry.getIssuer().isEmpty()) {
            return entry.getName();
        } else {
            return entry.getIssuer();
        }
    }

    public static void showDiscardDialog(Context context, DialogInterface.OnClickListener onSave, DialogInterface.OnClickListener onDiscard) {
        showSecureDialog(new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(context.getString(R.string.discard_changes))
                .setMessage(context.getString(R.string.discard_changes_description))
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(R.string.save, onSave)
                .setNegativeButton(R.string.discard, onDiscard)
                .create());
    }

    public static void showSetPasswordDialog(ComponentActivity activity, PasswordSlotListener listener) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_password, null);
        EditText textPassword = view.findViewById(R.id.text_password);
        EditText textPasswordConfirm = view.findViewById(R.id.text_password_confirm);
        ProgressBar barPasswordStrength = view.findViewById(R.id.progressBar);
        TextView textPasswordStrength = view.findViewById(R.id.text_password_strength);
        TextInputLayout textPasswordWrapper = view.findViewById(R.id.text_password_wrapper);
        CheckBox switchToggleVisibility = view.findViewById(R.id.check_toggle_visibility);
        PasswordStrengthHelper passStrength = new PasswordStrengthHelper(
                textPassword, barPasswordStrength, textPasswordStrength, textPasswordWrapper);

        switchToggleVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                textPassword.setTransformationMethod(null);
                textPasswordConfirm.setTransformationMethod(null);
                textPassword.clearFocus();
                textPasswordConfirm.clearFocus();
            } else {
                textPassword.setTransformationMethod(new PasswordTransformationMethod());
                textPasswordConfirm.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.set_password)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        final AtomicReference<Button> buttonOK = new AtomicReference<>();
        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(false);
            buttonOK.set(button);

            // replace the default listener
            button.setOnClickListener(v -> {
                if (!EditTextHelper.areEditTextsEqual(textPassword, textPasswordConfirm)) {
                    return;
                }

                char[] password = EditTextHelper.getEditTextChars(textPassword);
                PasswordSlot slot = new PasswordSlot();
                KeyDerivationTask task = new KeyDerivationTask(activity, (passSlot, key) -> {
                    Cipher cipher;
                    try {
                        cipher = Slot.createEncryptCipher(key);
                    } catch (SlotException e) {
                        listener.onException(e);
                        dialog.cancel();
                        return;
                    }
                    listener.onSlotResult(slot, cipher);
                    dialog.dismiss();
                });
                KeyDerivationTask.Params params = new KeyDerivationTask.Params(slot, password);
                task.execute(activity.getLifecycle(), params);
            });
        });

        TextWatcher watcher = new SimpleTextWatcher(text -> {
            boolean equal = EditTextHelper.areEditTextsEqual(textPassword, textPasswordConfirm);
            buttonOK.get().setEnabled(equal);
            passStrength.measure(activity);
        });
        textPassword.addTextChangedListener(watcher);
        textPasswordConfirm.addTextChangedListener(watcher);

        showSecureDialog(dialog);
    }

    private static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int messageId, @StringRes int hintId, TextInputListener listener, DialogInterface.OnCancelListener cancelListener, boolean isSecret,@Nullable String hint) {
        final AtomicReference<Button> buttonOK = new AtomicReference<>();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_text_input, null);
        TextInputEditText input = view.findViewById(R.id.text_input);
        if(hint != null) {
            input.setText(hint);
        }
        input.addTextChangedListener(new SimpleTextWatcher(text -> {
            if (buttonOK.get() != null) {
                buttonOK.get().setEnabled(!text.toString().isEmpty());
            }
        }));
        TextInputLayout inputLayout = view.findViewById(R.id.text_input_layout);
        if (isSecret) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        inputLayout.setHint(hintId);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null);

        if (cancelListener != null) {
            builder.setOnCancelListener(cancelListener);
        }

        if (messageId != 0) {
            builder.setMessage(messageId);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(false);
            buttonOK.set(button);

            button.setOnClickListener(v -> {
                char[] text = EditTextHelper.getEditTextChars(input);
                listener.onTextInputResult(text);
                dialog.dismiss();
            });
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        showSecureDialog(dialog);
    }

    public static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int hintId, TextInputListener listener, String text) {
        showTextInputDialog(context, titleId, 0, hintId, listener, null, false, text);
    }

    private static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int hintId, TextInputListener listener, boolean isSecret) {
        showTextInputDialog(context, titleId, 0, hintId, listener, null, isSecret, null);
    }

    public static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int hintId, TextInputListener listener) {
        showTextInputDialog(context, titleId, 0, hintId, listener, null, false, null);
    }

    public static void showPasswordInputDialog(Context context, TextInputListener listener) {
        showTextInputDialog(context, R.string.set_password, R.string.password, listener, true);
    }

    public static void showPasswordInputDialog(Context context, TextInputListener listener, DialogInterface.OnCancelListener cancelListener) {
        showTextInputDialog(context, R.string.set_password, 0, R.string.password, listener, cancelListener, true, null);
    }

    public static void showPasswordInputDialog(Context context, @StringRes int messageId, TextInputListener listener) {
        showTextInputDialog(context, R.string.set_password, messageId, R.string.password, listener, null, true, null);
    }

    public static void showPasswordInputDialog(Context context, @StringRes int messageId, TextInputListener listener, DialogInterface.OnCancelListener cancelListener) {
        showTextInputDialog(context, R.string.set_password, messageId, R.string.password, listener, cancelListener, true, null);
    }

    public static void showPasswordInputDialog(Context context, @StringRes int titleId, @StringRes int messageId, TextInputListener listener, DialogInterface.OnCancelListener cancelListener) {
        showTextInputDialog(context, titleId, messageId, R.string.password, listener, cancelListener, true, null);
    }

    public static void showCheckboxDialog(Context context, @StringRes int titleId, @StringRes int messageId, @StringRes int checkboxMessageId, CheckboxInputListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_checkbox, null);
        CheckBox checkBox = view.findViewById(R.id.checkbox);
        checkBox.setText(checkboxMessageId);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(titleId)
                .setView(view)
                .setNegativeButton(R.string.no, (dialog1, which) ->
                        listener.onCheckboxInputResult(false))
                .setPositiveButton(R.string.yes, (dialog1, which) ->
                        listener.onCheckboxInputResult(checkBox.isChecked()));

        if (messageId != 0) {
            builder.setMessage(messageId);
        }

        AlertDialog dialog = builder.create();

        final AtomicReference<Button> buttonOK = new AtomicReference<>();
        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(false);
            buttonOK.set(button);
        });

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> buttonOK.get().setEnabled(isChecked));

        showSecureDialog(dialog);
    }

    public static void showTapToRevealTimeoutPickerDialog(Context context, int currentValue, NumberInputListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_number_picker, null);
        NumberPicker numberPicker = view.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(60);
        numberPicker.setValue(currentValue);
        numberPicker.setWrapSelectorWheel(true);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.set_number)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) ->
                        listener.onNumberInputResult(numberPicker.getValue()))
                .create();

        showSecureDialog(dialog);
    }

    public static void showBackupVersionsPickerDialog(Context context, int currentVersionCount, NumberInputListener listener) {
        final int max = 30;
        String[] numbers = new String[max / 5];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = Integer.toString(i * 5 + 5);
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_number_picker, null);
        NumberPicker numberPicker = view.findViewById(R.id.numberPicker);
        numberPicker.setDisplayedValues(numbers);
        numberPicker.setMaxValue(numbers.length - 1);
        numberPicker.setMinValue(0);
        numberPicker.setValue(currentVersionCount / 5 - 1);
        numberPicker.setWrapSelectorWheel(false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.set_number)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) ->
                        listener.onNumberInputResult(numberPicker.getValue()))
                .create();

        showSecureDialog(dialog);
    }

    public static void showErrorDialog(Context context, @StringRes int message, Exception e) {
        showErrorDialog(context, message, e, null);
    }

    public static void showErrorDialog(Context context, String message, Exception e) {
        showErrorDialog(context, message, e, null);
    }

    public static void showErrorDialog(Context context, @StringRes int message, CharSequence error) {
        showErrorDialog(context, message, error, null);
    }

    public static void showErrorDialog(Context context, @StringRes int message, Exception e, DialogInterface.OnClickListener listener) {
        showErrorDialog(context, message, e.toString(), listener);
    }

    public static void showErrorDialog(Context context, String message, Exception e, DialogInterface.OnClickListener listener) {
        showErrorDialog(context, message, e.toString(), listener);
    }

    public static void showErrorDialog(Context context, @StringRes int message, CharSequence error, DialogInterface.OnClickListener listener) {
        showErrorDialog(context, context.getString(message), error, listener);
    }

    public static void showErrorDialog(Context context, String message, CharSequence error, DialogInterface.OnClickListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_error, null);
        TextView textDetails = view.findViewById(R.id.error_details);
        textDetails.setText(error);
        TextView textMessage = view.findViewById(R.id.error_message);
        textMessage.setText(message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                .setTitle(R.string.error_occurred)
                .setView(view)
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    if (listener != null) {
                        listener.onClick(dialog1, which);
                    }
                })
                .setNeutralButton(R.string.details, (dialog1, which) -> {
                    textDetails.setVisibility(View.VISIBLE);
                })
                .create();

        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(v -> {
                if (textDetails.getVisibility() == View.GONE) {
                    textDetails.setVisibility(View.VISIBLE);
                    button.setText(R.string.copy);
                } else {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("text/plain", error);
                        clipboard.setPrimaryClip(clip);
                    }
                }
            });
        });

        Dialogs.showSecureDialog(dialog);
    }

    public static void showBackupErrorDialog(Context context, Preferences.BackupResult backupRes, DialogInterface.OnClickListener listener) {
        String system = context.getString(backupRes.isBuiltIn() ? R.string.backup_system_builtin : R.string.backup_system_android);
        @StringRes int details = backupRes.isPermissionError() ? R.string.backup_permission_error_dialog_details : R.string.backup_error_dialog_details;
        String message = context.getString(details, system, backupRes.getElapsedSince(context));
        Dialogs.showErrorDialog(context, message, backupRes.getError(), listener);
    }

    public static void showMultiErrorDialog(
            Context context, @StringRes int title, String message, List<CharSequence> messages, DialogInterface.OnClickListener listener) {
        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (listener != null) {
                        listener.onClick(dialog, which);
                    }
                })
                .setNeutralButton(context.getString(R.string.details), (dialog, which) -> {
                    showDetailedMultiErrorDialog(context, title, messages, listener);
                })
                .create());
    }

    public static <T extends Throwable> void showMultiExceptionDialog(
            Context context, @StringRes int title, String message, List<T> errors, DialogInterface.OnClickListener listener) {
        List<CharSequence> messages = new ArrayList<>();
        for (Throwable e : errors) {
            messages.add(e.toString());
        }

        showMultiErrorDialog(context, title, message, messages, listener);
    }

    private static void showDetailedMultiErrorDialog(
            Context context, @StringRes int title, List<CharSequence> messages, DialogInterface.OnClickListener listener) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (CharSequence message : messages) {
            builder.append(message);
            builder.append("\n\n");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                .setTitle(title)
                .setMessage(builder)
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    if (listener != null) {
                        listener.onClick(dialog1, which);
                    }
                })
                .setNeutralButton(android.R.string.copy, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text/plain", builder.toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, R.string.errors_copied, Toast.LENGTH_SHORT).show();
            });
        });

        Dialogs.showSecureDialog(dialog);
    }
    
    public static void showTimeSyncWarningDialog(Context context, Dialog.OnClickListener listener) {
        Preferences prefs = new Preferences(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_time_sync, null);
        CheckBox checkBox = view.findViewById(R.id.check_warning_disable);

        showSecureDialog(new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(R.string.time_sync_warning_title)
                .setView(view)
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (checkBox.isChecked()) {
                        prefs.setIsTimeSyncWarningEnabled(false);
                    }
                    if (listener != null) {
                        listener.onClick(dialog, which);
                    }
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    if (checkBox.isChecked()) {
                        prefs.setIsTimeSyncWarningEnabled(false);
                    }
                })
                .create());
    }

    public static void showImportersDialog(Context context, boolean isDirect, ImporterListener listener) {
        List<DatabaseImporter.Definition> importers = DatabaseImporter.getImporters(isDirect);
        List<String> names = importers.stream().map(DatabaseImporter.Definition::getName).collect(Collectors.toList());

        int i = 0;
        if (!isDirect) {
            i = names.indexOf(context.getString(R.string.app_name));
        }
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_importers, null);
        TextView helpText = view.findViewById(R.id.text_importer_help);
        setImporterHelpText(helpText, importers.get(i), isDirect);
        ListView listView = view.findViewById(R.id.list_importers);
        listView.setAdapter(new ArrayAdapter<>(context, R.layout.card_importer, names));
        listView.setItemChecked(i, true);
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            setImporterHelpText(helpText, importers.get(position), isDirect);
        });

        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.choose_application)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    listener.onImporterSelectionResult(importers.get(listView.getCheckedItemPosition()));
                })
                .create());
    }

    public static void showPartialGoogleAuthImportWarningDialog(Context context, List<Integer> missingIndexes, int entries, List<CharSequence> scanningErrors, DialogInterface.OnClickListener dismissHandler) {
        String missingIndexesAsString = missingIndexes.stream()
                .map(index -> context.getString(R.string.missing_qr_code_descriptor, index + 1))
                .collect(Collectors.joining("\n"));

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_error, null);
        TextView errorDetails = view.findViewById(R.id.error_details);
        for (CharSequence error: scanningErrors) {
            errorDetails.append(error);
            errorDetails.append("\n\n");
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(R.string.partial_google_auth_import)
                .setMessage(context.getString(R.string.partial_google_auth_import_warning, missingIndexesAsString))
                .setView(view)
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(context.getString(R.string.import_partial_export_anyway, entries), (dialog, which) -> {
                    dismissHandler.onClick(dialog, which);
                })
                .setNegativeButton(android.R.string.cancel, null);

        if (scanningErrors.size() > 0) {
            builder.setNeutralButton(R.string.show_error_details, null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button btnNeutral = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (btnNeutral != null) {
                btnNeutral.setOnClickListener(b -> {
                    errorDetails.setVisibility(View.VISIBLE);
                    btnNeutral.setVisibility(View.GONE);
                });
            }
        });

        showSecureDialog(dialog);
    }

    private static void setImporterHelpText(TextView view, DatabaseImporter.Definition definition, boolean isDirect) {
        if (isDirect) {
            view.setText(view.getResources().getString(R.string.importer_help_direct, definition.getName()));
        } else {
            view.setText(definition.getHelp());
        }
    }

    public interface CheckboxInputListener {
        void onCheckboxInputResult(boolean checkbox);
    }

    public interface NumberInputListener {
        void onNumberInputResult(int number);
    }

    public interface TextInputListener {
        void onTextInputResult(char[] text);
    }

    public interface PasswordSlotListener {
        void onSlotResult(PasswordSlot slot, Cipher cipher);
        void onException(Exception e);
    }

    public interface ImporterListener {
        void onImporterSelectionResult(DatabaseImporter.Definition definition);
    }
}

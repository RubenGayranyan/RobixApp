package net.robixlab.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Browser;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout messagesContainer;
    private ScrollView messageScroll;
    private EditText messageInput;
    private ImageButton sendButton;
    private Button chatModelButton;
    private LinearLayout deviceListContainer;
    private Button addDeviceButton;
    private final List<Device> devices = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        bindViews();
        View rootView = findViewById(R.id.main);
        int basePaddingLeft = rootView.getPaddingLeft();
        int basePaddingTop = rootView.getPaddingTop();
        int basePaddingRight = rootView.getPaddingRight();
        int basePaddingBottom = rootView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(systemBars.bottom, imeInsets.bottom);
            v.setPadding(
                    basePaddingLeft + systemBars.left,
                    basePaddingTop + systemBars.top,
                    basePaddingRight + systemBars.right,
                    basePaddingBottom + bottomInset
            );
            return insets;
        });

        addAssistantMessage(getString(R.string.loading_message));
        setupInputListeners();
        setupDeviceManagement();
    }

    private void bindViews() {
        messagesContainer = findViewById(R.id.messages_container);
        messageScroll = findViewById(R.id.message_scroll);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        chatModelButton = findViewById(R.id.chat_model_button);
        deviceListContainer = findViewById(R.id.device_list_container);
        addDeviceButton = findViewById(R.id.add_device_button);
    }

    private void setupInputListeners() {
        sendButton.setOnClickListener(v -> handleSendMessage());
        chatModelButton.setOnClickListener(v -> openChatGpt());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                handleSendMessage();
                return true;
            }
            return false;
        });
    }

    private void setupDeviceManagement() {
        if (devices.isEmpty()) {
            devices.add(new Device("Роутер", "Сеть офиса", "192.168.0.10", "8080"));
            devices.add(new Device("Сервер", "Внутреннее приложение", "10.10.0.5", null));
        }

        addDeviceButton.setOnClickListener(v -> showAddDeviceDialog());
        refreshDevices();
    }

    private void refreshDevices() {
        deviceListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            View itemView = inflater.inflate(R.layout.item_device, deviceListContainer, false);
            TextView title = itemView.findViewById(R.id.device_title);
            TextView subtitle = itemView.findViewById(R.id.device_subtitle);
            ImageButton settingsButton = itemView.findViewById(R.id.device_settings_button);

            title.setText(device.name);
            StringBuilder subtitleText = new StringBuilder(device.appName)
                    .append("  •  ")
                    .append(device.ip);
            if (!TextUtils.isEmpty(device.port)) {
                subtitleText.append(":" + device.port);
            }
            subtitle.setText(subtitleText.toString());

            int position = i;
            settingsButton.setOnClickListener(v -> openDeviceSettings(position));
            itemView.setOnClickListener(v -> openDeviceActions(device));
            deviceListContainer.addView(itemView);
        }
    }

    private void openDeviceActions(Device device) {
        String[] items = new String[]{
                getString(R.string.device_actions_open),
                getString(R.string.device_actions_share),
                getString(R.string.device_actions_cancel)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.device_action_dialog_title, device.name))
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        Toast.makeText(this, getString(R.string.device_actions_open), Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        Toast.makeText(this, getString(R.string.device_actions_share), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void openDeviceSettings(int position) {
        Device device = devices.get(position);
        String[] options = new String[]{
                getString(R.string.device_settings_rename),
                getString(R.string.device_settings_delete)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.device_settings_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRenameDialog(position);
                    } else {
                        confirmDelete(position);
                    }
                })
                .show();
    }

    private void showRenameDialog(int position) {
        Device device = devices.get(position);
        EditText input = new EditText(this);
        input.setText(device.name);
        input.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_input_field));
        input.setPadding(24, 20, 24, 20);

        new AlertDialog.Builder(this)
                .setTitle(R.string.device_settings_rename)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, R.string.required_field_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    device.name = newName;
                    refreshDevices();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmDelete(int position) {
        Device device = devices.get(position);
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.delete_device_confirm, device.name))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    devices.remove(position);
                    refreshDevices();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAddDeviceDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_device, null, false);
        EditText nameInput = dialogView.findViewById(R.id.device_name_input);
        EditText appNameInput = dialogView.findViewById(R.id.app_name_input);
        EditText ipInput = dialogView.findViewById(R.id.ip_input);
        EditText portInput = dialogView.findViewById(R.id.port_input);
        Switch manualSwitch = dialogView.findViewById(R.id.manual_ip_switch);

        String autoIp = "192.168.0.10";
        manualSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ipInput.setEnabled(isChecked);
            if (isChecked) {
                ipInput.setText("");
                ipInput.setHint(R.string.ip_hint);
            } else {
                ipInput.setHint("");
                ipInput.setText(autoIp);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String appName = appNameInput.getText().toString().trim();
            String ip = ipInput.getText().toString().trim();
            String port = portInput.getText().toString().trim();

            boolean valid = true;
            if (TextUtils.isEmpty(name)) {
                nameInput.setError(getString(R.string.required_field_error));
                valid = false;
            } else {
                nameInput.setError(null);
            }

            if (TextUtils.isEmpty(appName)) {
                appNameInput.setError(getString(R.string.required_field_error));
                valid = false;
            } else {
                appNameInput.setError(null);
            }

            if (TextUtils.isEmpty(ip)) {
                ipInput.setError(getString(R.string.required_field_error));
                valid = false;
            } else {
                ipInput.setError(null);
            }

            if (!valid) {
                return;
            }

            devices.add(new Device(name, appName, ip, TextUtils.isEmpty(port) ? null : port));
            refreshDevices();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void handleSendMessage() {
        String prompt = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            messageInput.setError(getString(R.string.empty_message_error));
            return;
        }

        addUserMessage(prompt);
        messageInput.setText("");
        replyToMessage(prompt);
    }

    private void replyToMessage(String prompt) {
        handler.postDelayed(() -> {
            String response = getString(R.string.sample_answer_message, prompt);
            addAssistantMessage(response);
        }, 450);
    }

    private void addUserMessage(String message) {
        addMessageBubble(message, true);
    }

    private void addAssistantMessage(String message) {
        addMessageBubble(message, false);
    }

    private void addMessageBubble(String message, boolean isUser) {
        LinearLayout messageRow = new LinearLayout(this);
        messageRow.setOrientation(LinearLayout.HORIZONTAL);
        int rowHorizontalPadding = getResources().getDimensionPixelSize(R.dimen.message_horizontal_padding);
        int rowVerticalPadding = getResources().getDimensionPixelSize(R.dimen.message_vertical_padding);
        int messageSpacing = getResources().getDimensionPixelSize(R.dimen.message_spacing);
        messageRow.setPadding(rowHorizontalPadding, rowVerticalPadding, rowHorizontalPadding, messageSpacing);
        messageRow.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);

        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextSize(15f);
        int bubblePaddingHorizontal = getResources().getDimensionPixelSize(R.dimen.bubble_padding_horizontal);
        int bubblePaddingVertical = getResources().getDimensionPixelSize(R.dimen.bubble_padding_vertical);
        bubble.setPadding(bubblePaddingHorizontal, bubblePaddingVertical,
                bubblePaddingHorizontal, bubblePaddingVertical);
        int bubbleMarginHorizontal = getResources().getDimensionPixelSize(R.dimen.bubble_margin_horizontal);
        int bubbleMarginVertical = getResources().getDimensionPixelSize(R.dimen.bubble_margin_vertical);
        LinearLayout.LayoutParams bubbleLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubbleLayoutParams.setMargins(
                bubbleMarginHorizontal,
                bubbleMarginVertical,
                bubbleMarginHorizontal,
                bubbleMarginVertical
        );
        bubble.setLayoutParams(bubbleLayoutParams);
        bubble.setIncludeFontPadding(false);
        bubble.setBackground(ContextCompat.getDrawable(this,
                isUser ? R.drawable.bg_user_bubble : R.drawable.bg_assistant_bubble));
        bubble.setTextColor(ContextCompat.getColor(this,
                isUser ? R.color.primary_color : R.color.normal_text_color));
        if (isUser) {
            bubble.setTypeface(bubble.getTypeface(), android.graphics.Typeface.BOLD);
        }

        messageRow.addView(bubble);
        messagesContainer.addView(messageRow);
        scrollToBottom();
    }

    private void scrollToBottom() {
        messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void openChatGpt() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://openai.com/chatgpt"));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setPackage(null);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
        Intent chooser = Intent.createChooser(intent, getString(R.string.open_in_browser));
        startActivity(chooser);
    }

    private static class Device {
        String name;
        String appName;
        String ip;
        String port;

        Device(String name, String appName, String ip, String port) {
            this.name = name;
            this.appName = appName;
            this.ip = ip;
            this.port = port;
        }
    }
}

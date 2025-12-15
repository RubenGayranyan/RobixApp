package net.robixlab.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private LinearLayout messagesContainer;
    private ScrollView messageScroll;
    private EditText messageInput;
    private ImageButton sendButton;
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
    }

    private void bindViews() {
        messagesContainer = findViewById(R.id.messages_container);
        messageScroll = findViewById(R.id.message_scroll);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
    }

    private void setupInputListeners() {
        sendButton.setOnClickListener(v -> handleSendMessage());
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
        int messageSpacing = getResources().getDimensionPixelSize(R.dimen.message_spacing);
        messageRow.setPadding(rowHorizontalPadding, 0, rowHorizontalPadding, messageSpacing);
        messageRow.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);

        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextSize(15f);
        int bubblePaddingHorizontal = getResources().getDimensionPixelSize(R.dimen.bubble_padding_horizontal);
        int bubblePaddingVertical = getResources().getDimensionPixelSize(R.dimen.bubble_padding_vertical);
        bubble.setPadding(bubblePaddingHorizontal, bubblePaddingVertical,
                bubblePaddingHorizontal, bubblePaddingVertical);
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
}
package com.hanyao.whiteboard.ui;

import com.hanyao.whiteboard.model.ChatMessage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class ChatPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final JTextArea transcript = new JTextArea();
    private final JTextField input = new JTextField();
    private final Consumer<String> messageConsumer;

    public ChatPanel(Consumer<String> messageConsumer) {
        super(new BorderLayout(8, 8));
        this.messageConsumer = messageConsumer;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendMessage);
        input.addActionListener(this::sendMessage);

        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(new JScrollPane(transcript), BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    public void append(ChatMessage message) {
        transcript.append("[%s] %s: %s%n".formatted(
                TIME_FORMATTER.format(message.timestamp()), message.username(), message.message()));
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    private void sendMessage(ActionEvent event) {
        String text = input.getText().trim();
        if (!text.isEmpty()) {
            messageConsumer.accept(text);
            input.setText("");
        }
    }
}

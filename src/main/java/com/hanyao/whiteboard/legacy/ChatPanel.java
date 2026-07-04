package com.hanyao.whiteboard.legacy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatPanel extends JPanel {

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendBtn;
    private P2PWhiteboard p2p;

    public ChatPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Chat"));

        // Chat display area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Input area at bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("Send");

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            appendMessage("You: " + msg);
            inputField.setText("");
            if (p2p != null) {
                p2p.sendChat(msg);
            }
        }
    }

    public void appendMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void setP2P(P2PWhiteboard p2p) {
        this.p2p = p2p;
    }

}


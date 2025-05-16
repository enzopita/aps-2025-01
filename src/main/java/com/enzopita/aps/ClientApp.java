package com.enzopita.aps;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientApp extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    private JLabel statusLabel;
    private Socket socket;

    public ClientApp() {
        setupFlatLaf();
        initUI();
    }

    private void setupFlatLaf() {
        FlatRobotoFont.install();
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 5);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("defaultFont", new Font("Roboto", Font.PLAIN, 13));
    }

    private void initUI() {
        setTitle("Inspetor Ambiental - Cliente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel chatPanel = createChatPanel();
        JPanel reportsPanel = createReportsPanel();

        tabbedPane.addTab("Chat Principal", chatPanel);
        tabbedPane.addTab("Relatórios", reportsPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        statusLabel = new JLabel(" Desconectado");
        statusLabel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);
        createMenuBar();
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFixedCellWidth(200);
        userList.setFont(new Font("Roboto", Font.PLAIN, 13));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                chatScroll,
                new JScrollPane(userList)
        );
        splitPane.setDividerLocation(700);

        chatPanel.add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        messageField = new JTextField();
        messageField.setFont(new Font("Roboto", Font.PLAIN, 14));

        JButton sendButton = new JButton("Enviar");
        sendButton.setFont(new Font("Roboto", Font.BOLD, 12));
        sendButton.setBackground(new Color(0, 122, 204));
        sendButton.setForeground(Color.WHITE);
        sendButton.setPreferredSize(new Dimension(120, 40));

        JPanel sendPanel = new JPanel(new BorderLayout(5, 5));
        sendPanel.add(messageField, BorderLayout.CENTER);
        sendPanel.add(sendButton, BorderLayout.EAST);

        bottomPanel.add(sendPanel, BorderLayout.CENTER);
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        return chatPanel;
    }

    private JPanel createReportsPanel() {
        JPanel reportsPanel = new JPanel(new BorderLayout());
        reportsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Formulário de Relatório de Inspeção"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Componentes do formulário
        JLabel typeLabel = new JLabel("Tipo de Inspeção:");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Água", "Ar", "Solo", "Resíduos", "Ruído"});

        JLabel dateLabel = new JLabel("Data:");
        JTextField dateField = new JTextField(LocalDateTime.now().toLocalDate().toString());
        dateField.setEditable(false);

        JLabel levelLabel = new JLabel("Nível de Poluição:");
        JSlider levelSlider = new JSlider(0, 10, 0);
        levelSlider.setMajorTickSpacing(2);
        levelSlider.setMinorTickSpacing(1);
        levelSlider.setPaintTicks(true);
        levelSlider.setPaintLabels(true);

        JLabel notesLabel = new JLabel("Observações:");
        JTextArea notesArea = new JTextArea(4, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        JButton submitButton = new JButton("Enviar Relatório");
        submitButton.setBackground(new Color(76, 175, 80));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Roboto", Font.BOLD, 12));

        // Adiciona componentes ao formulário
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(typeLabel, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        formPanel.add(typeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        formPanel.add(dateLabel, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        formPanel.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        formPanel.add(levelLabel, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        formPanel.add(levelSlider, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(notesLabel, gbc);

        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(new JScrollPane(notesArea), gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(submitButton, gbc);

        // Ação do botão de enviar relatório
        submitButton.addActionListener(e -> {
            String report = String.format(
                    "RELATÓRIO: Tipo=%s | Nível=%d | Observações=%s",
                    typeCombo.getSelectedItem(),
                    levelSlider.getValue(),
                    notesArea.getText()
            );

            if (out != null) {
                out.println(report);
                chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] Você: " + report + "\n");
                JOptionPane.showMessageDialog(this, "Relatório enviado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                notesArea.setText("");
                levelSlider.setValue(0);
            } else {
                JOptionPane.showMessageDialog(this, "Não conectado ao servidor", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        reportsPanel.add(formPanel, BorderLayout.NORTH);

        return reportsPanel;
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu connectionMenu = new JMenu("Conexão");
        JMenuItem connectItem = new JMenuItem("Conectar");
        JMenuItem disconnectItem = new JMenuItem("Desconectar");
        JMenuItem exitItem = new JMenuItem("Sair");

        connectItem.addActionListener(e -> connectToServer());
        disconnectItem.addActionListener(e -> disconnectFromServer());
        exitItem.addActionListener(e -> System.exit(0));

        connectionMenu.add(connectItem);
        connectionMenu.add(disconnectItem);
        connectionMenu.addSeparator();
        connectionMenu.add(exitItem);

        menuBar.add(connectionMenu);
        setJMenuBar(menuBar);
    }

    private void connectToServer() {
        String name = JOptionPane.showInputDialog(this,
                "Digite seu nome de inspetor:",
                "Inspetor-" + (int)(Math.random() * 100));

        if (name == null || name.trim().isEmpty()) return;

        clientName = name.trim();

        String serverAddress = JOptionPane.showInputDialog(this,
                "Endereço do servidor:", "localhost");

        if (serverAddress == null || serverAddress.trim().isEmpty()) {
            serverAddress = "localhost";
        }

        try {
            socket = new Socket(serverAddress.trim(), 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(clientName);
            updateStatus("Conectado como " + clientName, new Color(46, 125, 50));

            // Solicita lista de usuários imediatamente após conectar
            out.println("/users");

            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        if (response.startsWith("/userlist")) {
                            updateUserList(response.substring(10).split(","));
                            continue;
                        }

                        if (response.equals("/disconnect")) {
                            // Servidor solicitou desconexão
                            SwingUtilities.invokeLater(() -> {
                                disconnectFromServer();
                                chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] Você foi desconectado pelo servidor\n");
                            });
                            break;
                        }

                        String finalResponse = response;
                        SwingUtilities.invokeLater(() -> {
                            chatArea.append(finalResponse + "\n");
                        });
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        updateStatus("Desconectado", new Color(198, 40, 40));
                        chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] Conexão perdida: " + e.getMessage() + "\n");
                    });
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        if (socket != null && !socket.isClosed()) {
                            try {
                                socket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            }).start();

            chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] Conectado ao servidor como " + clientName + "\n");

        } catch (IOException e) {
            updateStatus("Falha na conexão", new Color(198, 40, 40));
            JOptionPane.showMessageDialog(this,
                    "Não foi possível conectar: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                if (!user.isEmpty()) {
                    userListModel.addElement(user);
                }
            }
        });
    }

    private void disconnectFromServer() {
        if (out != null) {
            out.println("/quit");
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateStatus("Desconectado", new Color(198, 40, 40));
            chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] Desconectado do servidor\n");
        }
    }

    private void updateStatus(String message, Color color) {
        statusLabel.setText(" " + message);
        statusLabel.setForeground(color);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] Você: " + message + "\n");
            messageField.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientApp client = new ClientApp();
            client.setVisible(true);
        });
    }
}
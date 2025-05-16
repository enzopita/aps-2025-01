package com.enzopita.aps;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ServerApp extends JFrame {
    private JTextArea chatArea;
    private JTextArea logArea;
    private JList<String> userList;
    private DefaultListModel<String> listModel;
    private JTextField messageField;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private ServerSocket serverSocket;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    private int messageCount = 0;
    private int pollutionAlerts = 0;
    private JLabel statsMessageCount;
    private JLabel statsInspectors;
    private JLabel statsAlerts;

    public ServerApp() {
        setupFlatLaf();
        initUI();
        startServer();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerApp server = new ServerApp();
            server.setVisible(true);
        });
    }

    private void setupFlatLaf() {
        FlatRobotoFont.install();
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 5);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("defaultFont", new Font("Roboto", Font.PLAIN, 13));
    }

    private void initUI() {
        setTitle("Secretaria do Meio Ambiente - Central de Monitoramento");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // Painel principal com abas
        JTabbedPane tabbedPane = new JTabbedPane();

        // Aba de Chat
        JPanel chatPanel = createChatPanel();

        // Aba de Log
        JPanel logPanel = createLogPanel();

        // Aba de Estatísticas
        JPanel statsPanel = createStatsPanel();

        tabbedPane.addTab("Chat Principal", chatPanel);
        tabbedPane.addTab("Log do Sistema", logPanel);
        tabbedPane.addTab("Estatísticas", statsPanel);

        add(tabbedPane);
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Roboto", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setFixedCellWidth(200);
        userList.setFont(new Font("Roboto", Font.PLAIN, 13));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Menu de contexto para desconectar usuários
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem disconnectItem = new JMenuItem("Desconectar usuário");
        disconnectItem.addActionListener(_ -> {
            String selectedUser = userList.getSelectedValue();
            if (selectedUser != null) {
                int confirm = JOptionPane.showConfirmDialog(
                        ServerApp.this,
                        "Tem certeza que deseja desconectar " + selectedUser + "?",
                        "Confirmar Desconexão",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    disconnectUser(selectedUser);
                }
            }
        });
        popupMenu.add(disconnectItem);

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = userList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        userList.setSelectedIndex(index);
                        popupMenu.show(userList, e.getX(), e.getY());
                    }
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                chatScroll,
                new JScrollPane(userList)
        );
        splitPane.setDividerLocation(700);

        chatPanel.add(splitPane, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.setFont(new Font("Roboto", Font.PLAIN, 14));

        JButton sendButton = new JButton("Enviar");
        sendButton.setFont(new Font("Roboto", Font.BOLD, 12));
        sendButton.setBackground(new Color(0, 122, 204));
        sendButton.setForeground(Color.WHITE);
        sendButton.setPreferredSize(new Dimension(120, 30));

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(messagePanel, BorderLayout.SOUTH);

        sendButton.addActionListener(_ -> sendServerMessage());
        messageField.addActionListener(_ -> sendServerMessage());

        return chatPanel;
    }

    private JPanel createLogPanel() {
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Roboto Mono", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);

        JButton clearButton = new JButton("Limpar Logs");
        clearButton.addActionListener(_ -> logArea.setText(""));

        logPanel.add(logScroll, BorderLayout.CENTER);
        logPanel.add(clearButton, BorderLayout.SOUTH);

        return logPanel;
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        statsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        statsMessageCount = new JLabel("Mensagens trocadas: 0");
        statsInspectors = new JLabel("Inspetores cadastrados: 0");
        statsAlerts = new JLabel("Alertas de poluição: 0");
        JLabel connectionsLabel = new JLabel("Conexões ativas: 0");

        statsMessageCount.setFont(new Font("Roboto", Font.BOLD, 14));
        statsInspectors.setFont(new Font("Roboto", Font.BOLD, 14));
        statsAlerts.setFont(new Font("Roboto", Font.BOLD, 14));
        connectionsLabel.setFont(new Font("Roboto", Font.BOLD, 14));

        // Atualiza estatísticas periodicamente
        new Timer(1000, _ -> {
            connectionsLabel.setText("Conexões ativas: " + clients.size());
            statsInspectors.setText("Inspetores cadastrados: " + listModel.size());
        }).start();

        statsPanel.add(connectionsLabel);
        statsPanel.add(statsMessageCount);
        statsPanel.add(statsInspectors);
        statsPanel.add(statsAlerts);

        return statsPanel;
    }

    private void disconnectUser(String username) {
        synchronized (clients) {
            for (Iterator<ClientHandler> iterator = clients.iterator(); iterator.hasNext(); ) {
                ClientHandler client = iterator.next();
                if (client.getClientName().equals(username)) {
                    try {
                        client.sendMessage("/disconnect");
                        // Força o fechamento da conexão
                        client.socket.close();
                        iterator.remove();

                        log("Administrador desconectou: " + username);
                        chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] " +
                                username + " foi desconectado pelo administrador\n");
                        broadcast(username + " foi desconectado pelo administrador", null);
                        updateUserList();
                    } catch (IOException e) {
                        log("Erro ao desconectar " + username + ": " + e.getMessage());
                    }
                    break;
                }
            }
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(12345);
            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler client = new ClientHandler(clientSocket);
                        clients.add(client);
                        new Thread(client).start();
                    } catch (IOException e) {
                        log("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }).start();

            log("Servidor iniciado na porta 12345");
            chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] Servidor iniciado. Aguardando conexões...\n");

        } catch (IOException e) {
            log("Falha ao iniciar servidor: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Falha ao iniciar servidor: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendServerMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            String formattedMsg = "[SECRETARIA] " + message;
            chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] " + formattedMsg + "\n");
            broadcast(formattedMsg, null);
            messageField.setText("");
            messageCount++;
            statsMessageCount.setText("Mensagens trocadas: " + messageCount);
        }
    }

    void broadcast(String message, ClientHandler exclude) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != exclude) {
                    client.sendMessage(message);
                }
            }
        }
        if (!message.startsWith("[SECRETARIA]")) {
            messageCount++;
            statsMessageCount.setText("Mensagens trocadas: " + messageCount);
        }
    }

    void log(String message) {
        String timestamp = "[" + LocalDateTime.now().format(dtf) + "] ";
        logArea.append(timestamp + message + "\n");
    }

    void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            synchronized (clients) {
                List<String> names = new ArrayList<>();
                for (ClientHandler client : clients) {
                    names.add(client.getClientName());
                }
                Collections.sort(names);
                for (String name : names) {
                    listModel.addElement(name);
                }
            }
        });
    }

    private class ClientHandler implements Runnable {
        public final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private String clientName;
        private final String clientAddress;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.clientAddress = socket.getInetAddress().getHostAddress();
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public String getClientName() {
            return clientName;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                clientName = in.readLine();
                log(clientName + " conectado de " + clientAddress);

                chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] " +
                        clientName + " entrou no chat de inspeção\n");

                updateUserList();
                broadcast(clientName + " entrou no chat de inspeção", this);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("/disconnect")) {
                        break;
                    }

                    if (inputLine.equals("/users")) {
                        sendUserList();
                        continue;
                    }

                    String finalMessage = "[" + dtf.format(LocalDateTime.now()) + "] " +
                            clientName + ": " + inputLine;
                    chatArea.append(finalMessage + "\n");
                    broadcast(finalMessage, this);

                    if (inputLine.toLowerCase().contains("poluição") ||
                            inputLine.toLowerCase().contains("contaminação")) {
                        pollutionAlerts++;
                        statsAlerts.setText("Alertas de poluição: " + pollutionAlerts);
                        String alertMsg = "[" + dtf.format(LocalDateTime.now()) +
                                "] ! ALERTA DE POLUIÇÃO REGISTRADO !";
                        chatArea.append(alertMsg + "\n");
                        broadcast(alertMsg, null);
                    }
                }
            } catch (IOException e) {
                log("Erro na conexão com " + clientName + ": " + e.getMessage());
            } finally {
                disconnectClient();
            }
        }

        private void sendUserList() {
            synchronized (clients) {
                StringBuilder userList = new StringBuilder("/userlist ");
                for (ClientHandler client : clients) {
                    userList.append(client.getClientName()).append(",");
                }
                sendMessage(userList.toString());
            }
        }

        private void disconnectClient() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log("Erro ao fechar conexão com " + clientName);
            }

            clients.remove(this);
            broadcast(clientName + " saiu do chat", this);
            log(clientName + " desconectado (" + clientAddress + ")");
            chatArea.append("[" + dtf.format(LocalDateTime.now()) + "] " +
                    clientName + " saiu do chat\n");
            updateUserList();
        }
    }
}
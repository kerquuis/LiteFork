package com.kerwqui.litefork;

import litebans.api.Entry;
import litebans.api.Events;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.JDABuilder;
import org.bukkit.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class LiteBansListener extends JavaPlugin implements CommandExecutor, Listener {

    private Connection connection;
    private FileConfiguration config;
    private JDA jda;

    @Override
    public void onEnable() {
        // Config dosyasını yükle
        saveDefaultConfig();
        config = getConfig();

        // MySQL veritabanına bağlan
        connectDatabase();

        // Discord botunu başlat
        startDiscordBot();

        // LiteBans olaylarını dinle
        registerLiteBansEvents();

        // Komutları kaydet
        getCommand("unpoints").setExecutor(this);
        getCommand("reload").setExecutor(this);
        getCommand("lf").setExecutor(this);

        // Event dinleyicisini kaydet
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void connectDatabase() {
        String url = config.getString("mysql.url");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");

        try {
            connection = DriverManager.getConnection(url, username, password);
            getLogger().info("MySQL bağlantısı başarılı!");
            setupDatabase();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "MySQL bağlantısı başarısız!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void setupDatabase() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(255) NOT NULL," +
                "event VARCHAR(255) NOT NULL," +
                "points INT NOT NULL," +
                "event_id INT NOT NULL" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableQuery);
            getLogger().info("Veritabanı tabloları başarıyla oluşturuldu!");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Veritabanı tabloları oluşturulurken bir hata oluştu!", e);
        }
    }

    private void startDiscordBot() {
        String discordToken = config.getString("discord.token");

        try {
            jda = JDABuilder.createDefault(discordToken).build();
            jda.awaitReady();
            getLogger().info("Discord botu başarıyla başlatıldı!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Discord botu başlatılamadı!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerLiteBansEvents() {
        Events.get().register(new Events.Listener() {
            @Override
            public void entryAdded(Entry entry) {
                String executorName = entry.getExecutorName();
                String eventType = "";

                switch (entry.getType()) {
                    case "ban":
                        eventType = "Uzaklaştırma";
                        break;
                    case "mute":
                        eventType = "Susturma";
                        break;
                    case "kick":
                        eventType = "Kovulma";
                        break;
                    default:
                        return;
                }

                try {
                    int eventId = addPoints(executorName, eventType, 5);
                    sendDiscordNotification(executorName, eventType, eventId);
                } catch (SQLException e) {
                    getLogger().log(Level.SEVERE, "LiteBans olayı işlenirken bir hata oluştu!", e);
                }
            }
        });
    }

    @Override
    public void onDisable() {
        // Veritabanı bağlantısını kapat
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("MySQL bağlantısı kapatıldı.");
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "MySQL bağlantısı kapatılırken bir hata oluştu!", e);
            }
        }

        // Discord botunu kapat
        if (jda != null) {
            jda.shutdown();
            getLogger().info("Discord botu kapatıldı.");
        }
    }

    private int addPoints(String username, String event, int points) throws SQLException {
        String checkUserQuery = "SELECT points FROM users WHERE username = ?";
        String updatePointsQuery = "UPDATE users SET points = points + ? WHERE username = ?";
        String insertUserQuery = "INSERT INTO users (username, event, points, event_id) VALUES (?, ?, ?, 1)";

        try (PreparedStatement checkUserStmt = connection.prepareStatement(checkUserQuery)) {
            checkUserStmt.setString(1, username);
            ResultSet resultSet = checkUserStmt.executeQuery();

            if (resultSet.next()) {
                int currentPoints = resultSet.getInt("points");
                try (PreparedStatement updatePointsStmt = connection.prepareStatement(updatePointsQuery, Statement.RETURN_GENERATED_KEYS)) {
                    updatePointsStmt.setInt(1, points);
                    updatePointsStmt.setString(2, username);
                    updatePointsStmt.executeUpdate();

                    ResultSet keys = updatePointsStmt.getGeneratedKeys();
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            } else {
                try (PreparedStatement insertUserStmt = connection.prepareStatement(insertUserQuery, Statement.RETURN_GENERATED_KEYS)) {
                    insertUserStmt.setString(1, username);
                    insertUserStmt.setString(2, event);
                    insertUserStmt.setInt(3, points);
                    insertUserStmt.executeUpdate();

                    ResultSet keys = insertUserStmt.getGeneratedKeys();
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        }
        return 0;
    }

    private void sendDiscordNotification(String username, String event, int eventId) {
        if (jda == null) return;

        String title = config.getString("embed.title");
        String description = config.getString("embed.description")
                .replace("{username}", username)
                .replace("{event}", event)
                .replace("{eventId}", String.valueOf(eventId));

        Color color = Color.decode(config.getString("embed.color"));

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(description);
        embedBuilder.setColor(color);

        MessageEmbed embed = embedBuilder.build();

        String channelId = config.getString("discord.channelId");
        jda.getTextChannelById(channelId).sendMessageEmbeds(embed).queue();
    }

    private void sendHelpMessage(Player player) {
        List<String> helpMessage = config.getStringList("messages.help");
        for (String line : helpMessage) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.onlyPlayer")));
            return false;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("lf")) {
            if (args.length == 0) {
                sendHelpMessage(player);
                return false;
            }

            // Eğer alt komut girildiyse, doğrudan işlem yapılmasını sağla
            switch (args[0].toLowerCase()) {
                case "unpoints":
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.invalidUsageMessage")));
                        return false;
                    }

                    String username = args[1];

                    try {
                        removePoints(username);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.successMessage")));
                    } catch (SQLException e) {
                        getLogger().log(Level.SEVERE, "Puan kaldırma işlemi sırasında bir hata oluştu!", e);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.errorMessage")));
                    }
                    return true;

                case "reload":
                    if (!player.hasPermission("litefork.admin")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.noPermission")));
                        return false;
                    }

                    reloadConfig();
                    config = getConfig();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("messages.reloadedPlugin")));
                    return true;

                default:
                    // Geçersiz alt komut durumunda yardım mesajını gönder
                    sendHelpMessage(player);
                    return false; // Hatalı kullanım durumunda false döndür
            }
        }

        return false;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
    }

    private void removePoints(String username) throws SQLException {
        String updatePointsQuery = "UPDATE users SET points = points - 5 WHERE username = ?";

        try (PreparedStatement updatePointsStmt = connection.prepareStatement(updatePointsQuery)) {
            updatePointsStmt.setString(1, username);
            updatePointsStmt.executeUpdate();
        }
    }
}

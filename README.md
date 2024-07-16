# LiteFork Bukkit Plugin

LiteFork, Bukkit tabanlı sunucular için geliştirilmiş bir moderasyon eklentisidir. Bu eklenti, LiteBans API'sini kullanarak sunucu yöneticilerinin olayları izlemelerine ve puanlamalar yapmalarına olanak tanır.

## Özellikler

- LiteBans API entegrasyonu
- MySQL veritabanı desteği
- Discord botu ile entegrasyon
- Türkçe ve İngilizce dil desteği
- Kolay komutlar ve özelleştirilebilir mesajlar

## Gereksinimler

- Java 8 veya üstü
- Bukkit/Spigot sunucu
- MySQL veritabanı

## Kurulum

1. **Eklentiyi Sunucunuza Ekleyin:**
   - `.jar` dosyasını sunucunuzun `plugins` klasörüne ekleyin.
   
2. **MySQL Veritabanını Ayarlayın:**
   - `config.yml` dosyasında MySQL bağlantı bilgilerinizi ayarlayın:
     ```yaml
     mysql:
       url: "jdbc:mysql://localhost:3306/litefork"
       username: "your_mysql_username"
       password: "your_mysql_password"
     ```

3. **Discord Botunu Ayarlayın:**
   - Discord botunun token'ını ve kanal ID'sini `config.yml` dosyasına ekleyin:
     ```yaml
     discord:
       token: "your_discord_bot_token"
       channelId: "your_discord_channel_id"
     ```

4. **Sunucuyu Yeniden Başlatın:**
   - Sunucunuzu yeniden başlatın ve eklentinin yüklenmesini bekleyin.

## Komutlar

- `/lf reload` - Eklentinin dosyalarını yeniden yükler.
- `/lf unpoints <username> <event_id>` - Ceza puanını kaldırır.
- `/lf see <username>` - Oyuncunun puan durumunu görüntüler.
- `/lf testalert` - Discord üzerinde test mesajı gönderir.

## Yardım ve Destek

Eklenti hakkında herhangi bir sorunuz veya geri bildiriminiz varsa Discord sunucumuz üzerinden bize ulaşabilirsiniz: [LiteFork Discord Sunucusu](https://discord.gg/kerdev)

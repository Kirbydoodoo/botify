![# botify](https://raw.githubusercontent.com/robinfriedli/botify/master/resources-public/img/botify-logo-wide.png)
 Discord bot that plays Spotify tracks and YouTube videos or any URL including Soundcloud links and Twitch streams.

* Play and search Spotify tracks and YouTube videos or playlists or play any URL including Soundcloud links and Twitch streams
* Create cross-platform playlists with tracks from any source
* Simple and customisable player commands
* Create custom command presets as shortcuts for your most used commands
* Adjustable properties for even deeper customisation
* Sign in to Spotify to play your own playlists or upload botify playlists
* Manage what roles can access which commands
* Customise how you want to summon your bot by using a custom prefix or giving your bot a name

## Invite it to your guild

https://discordapp.com/api/oauth2/authorize?client_id=483377420494176258&permissions=70315072&scope=bot

## Host it yourself

### 1. Create a Discord app

#### 1.1 Go to https://discordapp.com/developers/applications and create an application
#### 1.2 Click "Bot" on the side menu to create a bot and copy the token for later

### 2. Create a Spotify app

#### 2.1 Go to https://developer.spotify.com/dashboard/applications to create a Spotify application and copy the client id
#### 2.2 Click on "Edit Settings" and whitelist your Redirect URI for the Spotify login
Don't have a domain? You could either go without logins all together and still use most of botify's features or use your
router's public ip and setup port forwarding for your router.

### 3. Create a YouTube Data API project
#### 3.1 Go to https://console.developers.google.com/ and create a project for the YouTube Data API and create and copy the credentials

### 4. Setup botify settings
#### 4.1 Navigate to your cloned project and go to ./resources and open the settings.properties file and fill in the blanks, it should look like this:
```properties
###################
# server settings #
###################
SERVER_PORT=8000
BASE_URI=http://localhost:8000
REDIRECT_URI=http://localhost:8000/login
HIBERNATE_CONFIGURATION=./resources/hibernate.cfg.xml
##########
# tokens #
##########
DISCORD_TOKEN=#copy your discord token here
SPOTIFY_CLIENT_ID=#copy your spotify client id here
SPOTIFY_CLIENT_SECRET=#copy your spotify client secret here
YOUTUBE_CREDENTIALS=#copy your youtube credentials here
#################
# contributions #
#################
PLAYLISTS_PATH=./resources/playlists.xml
GUILD_PLAYLISTS_PATH=./resources/%splaylists.xml
GUILD_SPECIFICATION_PATH=./resources/guildSpecifications.xml
COMMANDS_PATH=./resources/xml-contributions/commands.xml
COMMAND_INTERCEPTORS_PATH=./resources/xml-contributions/commandInterceptors.xml
HTTP_HANDLERS_PATH=./resources/xml-contributions/httpHandlers.xml
STARTUP_TASKS_PATH=./resources/xml-contributions/startupTasks.xml
LOGIN_PAGE_PATH=./resources/html/login.html
LIST_PAGE_PATH=./resources/html/playlist_view.html
ERROR_PAGE_PATH=./resources/html/default_error_page.html
QUEUE_PAGE_PATH=./resources/html/queue_view.html
EMBED_DOCUMENTS_PATH=./resources/xml-contributions/embedDocuments.xml
GUILD_PROPERTIES_PATH=./resources/xml-contributions/guildProperties.xml
###############
# preferences #
###############
MODE_PARTITIONED=true
# playlists per guild (if mode_partitioned = true, else playlist total)
PLAYLIST_COUNT_MAX=50
PLAYLIST_SIZE_MAX=5000
ADMIN_USERS=#define user ids (comma separated) that may access admin commands. These users can always use each command irregardless of access configurations
#######################################
# discordbots.org settings (optional) #
#######################################
#DISCORD_BOT_ID=#copy your discord client id here
#DISCORDBOTS_TOKEN=#copy your discordbots.org token here

```

### 5. Setup database
#### 5.1 Setup hibernate configuration
Navigate to ./resources/hibernate.cfg.xml and adjust the settings, if you use a local postgres server and name your
database botify_playlists you can leave it like this:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-configuration PUBLIC
    "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
    "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">

<hibernate-configuration>
  <session-factory>
    <!-- hibernate config -->
    <property name="hibernate.connection.driver_class">org.postgresql.Driver</property>
    <property name="hibernate.connection.url">jdbc:postgresql://localhost:5432/botify_playlists</property>
    <property name="hibernate.connection.username">postgres</property>
    <property name="hibernate.connection.password">postgres</property>
    <property name="hibernate.dialect">org.hibernate.dialect.PostgreSQL94Dialect</property>
    <property name="show_sql">false</property>
    <property name="hibernate.hbm2ddl.auto">update</property>
    <!-- C3P0 config -->
    <property name="hibernate.c3p0.min_size">5</property>
    <property name="hibernate.c3p0.max_size">20</property>
    <property name="hibernate.c3p0.timeout">1800</property>
    <property name="hibernate.c3p0.max_statements">50</property>
    <!-- annotated classes -->
    <mapping class="net.robinfriedli.botify.entities.Playlist"/>
    <mapping class="net.robinfriedli.botify.entities.Song"/>
    <mapping class="net.robinfriedli.botify.entities.Video"/>
    <mapping class="net.robinfriedli.botify.entities.UrlTrack"/>
    <mapping class="net.robinfriedli.botify.entities.Artist"/>
    <mapping class="net.robinfriedli.botify.entities.PlaylistItem"/>
    <mapping class="net.robinfriedli.botify.entities.CommandHistory"/>
    <mapping class="net.robinfriedli.botify.entities.PlaybackHistory"/>
    <mapping class="net.robinfriedli.botify.entities.Preset"/>
    <mapping class="net.robinfriedli.botify.entities.GuildSpecification"/>
    <mapping class="net.robinfriedli.botify.entities.AccessConfiguration"/>
    <mapping class="net.robinfriedli.botify.entities.GrantedRole"/>
  </session-factory>
</hibernate-configuration>
```
If you need help setting up your postgres server, please refer to their official documentation: http://www.postgresqltutorial.com/
package me.yacine.setHome

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Location
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID
import kotlin.text.get

class SetHome : JavaPlugin() {
    var homes: JsonObject = JsonObject()

    override fun onEnable() {
        homes = readHomes()

        // Register commands
        getCommand("sethome")?.setExecutor(this)
        getCommand("home")?.setExecutor(this)

        logger.info("SetHome plugin enabled!")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players")
            return true
        }

        val player = sender
        val playerId = player.uniqueId.toString()

        when (command.name.lowercase()) {
            "sethome" -> {
                // Save player's current location as home
                saveHome(playerId, player.location)
                player.sendMessage("§aHome location set successfully!")
                return true
            }

            "home" -> {
                // Teleport player to their home
                val playerHome = getPlayerHome(playerId)
                if (playerHome != null) {
                    player.teleport(playerHome)
                    player.sendMessage("§aTeleported to home!")
                } else {
                    player.sendMessage("§cYou haven't set a home yet. Use /sethome first.")
                }
                return true
            }
        }
        return false
    }

    private fun getPlayerHome(playerId: String): Location? {
        val homes = readHomes()

        if (!homes.has(playerId) || !homes.getAsJsonObject(playerId).has("home")) {
            return null
        }

        val homeData = homes.getAsJsonObject(playerId).getAsJsonObject("home")

        val worldName = homeData.get("world").asString
        val world = server.getWorld(worldName) ?: return null

        val x = homeData.get("x").asDouble
        val y = homeData.get("y").asDouble
        val z = homeData.get("z").asDouble
        val yaw = homeData.get("yaw").asFloat
        val pitch = homeData.get("pitch").asFloat

        return Location(world, x, y, z, yaw, pitch)
    }

    fun readHomes(): JsonObject {
        val homesFile = File(dataFolder, "homes.json")

        // Create directory if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Return empty JsonObject if file doesn't exist or is empty
        if (!homesFile.exists() || homesFile.length() == 0L) {
            return JsonObject()
        }

        return try {
            FileReader(homesFile).use { reader ->
                JsonParser.parseReader(reader).asJsonObject
            }
        } catch (e: Exception) {
            logger.warning("Failed to read homes file: ${e.message}")
            JsonObject()
        }
    }

    fun saveHome(playerId: String, location: Location) {
        val homes = readHomes()

        val locationData = JsonObject().apply {
            addProperty("world", location.world?.name)
            addProperty("x", location.x)
            addProperty("y", location.y)
            addProperty("z", location.z)
            addProperty("yaw", location.yaw)
            addProperty("pitch", location.pitch)
        }

        // Add or update the player's home
        if (!homes.has(playerId)) {
            homes.add(playerId, JsonObject())
        }

        // Set the location data directly to the player's ID object
        homes.getAsJsonObject(playerId).add("home", locationData)

        // Ensure data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Write to file
        val homesFile = File(dataFolder, "homes.json")
        FileWriter(homesFile).use { writer ->
            val gson = GsonBuilder().setPrettyPrinting().create()
            writer.write(gson.toJson(homes))
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

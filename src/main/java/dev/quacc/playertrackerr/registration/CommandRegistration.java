package dev.quacc.playertrackerr.registration;

import dev.quacc.playertrackerr.PlayerTrackerr;
import dev.quacc.playertrackerr.command.CommandParameters;
import dev.quacc.playertrackerr.command.PlayerTrackerCommand;
import dev.quacc.playertrackerr.config.ConfigOptionsManager;
import org.bukkit.command.PluginCommand;
import org.reflections.Reflections;

import java.util.*;
import java.util.logging.Level;

public class CommandRegistration {

    private final ConfigOptionsManager configManager;
    private final PlayerTrackerr plugin;

    private final Set<CommandHelpEntry> commandHelp = new HashSet<>();
    public record CommandHelpEntry(String name, String usage, String description) {};
    public Set<CommandHelpEntry> getCommandHelp() { return commandHelp; }

    public CommandRegistration(PlayerTrackerr plugin, ConfigOptionsManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void registerCommands() {
        final Reflections reflections = new Reflections("dev.quacc.playertrackerr.command.impl");
        final Set<Class<? extends PlayerTrackerCommand>> commandClasses = reflections.getSubTypesOf(PlayerTrackerCommand.class);

        commandClasses.forEach(clazz -> {
            try {
                CommandParameters info = clazz.getAnnotation(CommandParameters.class);
                if (info == null) {
                    plugin.getLogger().warning("Missing @CommandInfo annotation for " + clazz.getName());
                    return;
                }

                final PlayerTrackerCommand command = clazz.getConstructor(PlayerTrackerr.class, ConfigOptionsManager.class).newInstance(plugin, configManager);

                register(plugin, info.name(), command);

                Arrays.stream(info.aliases())
                        .filter(alias -> alias != null && !alias.isEmpty())
                        .forEach(alias -> register(plugin, alias, command));

                commandHelp.add(new CommandHelpEntry(info.name(), info.usage(), info.description()));

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to register command: " + clazz.getName(), e);
            }
        });
    }

    private void register(PlayerTrackerr plugin, String name, PlayerTrackerCommand executor) {
        final PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        } else {
            plugin.getLogger().warning("Command missing in plugin.yml: " + name);
        }
    }
}


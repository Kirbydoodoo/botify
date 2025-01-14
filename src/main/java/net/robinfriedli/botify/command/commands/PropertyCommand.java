package net.robinfriedli.botify.command.commands;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.dv8tion.jda.core.EmbedBuilder;
import net.robinfriedli.botify.Botify;
import net.robinfriedli.botify.command.AbstractCommand;
import net.robinfriedli.botify.command.ArgumentContribution;
import net.robinfriedli.botify.command.CommandContext;
import net.robinfriedli.botify.command.CommandManager;
import net.robinfriedli.botify.discord.GuildContext;
import net.robinfriedli.botify.discord.properties.AbstractGuildProperty;
import net.robinfriedli.botify.discord.properties.GuildPropertyManager;
import net.robinfriedli.botify.entities.xml.CommandContribution;
import net.robinfriedli.botify.exceptions.InvalidCommandException;
import net.robinfriedli.botify.util.Table2;

public class PropertyCommand extends AbstractCommand {


    public PropertyCommand(CommandContribution commandContribution, CommandContext context, CommandManager commandManager, String commandString, String identifier, String description) {
        super(commandContribution, context, commandManager, commandString, false, identifier, description, Category.CUSTOMISATION);
    }

    @Override
    public void doRun() {
        if (getCommandBody().isBlank()) {
            listProperties();
        } else {
            if (argumentSet("toggle")) {
                toggleProperty();
            } else {
                setProperty();
            }
        }
    }

    private void setProperty() {
        Pair<String, String> pair = splitInlineArgument("set");
        AbstractGuildProperty property = Botify.get().getGuildPropertyManager().getPropertyByName(pair.getLeft());
        if (property != null) {
            property.set(pair.getRight());
        } else {
            throw new InvalidCommandException("No such property '" + pair.getLeft() + "'");
        }
    }

    private void toggleProperty() {
        AbstractGuildProperty property = Botify.get().getGuildPropertyManager().getPropertyByName(getCommandBody());
        if (property != null) {
            Object value = property.get();
            if (value instanceof Boolean) {
                boolean newBoolValue = !((boolean) value);
                property.set(String.valueOf(newBoolValue));
            } else {
                throw new InvalidCommandException("Value of property '" + property.getName() + "' is not a boolean");
            }
        } else {
            throw new InvalidCommandException("No such property '" + getCommandBody() + "'");
        }
    }

    private void listProperties() {
        GuildPropertyManager guildPropertyManager = Botify.get().getGuildPropertyManager();
        List<AbstractGuildProperty> properties = guildPropertyManager.getProperties();
        GuildContext guildContext = getContext().getGuildContext();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        Table2 table = new Table2(embedBuilder);
        table.addColumn("Name", properties, AbstractGuildProperty::getName);
        table.addColumn("Default Value", properties, AbstractGuildProperty::getDefaultValue);
        table.addColumn("Set Value", properties, property -> String.valueOf(property.extractPersistedValue(guildContext.getSpecification())));
        table.build();
        sendMessage(embedBuilder);
    }

    @Override
    public void onSuccess() {
        // notification sent by GuildPropertyInterceptor
    }

    @Override
    public ArgumentContribution setupArguments() {
        ArgumentContribution argumentContribution = new ArgumentContribution(this);
        argumentContribution.map("toggle")
            .setDescription("Toggles a property with a boolean value (e.g. \"playback notification\") to its opposite value");
        return argumentContribution;
    }
}

package net.robinfriedli.botify.discord.properties;

import java.awt.Color;

import net.robinfriedli.botify.Botify;
import net.robinfriedli.botify.command.CommandContext;
import net.robinfriedli.botify.discord.GuildContext;
import net.robinfriedli.botify.entities.GuildSpecification;
import net.robinfriedli.botify.entities.xml.GuildPropertyContribution;
import net.robinfriedli.botify.exceptions.InvalidPropertyValueException;

public class ColorSchemeProperty extends AbstractGuildProperty {

    public static final Color DEFAULT_FALLBACK = Color.decode("#1DB954");

    public ColorSchemeProperty(GuildPropertyContribution contribution) {
        super(contribution);
    }

    public static Color getColor(GuildContext guildContext) {
        ColorSchemeProperty colorProperty = (ColorSchemeProperty) Botify.get().getGuildPropertyManager().getProperty("color");
        if (colorProperty != null) {
            return colorProperty.getAsColor(guildContext);
        } else {
            return DEFAULT_FALLBACK;
        }
    }

    public static Color getColor() {
        GuildPropertyManager guildPropertyManager = Botify.get().getGuildPropertyManager();
        ColorSchemeProperty colorProperty = (ColorSchemeProperty) guildPropertyManager.getProperty("color");
        if (colorProperty != null) {
            if (CommandContext.Current.isSet()) {
                return colorProperty.getAsColor();
            } else {
                return ColorSchemeProperty.parseColor(colorProperty.getDefaultValue());
            }
        }

        return DEFAULT_FALLBACK;
    }

    private static Color parseColor(String input) {
        try {
            return (Color) Color.class.getField(input).get(null);
        } catch (NoSuchFieldException e) {
            //continue
        } catch (IllegalAccessException e) {
            // should not happen as Color fields are public
        }

        try {
            return Color.decode(input);
        } catch (NumberFormatException e) {
            throw new InvalidPropertyValueException("Cannot decode '" + input + "' to a colour. Please enter a valid Java AWT colour constant or a valid hexadecimal colour code");
        }
    }

    @Override
    public void validate(Object state) {
        String input = (String) state;
        parseColor(input);
    }

    @Override
    public Object process(String input) {
        // the return type of the process method should match the persisted type, so don't return the AWT color yet
        return input;
    }

    @Override
    public void setValue(String value, GuildSpecification guildSpecification) {
        guildSpecification.setColor(value);
    }

    @Override
    public Object extractPersistedValue(GuildSpecification guildSpecification) {
        return guildSpecification.getColor();
    }

    public Color getAsColor() {
        return parseColor((String) get());
    }

    public Color getAsColor(GuildContext guildContext) {
        return parseColor((String) get(guildContext));
    }

}

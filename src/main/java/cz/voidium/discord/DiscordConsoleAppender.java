package cz.voidium.discord;

import cz.voidium.config.DiscordConfig;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

@Plugin(name = "DiscordConsoleAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class DiscordConsoleAppender extends AbstractAppender {

    protected DiscordConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @PluginFactory
    public static DiscordConsoleAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {
        
        if (name == null) {
            name = "DiscordConsole";
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new DiscordConsoleAppender(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        DiscordConfig config = DiscordConfig.getInstance();
        if (config == null || !config.isEnableConsoleLog()) {
            return;
        }
        
        String message = event.getMessage().getFormattedMessage();
        // Basic filtering to avoid loops or spam
        if (message == null || message.isEmpty()) return;
        
        // Send to DiscordManager queue
        DiscordManager.getInstance().queueConsoleMessage(message);
    }
}

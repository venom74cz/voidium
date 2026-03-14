package cz.voidium.web;

import java.io.Serializable;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class WebConsoleAppender extends AbstractAppender {
    public WebConsoleAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        if (event == null || event.getMessage() == null) {
            return;
        }
        String loggerName = event.getLoggerName();
        if (loggerName != null && loggerName.startsWith("net.dv8tion.jda")) {
            return;
        }
        WebConsoleFeed.getInstance().append(
                event.getLevel().name(),
                loggerName == null ? "root" : loggerName,
                event.getMessage().getFormattedMessage());
    }
}
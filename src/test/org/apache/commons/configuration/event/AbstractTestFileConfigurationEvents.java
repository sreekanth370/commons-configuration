/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration.event;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.reloading.ReloadingStrategy;

/**
 * A base test class that can be used for testing file-based configurations.
 * This class tests reload events, too.
 *
 * @version $Id$
 */
public abstract class AbstractTestFileConfigurationEvents extends
        AbstractTestConfigurationEvents
{
    /**
     * Initializes the file configuration for the tests.
     *
     * @throws ConfigurationException if an error occurs
     */
    protected void setUpFileConfiguration() throws ConfigurationException,
            IOException
    {
        FileConfiguration fc = (FileConfiguration) config;
        fc.setReloadingStrategy(new AlwaysReloadingStrategy());
        fc.setURL(getSourceURL());

        // deregister event listener before load because load will cause
        // other events being generated
        config.removeConfigurationListener(l);
        fc.load();
        config.addConfigurationListener(l);
    }

    /**
     * Returns the URL of the file to be loaded. Must be implemented in concrete
     * test classes.
     *
     * @return the URL of the file-based configuration
     * @throws IOException if an error occurs
     */
    protected abstract URL getSourceURL() throws IOException;

    /**
     * Tests events generated by the reload() method.
     */
    public void testReloadEvent() throws ConfigurationException, IOException
    {
        setUpFileConfiguration();
        config.isEmpty(); // This should cause a reload
        l.checkEvent(AbstractFileConfiguration.EVENT_RELOAD, null,
                getSourceURL(), true);
        l.checkEvent(AbstractFileConfiguration.EVENT_RELOAD, null,
                getSourceURL(), false);
        l.done();
    }

    /**
     * Tests events generated by the reload() method when detail events are
     * enabled.
     */
    public void testReloadEventWithDetails() throws ConfigurationException,
            IOException
    {
        setUpFileConfiguration();
        config.setDetailEvents(true);
        config.isEmpty(); // This should cause a reload
        l.checkEventCount(2);
        l.checkEvent(AbstractFileConfiguration.EVENT_RELOAD, null,
                getSourceURL(), true);
        l.skipToLast(AbstractFileConfiguration.EVENT_RELOAD);
        l.checkEvent(AbstractFileConfiguration.EVENT_RELOAD, null,
                getSourceURL(), false);
        l.done();
    }

    /**
     * Tests accessing a property during a reload event to ensure that no
     * infinite loops are possible.
     */
    public void testAccessPropertiesOnReload() throws ConfigurationException,
            IOException
    {
        setUpFileConfiguration();
        config.addConfigurationListener(new ConfigurationListener()
        {
            public void configurationChanged(ConfigurationEvent event)
            {
                config.getString("test");
            }
        });
        config.isEmpty();
        l.checkEvent(AbstractFileConfiguration.EVENT_RELOAD, null,
                getSourceURL(), true);
        l.checkEvent(AbstractFileConfiguration.EVENT_RELOAD, null,
                getSourceURL(), false);
        l.done();
    }

    /**
     * A dummy implementation of the ReloadingStrategy interface. This
     * implementation will always indicate that a reload should be performed. So
     * it can be used for testing reloading events.
     */
    static class AlwaysReloadingStrategy implements ReloadingStrategy
    {
        public void setConfiguration(FileConfiguration configuration)
        {
        }

        public void init()
        {
        }

        public boolean reloadingRequired()
        {
            return true;
        }

        public void reloadingPerformed()
        {
        }
    }
}

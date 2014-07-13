/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for BaseEventSource.
 *
 * @version $Id$
 */
public class TestEventSource
{
    /** Constant for the event type used for testing. */
    static final int TEST_TYPE = 42;

    /** Constant for the event property name. */
    static final String TEST_PROPNAME = "test.property.name";

    /** Constant for the event property value. */
    static final Object TEST_PROPVALUE = "a test property value";

    /** The object under test. */
    private CountingEventSource source;

    @Before
    public void setUp() throws Exception
    {
        source = new CountingEventSource();
    }

    /**
     * Tests a newly created source object.
     */
    @Test
    public void testInit()
    {
        assertTrue("Listeners list is not empty", source
                .getEventListeners(ConfigurationEvent.ANY).isEmpty());
        assertFalse("Removing listener", source.removeEventListener(
                ConfigurationEvent.ANY, new EventListenerTestImpl(null)));
        assertFalse("Detail events are enabled", source.isDetailEvents());
        assertTrue("Error listeners list is not empty", source
                .getErrorListeners().isEmpty());
    }

    /**
     * Tests registering a new listener.
     */
    @Test
    public void testAddEventListener()
    {
        EventListenerTestImpl l = new EventListenerTestImpl(this);
        source.addEventListener(ConfigurationEvent.ANY, l);
        Collection<EventListener<? super ConfigurationEvent>> listeners =
                source.getEventListeners(ConfigurationEvent.ANY);
        assertEquals("Wrong number of listeners", 1, listeners.size());
        assertTrue("Listener not in list", listeners.contains(l));
    }

    /**
     * Tests adding an undefined configuration listener. This should cause an
     * exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddNullConfigurationListener()
    {
        source.addEventListener(ConfigurationEvent.ANY, null);
    }

    /**
     * Tests removing a listener.
     */
    @Test
    public void testRemoveEventListener()
    {
        EventListenerTestImpl l = new EventListenerTestImpl(this);
        assertFalse("Listener can be removed?", source
                .removeEventListener(ConfigurationEvent.ANY, l));
        source.addEventListener(ConfigurationEvent.ADD_NODES, new EventListenerTestImpl(this));
        source.addEventListener(ConfigurationEvent.ANY, l);
        assertFalse("Unknown listener can be removed", source
                .removeEventListener(ConfigurationEvent.ANY, new EventListenerTestImpl(null)));
        assertTrue("Could not remove listener", source
                .removeEventListener(ConfigurationEvent.ANY, l));
        assertFalse("Listener still in list", source
                .getEventListeners(ConfigurationEvent.ANY).contains(l));
    }

    /**
     * Tests if a null listener can be removed. This should be a no-op.
     */
    @Test
    public void testRemoveNullConfigurationListener()
    {
        source.addEventListener(ConfigurationEvent.ANY, new EventListenerTestImpl(null));
        assertFalse("Null listener can be removed", source
                .removeEventListener(ConfigurationEvent.ANY, null));
        assertEquals("Listener list was modified", 1, source
                .getEventListeners(ConfigurationEvent.ANY).size());
    }

    /**
     * Tests whether the listeners list is read only.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testGetConfigurationListenersUpdate()
    {
        source.addEventListener(ConfigurationEvent.ANY,
                new EventListenerTestImpl(null));
        Collection<EventListener<? super ConfigurationEvent>> list =
                source.getEventListeners(ConfigurationEvent.ANY);
        list.clear();
    }

    /**
     * Tests that the collection returned by getEventListeners() is
     * really a snapshot. A later added listener must not be visible.
     */
    @Test
    public void testGetConfigurationListenersAddNew()
    {
        Collection<EventListener<? super ConfigurationEvent>> list =
                source.getEventListeners(ConfigurationEvent.ANY);
        source.addEventListener(ConfigurationEvent.ANY,
                new EventListenerTestImpl(null));
        assertTrue("Listener snapshot not empty", list.isEmpty());
    }

    /**
     * Tests enabling and disabling the detail events flag.
     */
    @Test
    public void testSetDetailEvents()
    {
        source.setDetailEvents(true);
        assertTrue("Detail events are disabled", source.isDetailEvents());
        source.setDetailEvents(true);
        source.setDetailEvents(false);
        assertTrue("Detail events are disabled again", source.isDetailEvents());
        source.setDetailEvents(false);
        assertFalse("Detail events are still enabled", source.isDetailEvents());
    }

    /**
     * Tests delivering an event to a listener.
     */
    @Test
    public void testFireEvent()
    {
        EventListenerTestImpl l = new EventListenerTestImpl(source);
        source.addEventListener(ConfigurationEvent.ANY, l);
        source.fireEvent(ConfigurationEvent.ADD_PROPERTY, TEST_PROPNAME,
                TEST_PROPVALUE, true);
        l.checkEvent(ConfigurationEvent.ADD_PROPERTY, TEST_PROPNAME,
                TEST_PROPVALUE, true);
        l.done();
    }

    /**
     * Tests firing an event if there are no listeners.
     */
    @Test
    public void testFireEventNoListeners()
    {
        source.fireEvent(ConfigurationEvent.ADD_NODES, TEST_PROPNAME, TEST_PROPVALUE, false);
        assertEquals("An event object was created", 0, source.eventCount);
    }

    /**
     * Tests generating a detail event if detail events are not allowed.
     */
    @Test
    public void testFireEventNoDetails()
    {
        EventListenerTestImpl l = new EventListenerTestImpl(source);
        source.addEventListener(ConfigurationEvent.ANY, l);
        source.setDetailEvents(false);
        source.fireEvent(ConfigurationEvent.SET_PROPERTY, TEST_PROPNAME, TEST_PROPVALUE, false);
        assertEquals("Event object was created", 0, source.eventCount);
        l.done();
    }

    /**
     * Tests whether an event listener can deregister itself in reaction of a
     * delivered event.
     */
    @Test
    public void testRemoveListenerInFireEvent()
    {
        EventListener<ConfigurationEvent> lstRemove = new EventListener<ConfigurationEvent>()
        {
            @Override
            public void onEvent(ConfigurationEvent event)
            {
                source.removeEventListener(ConfigurationEvent.ANY, this);
            }
        };

        source.addEventListener(ConfigurationEvent.ANY, lstRemove);
        EventListenerTestImpl l = new EventListenerTestImpl(source);
        source.addEventListener(ConfigurationEvent.ANY, l);
        source.fireEvent(ConfigurationEvent.ADD_PROPERTY, TEST_PROPNAME,
                TEST_PROPVALUE, false);
        l.checkEvent(ConfigurationEvent.ADD_PROPERTY, TEST_PROPNAME,
                TEST_PROPVALUE, false);
        assertEquals("Listener was not removed", 1,
                source.getEventListeners(ConfigurationEvent.ANY).size());
    }

    /**
     * Tests registering a new error listener.
     */
    @Test
    public void testAddErrorListener()
    {
        TestListener l = new TestListener();
        source.addErrorListener(l);
        Collection<ConfigurationErrorListener> listeners = source.getErrorListeners();
        assertEquals("Wrong number of listeners", 1, listeners.size());
        assertTrue("Listener not in list", listeners.contains(l));
    }

    /**
     * Tests adding an undefined error listener. This should cause an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddNullErrorListener()
    {
        source.addErrorListener(null);
    }

    /**
     * Tests removing an error listener.
     */
    @Test
    public void testRemoveErrorListener()
    {
        TestListener l = new TestListener();
        assertFalse("Listener can be removed?", source.removeErrorListener(l));
        source.addErrorListener(l);
        source.addErrorListener(new TestListener());
        assertFalse("Unknown listener can be removed", source
                .removeErrorListener(new TestListener()));
        assertTrue("Could not remove listener", source.removeErrorListener(l));
        assertFalse("Listener still in list", source.getErrorListeners()
                .contains(l));
    }

    /**
     * Tests if a null error listener can be removed. This should be a no-op.
     */
    @Test
    public void testRemoveNullErrorListener()
    {
        source.addErrorListener(new TestListener());
        assertFalse("Null listener can be removed", source
                .removeErrorListener(null));
        assertEquals("Listener list was modified", 1, source
                .getErrorListeners().size());
    }

    /**
     * Tests whether the listeners list is read only.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testGetErrorListenersUpdate()
    {
        source.addErrorListener(new TestListener());
        Collection<ConfigurationErrorListener> list = source.getErrorListeners();
        list.clear();
    }

    /**
     * Tests delivering an error event to a listener.
     */
    @Test
    public void testFireError()
    {
        TestListener l = new TestListener();
        source.addErrorListener(l);
        Exception testException = new Exception("A test");
        source.fireError(TEST_TYPE, TEST_PROPNAME, TEST_PROPVALUE,
                testException);
        assertEquals("Not 1 event created", 1, source.errorCount);
        assertEquals("Error listener not called once", 1, l.numberOfErrors);
        //assertEquals("Wrong event type", TEST_TYPE, l.lastEvent.getType());
        assertEquals("Wrong property name", TEST_PROPNAME, l.lastEvent
                .getPropertyName());
        assertEquals("Wrong property value", TEST_PROPVALUE, l.lastEvent
                .getPropertyValue());
        assertEquals("Wrong Throwable object", testException,
                l.lastEvent.getCause());
    }

    /**
     * Tests firing an error event if there are no error listeners.
     */
    @Test
    public void testFireErrorNoListeners()
    {
        source.fireError(TEST_TYPE, TEST_PROPNAME, TEST_PROPVALUE,
                new Exception());
        assertEquals("An error event object was created", 0, source.errorCount);
    }

    /**
     * Tests cloning an event source object. The registered listeners should not
     * be registered at the clone.
     */
    @Test
    public void testClone() throws CloneNotSupportedException
    {
        source.addEventListener(ConfigurationEvent.ANY, new EventListenerTestImpl(source));
        BaseEventSource copy = (BaseEventSource) source.clone();
        assertTrue("Configuration listeners registered for clone", copy
                .getEventListeners(ConfigurationEvent.ANY).isEmpty());
        assertTrue("Error listeners registered for clone", copy
                .getErrorListeners().isEmpty());
    }

    /**
     * Tests whether all event listeners can be removed.
     */
    @Test
    public void testClearEventListeners()
    {
        source.addEventListener(ConfigurationEvent.ANY,
                new EventListenerTestImpl(source));
        source.addEventListener(ConfigurationEvent.ANY_HIERARCHICAL,
                new EventListenerTestImpl(source));

        source.clearEventListeners();
        assertTrue("Got ANY listeners",
                source.getEventListeners(ConfigurationEvent.ANY).isEmpty());
        assertTrue("Got HIERARCHICAL listeners",
                source.getEventListeners(ConfigurationEvent.ANY_HIERARCHICAL)
                        .isEmpty());
    }

    /**
     * Tests whether event listeners can be copied to another source.
     */
    @Test
    public void testCopyEventListeners()
    {
        EventListenerTestImpl l1 = new EventListenerTestImpl(source);
        EventListenerTestImpl l2 = new EventListenerTestImpl(source);
        source.addEventListener(ConfigurationEvent.ANY, l1);
        source.addEventListener(ConfigurationEvent.ANY_HIERARCHICAL, l2);

        BaseEventSource source2 = new BaseEventSource();
        source.copyEventListeners(source2);
        Collection<EventListener<? super ConfigurationEvent>> listeners =
                source2.getEventListeners(ConfigurationEvent.ANY_HIERARCHICAL);
        assertEquals("Wrong number of listeners (1)", 2, listeners.size());
        assertTrue("l1 not found", listeners.contains(l1));
        assertTrue("l2 not found", listeners.contains(l2));
        listeners = source2.getEventListeners(ConfigurationEvent.ANY);
        assertEquals("Wrong number of listeners (2)", 1, listeners.size());
        assertTrue("Wrong listener", listeners.contains(l1));
    }

    /**
     * Tries to copy event listeners to a null source.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCopyEventListenersNullSource()
    {
        source.copyEventListeners(null);
    }

    /**
     * Tests whether all error listeners can be cleared.
     */
    @Test
    public void testClearErrorListeners()
    {
        EventListener<ConfigurationEvent> cl = new EventListenerTestImpl(null);
        ErrorListenerTestImpl el1 = new ErrorListenerTestImpl(null);
        ErrorListenerTestImpl el2 = new ErrorListenerTestImpl(null);
        ErrorListenerTestImpl el3 = new ErrorListenerTestImpl(null);
        source.addEventListener(ConfigurationErrorEvent.READ, el1);
        source.addEventListener(ConfigurationErrorEvent.ANY, el2);
        source.addEventListener(ConfigurationEvent.ANY, cl);
        source.addEventListener(ConfigurationErrorEvent.WRITE, el3);

        source.clearErrorListeners();
        List<EventListenerRegistrationData<?>> regs =
                source.getEventListenerRegistrations();
        assertEquals("Wrong number of event listener registrations", 1,
                regs.size());
        assertSame("Wrong remaining listener", cl, regs.get(0).getListener());
    }

    /**
     * A test event listener implementation.
     */
    static class TestListener implements ConfigurationErrorListener
    {
        ConfigurationErrorEvent lastEvent;

        int numberOfErrors;

        @Override
        public void configurationError(ConfigurationErrorEvent event)
        {
            lastEvent = event;
            numberOfErrors++;
        }
    }

    /**
     * A specialized event source implementation that counts the number of
     * created event objects. It is used to test whether the
     * {@code fireEvent()} methods only creates event objects if
     * necessary. It also allows testing the clone() operation.
     */
    private static class CountingEventSource extends BaseEventSource implements Cloneable
    {
        int eventCount;

        int errorCount;

        @Override
        protected <T extends ConfigurationEvent> ConfigurationEvent createEvent(
                EventType<T> eventType, String propName, Object propValue,
                boolean before)
        {
            eventCount++;
            return super.createEvent(eventType, propName, propValue, before);
        }

        @Override
        protected ConfigurationErrorEvent createErrorEvent(int type,
                String propName, Object value, Throwable ex)
        {
            errorCount++;
            return super.createErrorEvent(type, propName, value, ex);
        }
    }
}

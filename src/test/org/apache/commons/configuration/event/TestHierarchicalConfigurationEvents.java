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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.DefaultConfigurationNode;

/**
 * Test class for the events generated by hierarchical configurations.
 *
 * @version $Id$
 */
public class TestHierarchicalConfigurationEvents extends
        AbstractTestConfigurationEvents
{
    protected AbstractConfiguration createConfiguration()
    {
        return new HierarchicalConfiguration();
    }

    /**
     * Tests events generated by the clearTree() method.
     */
    public void testClearTreeEvent()
    {
        HierarchicalConfiguration hc = (HierarchicalConfiguration) config;
        String key = EXIST_PROPERTY.substring(0, EXIST_PROPERTY.indexOf('.'));
        Collection nodes = hc.getExpressionEngine()
                .query(hc.getRootNode(), key);
        hc.clearTree(key);
        l.checkEvent(HierarchicalConfiguration.EVENT_CLEAR_TREE, key, null,
                true);
        l.checkEvent(HierarchicalConfiguration.EVENT_CLEAR_TREE, key, nodes,
                false);
        l.done();
    }

    /**
     * Tests events generated by the addNodes() method.
     */
    public void testAddNodesEvent()
    {
        HierarchicalConfiguration hc = (HierarchicalConfiguration) config;
        Collection nodes = new ArrayList(1);
        nodes.add(new DefaultConfigurationNode("a_key", TEST_PROPVALUE));
        hc.addNodes(TEST_PROPNAME, nodes);
        l.checkEvent(HierarchicalConfiguration.EVENT_ADD_NODES, TEST_PROPNAME,
                nodes, true);
        l.checkEvent(HierarchicalConfiguration.EVENT_ADD_NODES, TEST_PROPNAME,
                nodes, false);
        l.done();
    }

    /**
     * Tests events generated by addNodes() when the list of nodes is empty. In
     * this case no events should be generated.
     */
    public void testAddNodesEmptyEvent()
    {
        ((HierarchicalConfiguration) config).addNodes(TEST_PROPNAME,
                new ArrayList());
        l.done();
    }
}

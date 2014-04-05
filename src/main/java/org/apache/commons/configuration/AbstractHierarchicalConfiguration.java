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

package org.apache.commons.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.configuration.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration.sync.LockMode;
import org.apache.commons.configuration.sync.NoOpSynchronizer;
import org.apache.commons.configuration.sync.Synchronizer;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.configuration.tree.ConfigurationNodeVisitorAdapter;
import org.apache.commons.configuration.tree.DefaultExpressionEngine;
import org.apache.commons.configuration.tree.ExpressionEngine;
import org.apache.commons.configuration.tree.NodeAddData;
import org.apache.commons.configuration.tree.NodeHandler;
import org.apache.commons.configuration.tree.NodeKeyResolver;
import org.apache.commons.configuration.tree.NodeModel;
import org.apache.commons.configuration.tree.NodeTreeWalker;
import org.apache.commons.configuration.tree.NodeUpdateData;
import org.apache.commons.configuration.tree.QueryResult;

/**
 * <p>A specialized configuration class that extends its base class by the
 * ability of keeping more structure in the stored properties.</p>
 * <p>There
 * are some sources of configuration data that cannot be stored very well in a
 * {@code BaseConfiguration} object because then their structure is lost.
 * This is for instance true for XML documents. This class can deal with such
 * structured configuration sources by storing the properties in a tree-like
 * organization. The exact storage structure of the underlying data does not
 * matter for the configuration instance; it uses a {@link NodeModel} object
 * for accessing it.</p>
 * <p>The hierarchical organization allows for a more
 * sophisticated access to single properties. As an example consider the
 * following XML document:</p>
 * <p>
 *
 * <pre>
 * &lt;database&gt;
 *   &lt;tables&gt;
 *     &lt;table&gt;
 *       &lt;name&gt;users&lt;/name&gt;
 *       &lt;fields&gt;
 *         &lt;field&gt;
 *           &lt;name&gt;lid&lt;/name&gt;
 *           &lt;type&gt;long&lt;/name&gt;
 *         &lt;/field&gt;
 *         &lt;field&gt;
 *           &lt;name&gt;usrName&lt;/name&gt;
 *           &lt;type&gt;java.lang.String&lt;/type&gt;
 *         &lt;/field&gt;
 *        ...
 *       &lt;/fields&gt;
 *     &lt;/table&gt;
 *     &lt;table&gt;
 *       &lt;name&gt;documents&lt;/name&gt;
 *       &lt;fields&gt;
 *         &lt;field&gt;
 *           &lt;name&gt;docid&lt;/name&gt;
 *           &lt;type&gt;long&lt;/type&gt;
 *         &lt;/field&gt;
 *         ...
 *       &lt;/fields&gt;
 *     &lt;/table&gt;
 *     ...
 *   &lt;/tables&gt;
 * &lt;/database&gt;
 * </pre>
 *
 * </p>
 * <p>If this document is parsed and stored in a hierarchical configuration
 * object (which can be done by one of
 * the sub classes), there are enhanced possibilities of accessing properties.
 * Per default, the keys for querying information can contain indices that select a specific
 * element if there are multiple hits.</p>
 * <p>For instance the key
 * {@code tables.table(0).name} can be used to find out the name of the
 * first table. In opposite {@code tables.table.name} would return a
 * collection with the names of all available tables. Similarly the key
 * {@code tables.table(1).fields.field.name} returns a collection with
 * the names of all fields of the second table. If another index is added after
 * the {@code field} element, a single field can be accessed:
 * {@code tables.table(1).fields.field(0).name}.</p>
 * <p>There is a
 * {@code getMaxIndex()} method that returns the maximum allowed index
 * that can be added to a given property key. This method can be used to iterate
 * over all values defined for a certain property.</p>
 * <p>Since the 1.3 release of <em>Commons Configuration</em> hierarchical
 * configurations support an <em>expression engine</em>. This expression engine
 * is responsible for evaluating the passed in configuration keys and map them
 * to the stored properties. The examples above are valid for the default
 * expression engine, which is used when a new {@code AbstractHierarchicalConfiguration}
 * instance is created. With the {@code setExpressionEngine()} method a
 * different expression engine can be set. For instance with
 * {@link org.apache.commons.configuration.tree.xpath.XPathExpressionEngine}
 * there is an expression engine available that supports configuration keys in
 * XPATH syntax.</p>
 * <p>In addition to the events common for all configuration classes hierarchical
 * configurations support some more events that correspond to some specific
 * methods and features:
 * <dl><dt><em>EVENT_ADD_NODES</em></dt><dd>The {@code addNodes()} method
 * was called; the event object contains the key, to which the nodes were added,
 * and a collection with the new nodes as value.</dd>
 * <dt><em>EVENT_CLEAR_TREE</em></dt><dd>The {@code clearTree()} method was
 * called; the event object stores the key of the removed sub tree.</dd>
 * <dt><em>EVENT_SUBNODE_CHANGED</em></dt><dd>A {@code SubnodeConfiguration}
 * that was created from this configuration has been changed. The value property
 * of the event object contains the original event object as it was sent by the
 * subnode configuration.</dd></dl></p>
 * <p>
 * Whether an {@code AbstractHierarchicalConfiguration} object is thread-safe or not
 * depends on the underlying {@code NodeModel} and the {@link Synchronizer} it is associated with.
 * Some {@code NodeModel} implementations are inherently thread-safe; they do not require
 * a special {@code Synchronizer}. (Per default, a
 * dummy {@code Synchronizer} is used which is not thread-safe!) The methods
 * for querying or updating configuration data invoke this {@code Synchronizer}
 * accordingly. There is one exception to this rule: The {@link #getRootNode()}
 * method is not guarded using the {@code Synchronizer}. This is due to the
 * fact that the caller can do anything with this root node, so it is not
 * clear which kind of synchronization should be performed. So when accessing
 * the configuration's root node directly, the client application is responsible
 * for proper synchronization. This is achieved by calling the methods
 * {@link #lock(LockMode)}, and {@link #unlock(LockMode)} with a proper
 * {@link LockMode} argument. In any case, it is recommended to not access the
 * root node directly, but to use corresponding methods for querying or
 * updating configuration data instead. Direct manipulations of a
 * configuration's node structure circumvent many internal mechanisms and thus
 * can cause undesired effects. For concrete subclasses dealing with specific
 * node structures, this situation may be different.
 * </p>
 *
 * @version $Id$
 * @since 2.0
 * @param  <T> the type of the nodes managed by this hierarchical configuration
 */
public abstract class AbstractHierarchicalConfiguration<T> extends AbstractConfiguration
    implements Cloneable, NodeKeyResolver<T>, HierarchicalConfiguration<T>
{
    /**
     * Constant for the clear tree event.
     * @since 1.3
     */
    public static final int EVENT_CLEAR_TREE = 10;

    /**
     * Constant for the add nodes event.
     * @since 1.3
     */
    public static final int EVENT_ADD_NODES = 11;

    /**
     * Constant for the subnode configuration modified event.
     * @since 1.5
     */
    public static final int EVENT_SUBNODE_CHANGED = 12;

    /** The model for managing the data stored in this configuration. */
    private NodeModel<T> model;

    /** Stores the expression engine for this instance.*/
    private ExpressionEngine expressionEngine;

    /**
     * Creates a new instance of {@code AbstractHierarchicalConfiguration} and
     * sets the {@code NodeModel} to be used.
     *
     * @param nodeModel the {@code NodeModel}
     */
    protected AbstractHierarchicalConfiguration(NodeModel<T> nodeModel)
    {
        model = nodeModel;
    }

    /**
     * Returns the root node of this hierarchical configuration.
     *
     * @return the root node
     */
    public T getRootNode()
    {
        return getModel().getNodeHandler().getRootNode();
    }

    /**
     * Sets the root node of this hierarchical configuration.
     *
     * @param rootNode the root node
     */
    public final void setRootNode(T rootNode)
    {
        if (rootNode == null)
        {
            throw new IllegalArgumentException("Root node must not be null!");
        }

        beginWrite(false);
        try
        {
            setRootNodeInternal(rootNode);
        }
        finally
        {
            endWrite();
        }
    }

    /**
     * Actually sets the root node of this configuration. This method is called
     * by {@code setRootNode()}. Subclasses that need to adapt this operation
     * can override it.
     *
     * @param rootNode the new root node of this configuration
     */
    protected void setRootNodeInternal(T rootNode)
    {
        getModel().setRootNode(rootNode);
    }

    /**
     * {@inheritDoc} This implementation handles synchronization and delegates
     * to {@code getRootElementNameInternal()}.
     */
    public final String getRootElementName()
    {
        beginRead(false);
        try
        {
            return getRootElementNameInternal();
        }
        finally
        {
            endRead();
        }
    }

    /**
     * Actually obtains the name of the root element. This method is called by
     * {@code getRootElementName()}. It just returns the name of the root node.
     * Subclasses that treat the root element name differently can override this
     * method.
     *
     * @return the name of this configuration's root element
     */
    protected String getRootElementNameInternal()
    {
        return getModel().getNodeHandler().nodeName(getRootNode());
    }

    /**
     * Returns the expression engine used by this configuration. This method
     * will never return <b>null</b>; if no specific expression engine was set,
     * the default expression engine will be returned.
     *
     * @return the current expression engine
     * @since 1.3
     */
    public ExpressionEngine getExpressionEngine()
    {
        return (expressionEngine != null) ? expressionEngine
                : DefaultExpressionEngine.INSTANCE;
    }

    /**
     * Sets the expression engine to be used by this configuration. All property
     * keys this configuration has to deal with will be interpreted by this
     * engine.
     *
     * @param expressionEngine the new expression engine; can be <b>null</b>,
     * then the default expression engine will be used
     * @since 1.3
     */
    public void setExpressionEngine(ExpressionEngine expressionEngine)
    {
        this.expressionEngine = expressionEngine;
    }

    /**
     * Fetches the specified property. This task is delegated to the associated
     * expression engine.
     *
     * @param key the key to be looked up
     * @return the found value
     */
    @Override
    protected Object getPropertyInternal(String key)
    {
        List<QueryResult<T>> results = fetchNodeList(key);

        if (results.isEmpty())
        {
            return null;
        }
        else
        {
            NodeHandler<T> handler = getModel().getNodeHandler();
            List<Object> list = new ArrayList<Object>();
            for (QueryResult<T> result : results)
            {
                Object value = valueFromResult(result, handler);
                if (value != null)
                {
                    list.add(value);
                }
            }

            if (list.size() < 1)
            {
                return null;
            }
            else
            {
                return (list.size() == 1) ? list.get(0) : list;
            }
        }
    }

    /**
     * Adds the property with the specified key. This task will be delegated to
     * the associated {@code ExpressionEngine}, so the passed in key
     * must match the requirements of this implementation.
     *
     * @param key the key of the new property
     * @param obj the value of the new property
     */
    @Override
    protected void addPropertyInternal(String key, Object obj)
    {
        getModel().addProperty(key, getListDelimiterHandler().parse(obj), this);
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        // will not be called
    }

    /**
     * Adds a collection of nodes at the specified position of the configuration
     * tree. This method works similar to {@code addProperty()}, but
     * instead of a single property a whole collection of nodes can be added -
     * and thus complete configuration sub trees. E.g. with this method it is
     * possible to add parts of another {@code BaseHierarchicalConfiguration}
     * object to this object. (However be aware that a
     * {@code ConfigurationNode} object can only belong to a single
     * configuration. So if nodes from one configuration are directly added to
     * another one using this method, the structure of the source configuration
     * will be broken. In this case you should clone the nodes to be added
     * before calling {@code addNodes()}.) If the passed in key refers to
     * an existing and unique node, the new nodes are added to this node.
     * Otherwise a new node will be created at the specified position in the
     * hierarchy. Implementation node: This method performs some book-keeping
     * and then delegates to {@code addNodesInternal()}.
     *
     * @param key the key where the nodes are to be added; can be <b>null</b>,
     * then they are added to the root node
     * @param nodes a collection with the {@code Node} objects to be
     * added
     */
    public final void addNodes(String key, Collection<? extends T> nodes)
    {
        if (nodes == null || nodes.isEmpty())
        {
            return;
        }

        beginWrite(false);
        try
        {
            fireEvent(EVENT_ADD_NODES, key, nodes, true);
            addNodesInternal(key, nodes);
            fireEvent(EVENT_ADD_NODES, key, nodes, false);
        }
        finally
        {
            endWrite();
        }
    }

    /**
     * Actually adds a collection of new nodes to this configuration. This
     * method is called by {@code addNodes()}. It can be overridden by
     * subclasses that need to adapt this operation.
     *
     * @param key the key where the nodes are to be added; can be <b>null</b>,
     *        then they are added to the root node
     * @param nodes a collection with the {@code Node} objects to be added
     * @since 2.0
     */
    protected void addNodesInternal(String key, Collection<? extends T> nodes)
    {
        getModel().addNodes(key, nodes, this);
    }

    /**
     * Checks if this configuration is empty. Empty means that there are no keys
     * with any values, though there can be some (empty) nodes.
     *
     * @return a flag if this configuration is empty
     */
    @Override
    protected boolean isEmptyInternal()
    {
        return !nodeDefined(getRootNode());
    }

    /**
     * Checks if the specified key is contained in this configuration. Note that
     * for this configuration the term &quot;contained&quot; means that the key
     * has an associated value. If there is a node for this key that has no
     * value but children (either defined or undefined), this method will still
     * return <b>false </b>.
     *
     * @param key the key to be checked
     * @return a flag if this key is contained in this configuration
     */
    @Override
    protected boolean containsKeyInternal(String key)
    {
        return getPropertyInternal(key) != null;
    }

    /**
     * Sets the value of the specified property.
     *
     * @param key the key of the property to set
     * @param value the new value of this property
     */
    @Override
    protected void setPropertyInternal(String key, Object value)
    {
        getModel().setProperty(key, value, this);
    }

    /**
     * {@inheritDoc} This implementation delegates to the expression engine.
     */
    public List<QueryResult<T>> resolveKey(T root, String key,
            NodeHandler<T> handler)
    {
        return getExpressionEngine().query(root, key, handler);
    }

    /**
     * {@inheritDoc} This implementation delegates to {@code resolveKey()} and
     * then filters out attribute results.
     */
    public List<T> resolveNodeKey(T root, String key, NodeHandler<T> handler)
    {
        List<QueryResult<T>> results = resolveKey(root, key, handler);
        List<T> targetNodes = new LinkedList<T>();
        for (QueryResult<T> result : results)
        {
            if (!result.isAttributeResult())
            {
                targetNodes.add(result.getNode());
            }
        }
        return targetNodes;
    }

    /**
     * {@inheritDoc} This implementation delegates to the expression engine.
     */
    public NodeAddData<T> resolveAddKey(T root, String key,
            NodeHandler<T> handler)
    {
        return getExpressionEngine().prepareAdd(root, key, handler);
    }

    /**
     * {@inheritDoc} This implementation executes a query for the given key and
     * constructs a {@code NodeUpdateData} object based on the results. It
     * determines which nodes need to be changed and whether new ones need to be
     * added or existing ones need to be removed.
     */
    public NodeUpdateData<T> resolveUpdateKey(T root, String key,
            Object newValue, NodeHandler<T> handler)
    {
        Iterator<QueryResult<T>> itNodes = fetchNodeList(key).iterator();
        Iterator<?> itValues = getListDelimiterHandler().parse(newValue).iterator();
        Map<QueryResult<T>, Object> changedValues =
                new HashMap<QueryResult<T>, Object>();
        Collection<Object> additionalValues = null;
        Collection<QueryResult<T>> removedItems = null;

        while (itNodes.hasNext() && itValues.hasNext())
        {
            changedValues.put(itNodes.next(), itValues.next());
        }

        // Add additional nodes if necessary
        if (itValues.hasNext())
        {
            additionalValues = new LinkedList<Object>();
            while (itValues.hasNext())
            {
                additionalValues.add(itValues.next());
            }
        }

        // Remove remaining nodes
        if (itNodes.hasNext())
        {
            removedItems = new LinkedList<QueryResult<T>>();
            while (itNodes.hasNext())
            {
                removedItems.add(itNodes.next());
            }
        }

        return new NodeUpdateData<T>(changedValues, additionalValues,
                removedItems, key);
    }

    /**
     * {@inheritDoc} This implementation uses the expression engine to generate a
     * canonical key for the passed in node. For this purpose, the path to the
     * root node has to be traversed. The cache is used to store and access keys
     * for nodes encountered on the path.
     */
    public String nodeKey(T node, Map<T, String> cache, NodeHandler<T> handler) {
        List<T> path = new LinkedList<T>();
        T currentNode = node;
        String key = cache.get(node);
        while (key == null && currentNode != null)
        {
            path.add(0, currentNode);
            currentNode = handler.getParent(currentNode);
            key = cache.get(currentNode);
        }

        for (T n : path)
        {
            String currentKey = getExpressionEngine().canonicalKey(n, key, handler);
            cache.put(n, currentKey);
            key = currentKey;
        }

        return key;
    }

    /**
     * Clears this configuration. This is a more efficient implementation than
     * the one inherited from the base class. It delegates to the node model.
     */
    @Override
    protected void clearInternal()
    {
        getModel().clear(this);
    }

    /**
     * Removes all values of the property with the given name and of keys that
     * start with this name. So if there is a property with the key
     * &quot;foo&quot; and a property with the key &quot;foo.bar&quot;, a call
     * of {@code clearTree("foo")} would remove both properties.
     *
     * @param key the key of the property to be removed
     */
    public final void clearTree(String key)
    {
        beginWrite(false);
        try
        {
            fireEvent(EVENT_CLEAR_TREE, key, null, true);
            List<ConfigurationNode> nodes = clearTreeInternal(key);
            fireEvent(EVENT_CLEAR_TREE, key, nodes, false);
        }
        finally
        {
            endWrite();
        }
    }

    /**
     * Actually clears the tree of elements referenced by the given key. This
     * method is called by {@code clearTree()}. Subclasses that need to adapt
     * this operation can override this method. This base implementation
     * delegates to the node model.
     *
     * @param key the key of the property to be removed
     * @return a collection with the nodes that have been removed (this is
     *         needed for firing a meaningful event of type EVENT_CLEAR_TREE)
     * @since 2.0
     */
    protected List<ConfigurationNode> clearTreeInternal(String key)
    {
        getModel().clearTree(key, this);
        //TODO return something meaningful
        return null;
    }

    /**
     * Removes the property with the given key. Properties with names that start
     * with the given key (i.e. properties below the specified key in the
     * hierarchy) won't be affected. This implementation delegates to the node+
     * model.
     *
     * @param key the key of the property to be removed
     */
    @Override
    protected void clearPropertyDirect(String key)
    {
        getModel().clearProperty(key, this);
    }

    /**
     * Returns an iterator with all keys defined in this configuration.
     * Note that the keys returned by this method will not contain any
     * indices. This means that some structure will be lost.</p>
     *
     * @return an iterator with the defined keys in this configuration
     */
    @Override
    protected Iterator<String> getKeysInternal()
    {
        DefinedKeysVisitor<T> visitor = new DefinedKeysVisitor<T>();
        NodeTreeWalker.INSTANCE.walkDFS(getRootNode(), visitor,
                getModel().getNodeHandler());

        return visitor.getKeyList().iterator();
    }

    /**
     * Returns an iterator with all keys defined in this configuration that
     * start with the given prefix. The returned keys will not contain any
     * indices. This implementation tries to locate a node whose key is the same
     * as the passed in prefix. Then the subtree of this node is traversed, and
     * the keys of all nodes encountered (including attributes) are added to the
     * result set.
     *
     * @param prefix the prefix of the keys to start with
     * @return an iterator with the found keys
     */
    @Override
    protected Iterator<String> getKeysInternal(String prefix)
    {
        DefinedKeysVisitor<T> visitor = new DefinedKeysVisitor<T>(prefix);
        if (containsKey(prefix))
        {
            // explicitly add the prefix
            visitor.getKeyList().add(prefix);
        }

        List<QueryResult<T>> results = fetchNodeList(prefix);
        NodeHandler<T> handler = getModel().getNodeHandler();

        for (QueryResult<T> result : results)
        {
            if (!result.isAttributeResult())
            {
                for (T c : handler.getChildren(result.getNode()))
                {
                    NodeTreeWalker.INSTANCE.walkDFS(c, visitor, handler);
                }
                visitor.handleAttributeKeys(prefix, result.getNode(), handler);
            }
        }

        return visitor.getKeyList().iterator();
    }

    /**
     * Returns the maximum defined index for the given key. This is useful if
     * there are multiple values for this key. They can then be addressed
     * separately by specifying indices from 0 to the return value of this
     * method. If the passed in key is not contained in this configuration,
     * result is -1.
     *
     * @param key the key to be checked
     * @return the maximum defined index for this key
     */
    public final int getMaxIndex(String key)
    {
        beginRead(false);
        try
        {
            return getMaxIndexInternal(key);
        }
        finally
        {
            endRead();
        }
    }

    /**
     * Actually retrieves the maximum defined index for the given key. This
     * method is called by {@code getMaxIndex()}. Subclasses that need to adapt
     * this operation have to override this method.
     *
     * @param key the key to be checked
     * @return the maximum defined index for this key
     * @since 2.0
     */
    protected int getMaxIndexInternal(String key)
    {
        return fetchNodeList(key).size() - 1;
    }

    /**
     * Creates a copy of this object. This new configuration object will contain
     * copies of all nodes in the same structure. Registered event listeners
     * won't be cloned; so they are not registered at the returned copy.
     *
     * @return the copy
     * @since 1.2
     */
    @Override
    public Object clone()
    {
        beginRead(false);
        try
        {
            AbstractHierarchicalConfiguration copy = (AbstractHierarchicalConfiguration) super
                    .clone();
            copy.setSynchronizer(NoOpSynchronizer.INSTANCE);
            copy.cloneInterpolator(this);
            copy.setSynchronizer(ConfigurationUtils.cloneSynchronizer(getSynchronizer()));
            copy.model = cloneNodeModel();

            return copy;
        }
        catch (CloneNotSupportedException cex)
        {
            // should not happen
            throw new ConfigurationRuntimeException(cex);
        }
        finally
        {
            endRead();
        }
    }

    /**
     * Creates a clone of the node model. This method is called by
     * {@code clone()}.
     *
     * @return the clone of the {@code NodeModel}
     * @since 2.0
     */
    protected abstract NodeModel<T> cloneNodeModel();

    /**
     * Helper method for resolving the specified key.
     *
     * @param key the key
     * @return a list with all results selected by this key
     */
    protected List<QueryResult<T>> fetchNodeList(String key)
    {
        return resolveKey(getRootNode(), key, getModel().getNodeHandler());
    }

    /**
     * Checks if the specified node is defined.
     *
     * @param node the node to be checked
     * @return a flag if this node is defined
     */
    protected boolean nodeDefined(T node)
    {
        DefinedVisitor<T> visitor = new DefinedVisitor<T>();
        NodeTreeWalker.INSTANCE.walkBFS(node, visitor, getModel().getNodeHandler());
        return visitor.isDefined();
    }

    /**
     * Returns the model used by this configuration.
     *
     * @return the node model
     */
    public NodeModel<T> getModel()
    {
        return model;
    }

    /**
     * Extracts the value from a query result.
     *
     * @param result the {@code QueryResult}
     * @param handler the {@code NodeHandler}
     * @return the value of this result (may be <b>null</b>)
     */
    private Object valueFromResult(QueryResult<T> result, NodeHandler<T> handler)
    {
        return result.isAttributeResult() ? result.getAttributeValue(handler)
                : handler.getValue(result.getNode());
    }

    /**
     * A specialized visitor that checks if a node is defined.
     * &quot;Defined&quot; in this terms means that the node or at least one of
     * its sub nodes is associated with a value.
     *
     */
    private static class DefinedVisitor<T> extends
            ConfigurationNodeVisitorAdapter<T>
    {
        /** Stores the defined flag. */
        private boolean defined;

        /**
         * Checks if iteration should be stopped. This can be done if the first
         * defined node is found.
         *
         * @return a flag if iteration should be stopped
         */
        @Override
        public boolean terminate()
        {
            return isDefined();
        }

        /**
         * Visits the node. Checks if a value is defined.
         *
         * @param node the actual node
         */
        @Override
        public void visitBeforeChildren(T node, NodeHandler<T> handler)
        {
            defined =
                    handler.getValue(node) != null
                            || !handler.getAttributes(node).isEmpty();
        }

        /**
         * Returns the defined flag.
         *
         * @return the defined flag
         */
        public boolean isDefined()
        {
            return defined;
        }
    }

    /**
     * A specialized visitor that fills a list with keys that are defined in a
     * node hierarchy.
     *
     * @param <T> the type of the nodes to be visited
     */
    private class DefinedKeysVisitor<T> extends
            ConfigurationNodeVisitorAdapter<T>
    {
        /** Stores the list to be filled. */
        private final Set<String> keyList;

        /** A stack with the keys of the already processed nodes. */
        private final Stack<String> parentKeys;

        /**
         * Default constructor.
         */
        public DefinedKeysVisitor()
        {
            keyList = new LinkedHashSet<String>();
            parentKeys = new Stack<String>();
        }

        /**
         * Creates a new {@code DefinedKeysVisitor} instance and sets the
         * prefix for the keys to fetch.
         *
         * @param prefix the prefix
         */
        public DefinedKeysVisitor(String prefix)
        {
            this();
            parentKeys.push(prefix);
        }

        /**
         * Returns the list with all defined keys.
         *
         * @return the list with the defined keys
         */
        public Set<String> getKeyList()
        {
            return keyList;
        }

        /**
         * {@inheritDoc} This implementation removes this
         * node's key from the stack.
         */
        @Override
        public void visitAfterChildren(T node, NodeHandler<T> handler)
        {
            parentKeys.pop();
        }

        /**
         * {@inheritDoc} If this node has a value, its key is added
         * to the internal list.
         */
        @Override
        public void visitBeforeChildren(T node, NodeHandler<T> handler)
        {
            String parentKey = parentKeys.isEmpty() ? null
                    : parentKeys.peek();
            String key = getExpressionEngine().nodeKey(node, parentKey, handler);
            parentKeys.push(key);
            if (handler.getValue(node) != null)
            {
                keyList.add(key);
            }
            handleAttributeKeys(key, node, handler);
        }

        /**
         * Appends all attribute keys of the current node.
         *
         * @param parentKey the parent key
         * @param node the current node
         * @param handler the {@code NodeHandler}
         */
        public void handleAttributeKeys(String parentKey, T node,
                NodeHandler<T> handler)
        {
            for (String attr : handler.getAttributes(node))
            {
                keyList.add(getExpressionEngine().attributeKey(parentKey, attr));
            }
        }
    }
}
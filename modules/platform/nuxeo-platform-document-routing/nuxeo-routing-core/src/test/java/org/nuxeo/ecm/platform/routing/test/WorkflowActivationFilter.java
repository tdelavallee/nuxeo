/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 *
 * Contributors:
 *     Guillaume Renard
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.routing.test;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.webengine.test.WebEngineFeatureCore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.3
 */
@RunWith(FeaturesRunner.class)
@Features({ WorkflowFeature.class, WebEngineFeatureCore.class })
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-document-routing-activation-filters.xml")
public class WorkflowActivationFilter extends AbstractGraphRouteTest {

    @Inject
    CoreSession session;

    @Inject
    DocumentRoutingService routing;

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "file");
        doc = session.createDocument(doc);
    }

    public void setRoute(String activationFiltername) {
        routeDoc = createRoute("myroute", session);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        session.saveDocument(node1);
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.setPropertyValue(GraphRoute.PROP_AVAILABILITY_FILTER, activationFiltername);
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        validate(routeDoc, session);
    }

    @Test
    public void testWorkflowWithoutFilterCanBeStarted() {
        setRoute(null);
        Assert.assertTrue(routing.canCreateInstance(session, Arrays.asList(doc.getId()), routeDoc.getName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidWorkflowName() {
        Assert.assertTrue(routing.canCreateInstance(session, Arrays.asList(doc.getId()), "WorkflowThatDoesNotExist"));
    }

    @Test
    public void testWorkflowIsRunnable() {
        setRoute("test_wf_pass");
        List<DocumentRoute> runnables = routing.getRunnableWorkflows(session, Arrays.asList(doc.getId()));
        Assert.assertEquals(1, runnables.size());
    }

    @Test
    public void testWorkflowCanBeStarted() {
        setRoute("test_wf_pass");
        Assert.assertTrue(routing.canCreateInstance(session, Arrays.asList(doc.getId()), routeDoc.getName()));
    }

    @Test
    public void testWorkflowIsNotRunnable() {
        setRoute("test_wf_fail");
        List<DocumentRoute> runnables = routing.getRunnableWorkflows(session, Arrays.asList(doc.getId()));
        Assert.assertEquals(0, runnables.size());
    }

    @Test
    public void testWorkflowCannotBeStarted() {
        setRoute("test_wf_fail");
        Assert.assertFalse(routing.canCreateInstance(session, Arrays.asList(doc.getId()), routeDoc.getName()));
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.elk.alg.layered.p5edges.oldloops.routing;

import java.util.List;

import org.eclipse.elk.alg.layered.graph.LEdge;
import org.eclipse.elk.alg.layered.options.InternalProperties;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopEdge;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopNode;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.math.KVectorChain;

/**
 * A self loop router that routes edges in the orthogonal edge routing style.
 */
public class OrthogonalSelfLoopRouter extends AbstractSelfLoopRouter {

    @Override
    public void routeSideSelfLoop(final OldSelfLoopEdge slEdge) {
        LEdge lEdge = slEdge.getEdge();

        // Calculate all bend points
        KVector sourceBendPoint = computeSourceBendPoint(slEdge);
        KVector targetBendPoint = computeTargetBendPoint(slEdge);

        // Apply bend points
        lEdge.getBendPoints().addAll(sourceBendPoint, targetBendPoint);
    }

    @Override
    public void routeCornerSelfLoop(final OldSelfLoopEdge slEdge) {
        LEdge lEdge = slEdge.getEdge();

        // Calculate all bend points
        KVector sourceBendPoint = computeSourceBendPoint(slEdge);
        KVector targetBendPoint = computeTargetBendPoint(slEdge);
        
        KVector cornerBendPoint = computeSingleCornerBendPoint(
                sourceBendPoint, targetBendPoint, slEdge.getTarget().getPortSide());
        
        // Apply bend points
        lEdge.getBendPoints().addAll(sourceBendPoint, cornerBendPoint, targetBendPoint);
    }

    @Override
    public void routeOpposingSelfLoop(final OldSelfLoopEdge slEdge) {
        LEdge lEdge = slEdge.getEdge();
        OldSelfLoopNode slNode = lEdge.getSource().getNode().getProperty(InternalProperties.SELF_LOOP_NODE_REPRESENTATION);

        // Calculate all bend points
        KVector sourceBendPoint = computeSourceBendPoint(slEdge);
        KVector targetBendPoint = computeTargetBendPoint(slEdge);

        List<KVector> cornerBendPoints = computeCornerBendpoints(slNode, slEdge, sourceBendPoint, targetBendPoint);

        // Apply bend points
        KVectorChain bendPoints = lEdge.getBendPoints();
        bendPoints.add(sourceBendPoint);
        bendPoints.addAll(cornerBendPoints);
        bendPoints.add(targetBendPoint);
    }

    @Override
    public void routeThreeCornerSelfLoop(final OldSelfLoopEdge slEdge) {
        LEdge lEdge = slEdge.getEdge();
        OldSelfLoopNode slNode = lEdge.getSource().getNode().getProperty(InternalProperties.SELF_LOOP_NODE_REPRESENTATION);

        // Calculate all bend points
        KVector sourceBendPoint = computeSourceBendPoint(slEdge);
        KVector targetBendPoint = computeTargetBendPoint(slEdge);

        List<KVector> cornerBendPoints = computeCornerBendpoints(slNode, slEdge, sourceBendPoint, targetBendPoint);

        // Apply bend points
        KVectorChain bendPoints = lEdge.getBendPoints();
        bendPoints.add(sourceBendPoint);
        bendPoints.addAll(cornerBendPoints);
        bendPoints.add(targetBendPoint);
    }

    @Override
    public void routeFourCornerSelfLoop(final OldSelfLoopEdge slEdge) {
        LEdge lEdge = slEdge.getEdge();
        OldSelfLoopNode slNode = lEdge.getSource().getNode().getProperty(InternalProperties.SELF_LOOP_NODE_REPRESENTATION);

        // Calculate all bend points
        KVector sourceBendPoint = computeSourceBendPoint(slEdge);
        KVector targetBendPoint = computeTargetBendPoint(slEdge);

        List<KVector> cornerBendPoints = computeCornerBendpoints(slNode, slEdge, sourceBendPoint, targetBendPoint);

        // Apply bend points
        KVectorChain bendPoints = lEdge.getBendPoints();
        bendPoints.add(sourceBendPoint);
        bendPoints.addAll(cornerBendPoints);
        bendPoints.add(targetBendPoint);
    }

}

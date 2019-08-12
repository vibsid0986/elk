/*******************************************************************************
 * Copyright (c) 2018 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.elk.alg.layered.p5edges.oldloops.labeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.elk.alg.layered.graph.LGraphUtil;
import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.alg.layered.options.InternalProperties;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopComponent;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopLabel;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopNode;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopPort;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.options.PortConstraints;
import org.eclipse.elk.core.options.PortSide;

/**
 * Knows how to merge the labels of {@link OldSelfLoopComponent}s.
 */
public final class SelfLoopComponentMerger {

    /** TODO: This should probably not be a constant. */
    private static final int PORT_DISTANCE = 5;

    /**
     * No instantiation.
     */
    private SelfLoopComponentMerger() {
    }

    /**
     * Merges the labels of all components of the given node.
     */
    public static void mergeComponents(final OldSelfLoopNode slNode) {
        LNode lNode = slNode.getNode();
        
        // only with free port constraints the edges will be merged.
        if (lNode.getProperty(InternalProperties.ORIGINAL_PORT_CONSTRAINTS) == PortConstraints.FREE) {
            List<OldSelfLoopPort> sourcePorts = new ArrayList<>();
            List<OldSelfLoopPort> targetPorts = new ArrayList<>();

            OldSelfLoopLabel label = new OldSelfLoopLabel();
            
            // for each component the labels are collected
            int componentsWithLabels = 0;
            for (OldSelfLoopComponent component : slNode.getSelfLoopComponents()) {
                // update the dependency graph to avoid offsets
                slNode.getNodeSide(PortSide.NORTH).getComponentDependencies().add(component);
                slNode.getNodeSide(PortSide.NORTH).setMaximumPortLevel(1);
                component.getDependencyComponents().clear();

                // set port level back to one and sort ports into target and source
                for (OldSelfLoopPort port : component.getPorts()) {
                    if (port.getLPort().getIncomingEdges().isEmpty()) {
                        targetPorts.add(port);
                    } else {
                        sourcePorts.add(port);
                    }
                    port.setMaximumLevel(1);
                    port.setPortSide(PortSide.NORTH);
                }

                // collect components label and save them to a new one (label)
                OldSelfLoopLabel componentLabel = component.getSelfLoopLabel();
                if (componentLabel != null) {
                    label.getLabels().addAll(componentLabel.getLabels());
                    label.getSize().y += componentLabel.getSize().y;
                    label.getSize().x = Math.max(label.getSize().x, componentLabel.getSize().x);
                    
                    // delete old label reference
                    component.setSelfLoopLabel(null);
                    
                    componentsWithLabels++;
                }
            }
            
            // we need to add an appropriate amount of label-label spacings
            if (componentsWithLabels > 0) {
                double labelLabelSpacing = LGraphUtil.getIndividualOrInherited(
                        lNode, LayeredOptions.SPACING_LABEL_LABEL);
                double delta = Math.max(0, labelLabelSpacing * (componentsWithLabels - 1));
                label.getSize().y += delta;
            }
            
            // a random component holds the label now
            slNode.getSelfLoopComponents().get(0).setSelfLoopLabel(label);
            Collections.reverse(label.getLabels());

            // route all edges anew
            double nodeWidth = lNode.getSize().x;
            for (OldSelfLoopPort port : sourcePorts) {
                KVector source = port.getLPort().getPosition();
                KVector size = port.getLPort().getSize();
                source.set(new KVector(nodeWidth / 2 + PORT_DISTANCE - size.x / 2, -size.y));
            }
            
            for (OldSelfLoopPort port : targetPorts) {
                KVector target = port.getLPort().getPosition();
                KVector size = port.getLPort().getSize();
                target.set(new KVector(nodeWidth / 2 - PORT_DISTANCE - size.x / 2, -size.y));
            }
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.elk.alg.layered.p5edges.oldloops.position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.alg.layered.options.InternalProperties;
import org.eclipse.elk.alg.layered.p3order.counting.BinaryIndexedTree;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopComponent;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopEdge;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopNode;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopNodeSide;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopOpposingSegment;
import org.eclipse.elk.alg.layered.p5edges.oldloops.OldSelfLoopPort;
import org.eclipse.elk.core.options.PortSide;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Positions ports around nodes that have fixed order port constraints such that edge crossings are minimized.
 */
public class FixedOrderSelfLoopPortPositioner extends AbstractSelfLoopPortPositioner {
    
    @Override
    public void position(final LNode node) {
        // receive the node representation and the nodes components
        OldSelfLoopNode slNode = node.getProperty(InternalProperties.SELF_LOOP_NODE_REPRESENTATION);

        // retrieve the ports from the components and sort them by their original index
        List<OldSelfLoopPort> allPorts = new ArrayList<OldSelfLoopPort>();
        for (OldSelfLoopComponent component : slNode.getSelfLoopComponents()) {
            List<OldSelfLoopPort> ports = component.getPorts();
            allPorts.addAll(ports);
        }
        allPorts.sort(ORIGINAL_INDEX_PORT_COMPARATOR);

        // add the original index to the ports
        for (int i = 0; i < allPorts.size(); i++) {
            OldSelfLoopPort port = allPorts.get(i);
            port.setOriginalIndex(i);
            slNode.appendPort(port, port.getPortSide());
        }

        // calculate the routing direction for the components
        minimizeSides(slNode);
        minimizeCrossings(slNode);

        // create the opposing segments
        for (OldSelfLoopComponent component : slNode.getSelfLoopComponents()) {
            Map<PortSide, Map<OldSelfLoopEdge, OldSelfLoopOpposingSegment>> componentSegments =
                    OldSelfLoopOpposingSegment.create(component, slNode);

            for (OldSelfLoopNodeSide side : slNode.getSides()) {
                Map<OldSelfLoopEdge, OldSelfLoopOpposingSegment> segmentsOfSide = componentSegments.get(side.getSide());
                if (segmentsOfSide != null) {
                    side.getOpposingSegments().putAll(segmentsOfSide);
                }
            }
        }

    }

    /**
     * TODO Document.
     */
    private void minimizeSides(final OldSelfLoopNode slNode) {
        for (OldSelfLoopComponent component : slNode.getSelfLoopComponents()) {
            List<OldSelfLoopPort> ports = component.getPorts();
            OldSelfLoopPort portWithGreatestDistanceToRight = ports.get(0);
            int portDistance = 0;
            int greatestDistance = 0;

            for (int i = 0; i < ports.size(); i++) {
                OldSelfLoopPort currentPort = ports.get(i);
                OldSelfLoopPort nextPort = ports.get((i + 1) % ports.size());

                // First port's side and index
                PortSide currentSide = currentPort.getPortSide();
                OldSelfLoopNodeSide currentSideNodeRep = slNode.getNodeSide(currentSide);
                int currentPortSideIndex = currentSideNodeRep.getPorts().indexOf(currentPort);

                // Second port's side and index
                PortSide nextSide = nextPort.getPortSide();
                OldSelfLoopNodeSide nextSideNodeRep = slNode.getNodeSide(nextSide);
                int nextPortSideIndex = nextSideNodeRep.getPorts().indexOf(nextPort);

                int newPortDistance = 0;

                int sideDistance = 0;
                if (currentSide == nextSide && currentPort.getOriginalIndex() > nextPort.getOriginalIndex()) {
                    sideDistance = slNode.getSides().size();
                    newPortDistance = currentSideNodeRep.getPorts().size() - currentPortSideIndex + nextPortSideIndex;
                    
                } else {
                    while (currentSide != nextSide) {
                        sideDistance++;
                        currentSide = currentSide.right();
                    }

                    newPortDistance = currentSide != nextSide
                            ? currentSideNodeRep.getPorts().size() - currentPortSideIndex + nextPortSideIndex
                            : nextPortSideIndex - currentPortSideIndex;
                }
                
                if (greatestDistance < sideDistance) {
                    greatestDistance = sideDistance;
                    portWithGreatestDistanceToRight = currentPort;
                    portDistance = newPortDistance;
                } else if (greatestDistance == sideDistance && portDistance < newPortDistance) {
                    portWithGreatestDistanceToRight = currentPort;
                    portDistance = newPortDistance;
                }
            }

            int portWithGreatestDistanceToRightIndex = ports.indexOf(portWithGreatestDistanceToRight);
            int rightIndex = portWithGreatestDistanceToRightIndex + 1 > ports.size()
                    ? ports.size()
                    : portWithGreatestDistanceToRightIndex + 1;
                    
            List<OldSelfLoopPort> startList = new ArrayList<>(ports.subList(0, rightIndex));
            List<OldSelfLoopPort> endList = new ArrayList<>(ports.subList(rightIndex, ports.size()));

            ports.clear();
            ports.addAll(endList);
            ports.addAll(startList);

            for (int i = 0; i < ports.size(); i++) {
                setDirection(ports.get(i), i, ports.size());
            }
        }

    }

    /**
     * TODO Document.
     */
    private void minimizeCrossings(final OldSelfLoopNode slNode) {
        int numberOfPorts = slNode.getNumberOfPorts();
        
        // initialize
        Multimap<Integer, Integer> componentsConfiguration = ArrayListMultimap.create();
        for (OldSelfLoopComponent component : slNode.getSelfLoopComponents()) {
            List<OldSelfLoopPort> ports = component.getPorts();
            int startIndex = ports.get(0).getOriginalIndex() + 1;
            int endIndex = ports.get(ports.size() - 1).getOriginalIndex() + 1;
            updateComponentConfiguration(componentsConfiguration, ports, startIndex, endIndex, numberOfPorts, true);
        }

        // find the minimum
        int previousMinimumCrossings = Integer.MAX_VALUE;
        int minimumCrossings = countCrossings(slNode, componentsConfiguration);

        while (minimumCrossings != 0 && minimumCrossings != previousMinimumCrossings) {
            previousMinimumCrossings = minimumCrossings;
            for (OldSelfLoopComponent component : slNode.getSelfLoopComponents()) {
                List<OldSelfLoopPort> ports = component.getPorts();
                List<OldSelfLoopPort> minimumRotation = new ArrayList<>(ports);

                for (int cnt = 0; cnt < ports.size() - 1; cnt++) {
                    // remove previous configuration
                    int previousStart = component.getPorts().get(0).getOriginalIndex() + 1;
                    int previousEnd = component.getPorts().get(component.getPorts().size() - 1).getOriginalIndex() + 1;
                    updateComponentConfiguration(componentsConfiguration, ports, previousStart, previousEnd,
                            numberOfPorts, false);

                    // rotate and add to configuration
                    List<OldSelfLoopPort> newRotation = rotate(component.getPorts());
                    component.getPorts().clear();
                    component.getPorts().addAll(newRotation);

                    int startIndex = newRotation.get(0).getOriginalIndex() + 1;
                    int endIndex = newRotation.get(newRotation.size() - 1).getOriginalIndex() + 1;
                    updateComponentConfiguration(componentsConfiguration, ports, startIndex, endIndex, numberOfPorts,
                            true);

                    // evaluate rotation
                    int newNumberOfCrossings = countCrossings(slNode, componentsConfiguration);
                    if (newNumberOfCrossings < minimumCrossings) {
                        minimumCrossings = newNumberOfCrossings;
                        minimumRotation = newRotation;
                        ports = newRotation;
                    }

                    component.getPorts().clear();
                    component.getPorts().addAll(minimumRotation);

                }
            }

            // set direction for the final setup
            for (OldSelfLoopComponent component : slNode.getSelfLoopComponents()) {
                List<OldSelfLoopPort> ports = component.getPorts();
                for (int i = 0; i < ports.size(); i++) {
                    setDirection(ports.get(i), i, ports.size());
                }

            }
        }
    }

    /**
     * TODO Document.
     */
    private void updateComponentConfiguration(final Multimap<Integer, Integer> componentsConfiguration,
            final List<OldSelfLoopPort> ports, final int startIndex, final int endIndex, final int totalNumberOfPorts,
            final boolean addValue) {

        if (addValue) {
            if (ports.size() != 1) {
                if (startIndex > endIndex) {
                    componentsConfiguration.put(0, endIndex);
                    componentsConfiguration.put(startIndex, endIndex + totalNumberOfPorts);
                } else {
                    componentsConfiguration.put(startIndex, endIndex);
                }
            } else {
                componentsConfiguration.put(startIndex, startIndex + totalNumberOfPorts);
            }
        } else {
            if (startIndex > endIndex) {
                componentsConfiguration.remove(0, endIndex);
                componentsConfiguration.remove(startIndex, endIndex + totalNumberOfPorts);
            } else {
                componentsConfiguration.remove(startIndex, endIndex);
            }
        }
    }

    /**
     * TODO Document.
     */
    private List<OldSelfLoopPort> rotate(final List<OldSelfLoopPort> formerRotation) {
        List<OldSelfLoopPort> newRotation = new ArrayList<>();
        newRotation.addAll(formerRotation.subList(1, formerRotation.size()));
        newRotation.add(formerRotation.get(0));
        return newRotation;
    }

    /**
     * TODO Document.
     */
    private int countCrossings(final OldSelfLoopNode slNode, final Multimap<Integer, Integer> componentsConfiguration) {
        int numberOfPorts = slNode.getNumberOfPorts();
        BinaryIndexedTree indexTree = new BinaryIndexedTree(numberOfPorts * 2 + 1);
        Deque<Integer> ends = new ArrayDeque<>();
        int crossings = 0;

        for (int index = 0; index < numberOfPorts * 2 + 1; index++) {
            indexTree.removeAll(index);
            
            // First get crossings for all edges.
            Collection<Integer> endPositions = componentsConfiguration.get(index);
            for (int endPosition : endPositions) {
                if (endPosition > index) {
                    crossings += indexTree.rank(endPosition);
                    ends.push(endPosition);
                }
            }
            
            // Then add end points.
            while (!ends.isEmpty()) {
                indexTree.add(ends.pop());
            }
        }

        return crossings;
    }
}

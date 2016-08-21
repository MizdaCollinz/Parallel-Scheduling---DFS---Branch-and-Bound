package scheduler;

import java.util.List;
import java.util.Queue;

import models.Edge;
import models.Node;

public interface ParallelSchedulerInterface {
	
	public void initiateNewSubtree(List<Node> nodes, List<Edge> edgeList, List<Queue<Node>> initialNodeStack, int initialBestBound);
	
}
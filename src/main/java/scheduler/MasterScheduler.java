package scheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import models.Edge;
import models.Node;

public class MasterScheduler implements SchedulerInterface {
	private static MasterScheduler masterScheduler;
	private List<SchedulerInterface> schedulerList;
	private List<Node> nodeList;
	private List<Edge> edgeList;
	private List<Node> optimalSchedule;
	private List<List<Node>> nodeLists;
	private int bestBound = 0;
	private static int traverseThreads;
	private static int numProcessors;

	// NEED TO MAKE THIS THREAD SAFE
	private Queue<ComparisonTuple> comparisonQueue = new LinkedList<ComparisonTuple>();

	// Prevents new objects of this class from being instantiated
	private MasterScheduler() {
	}

	public static MasterScheduler getInstance() {
		if (masterScheduler == null) {
			masterScheduler = new MasterScheduler();
		}

		return masterScheduler;
	}

	public static MasterScheduler getInstance(int numThreads, int numProcessors) {
		MasterScheduler.traverseThreads = numThreads - 1;
		MasterScheduler.numProcessors = numProcessors;

		if (masterScheduler == null) {
			masterScheduler = new MasterScheduler();
		}

		return masterScheduler;
	}

	public synchronized void compare(List<Node> schedule, int scheduleBound) {
		this.comparisonQueue.add(new ComparisonTuple(schedule, scheduleBound));
	}

	public List<Node> createSchedule(List<Node> nodeList) {
		// Initially bestBound is equivalent to serial schedule
		for (Node n : nodeList) {
			bestBound += n.getWeight();
		}

		// Create schedulers and pass in appropriate partial schedule
		Queue<SubpathTuple> subpathQueue = createSubpathTuples();
		
		ExecutorService executorService = Executors.newFixedThreadPool(traverseThreads);

		for (int i = 0; i < traverseThreads; i++) {
			executorService.execute(new Runnable() {
				public void run() {
					SubpathTuple tuple = subpathQueue.remove();
					ParallelSchedulerInterface scheduler = new PnV_DepthFirst_BaB_Scheduler(new ValidNodeFinder(),tuple.processorAllocator );
					scheduler.initiateNewSubtree(tuple.nodeList, null , tuple.nodeStack, bestBound );
				}
			});
		}

		executorService.shutdown();
		while(executorService.isTerminated() == false) {
			checkQueue();
		}
		
		return optimalSchedule;
	}
	
	private void checkQueue(){
		while(comparisonQueue.isEmpty() == false){
			ComparisonTuple tuple = comparisonQueue.remove();
			compareBounds(tuple.schedule,tuple.scheduleBound);
		}
	}

	private void compareBounds(List<Node> schedule, int scheduleBound) {
		if (scheduleBound < bestBound) {
			bestBound = scheduleBound;
			// Notify all schedules
			notifyAllSchedulers(bestBound);

			optimalSchedule = schedule;
		} else if (scheduleBound == bestBound && optimalSchedule.size() == 0) {
			optimalSchedule = schedule;
		}

	}

	private void notifyAllSchedulers(int bestBound) {
		for (SchedulerInterface scheduler : schedulerList) {
			// scheduler.setBestBound(bestBound);
		}
	}

	private SubpathTuple cloneSubpathTuple(SubpathTuple tuple) {
		// NodeStack, NodeList, ProcAll

		List<Queue<Node>> newNodeStack = new ArrayList<Queue<Node>>();
		List<Node> newNodeList = new ArrayList<Node>();
		ProcessorAllocator newProcessorAllocator = new ProcessorAllocator(
				numProcessors);

		// Clone the node list into newNodeList

		for (Node node : tuple.nodeList) {
			newNodeList.add(node.fullClone());
		}

		// Clone the node stack into newNodeStack
		for (Queue<Node> queue : tuple.nodeStack) {
			Queue<Node> newQueue = new LinkedList<Node>();
			for (Node node : queue) {
				// Find equivalent node in our new node references to place in
				// our new queue
				for (Node node2 : newNodeList) {
					if (node.equals(node2)) {
						newQueue.add(node2);
						break;
					}
				}
			}
			newNodeStack.add(newQueue);
		}

		// ProcessorAllocator is cloned
		for (Node node : newNodeList) {
			if (node.getHasRun() == true) {
				newProcessorAllocator.addToProcessor(node, node.getProcessor());
			}
		}
		
		// Clone all edges - This will take forever, well team, we'll sink with our ship.
		for (Node node : newNodeList) {
			List<Edge> incomingEdges = node.getIncomingEdges();
			List<Edge> outgoingEdges = node.getOutgoingEdges();
			
			for(Edge edge: incomingEdges){
				for(Node node2: newNodeList){
					if(edge.getStartNode().equals(node2)){
						edge.setStartNode(node2);
						break;
					}
				}
				edge.setEndNode(node);
			}
			
			for(Edge edge: outgoingEdges){
				for(Node node2: newNodeList){
					if(edge.getEndNode().equals(node2)){
						edge.setEndNode(node2);
						break;
					}
				}
				edge.setStartNode(node);
			}
		}
		
		SubpathTuple clonedTuple = new SubpathTuple(newNodeList, newNodeStack, newProcessorAllocator);

		return clonedTuple;

	}

	private Queue<SubpathTuple> createSubpathTuples() {
		// Find root nodes to work from
		ValidNodeFinder nodeFinder = new ValidNodeFinder();
		List<Node> rootNodes = nodeFinder.findRootNodes(this.nodeList);
		
		// Store all root nodes into a queue
		Queue<Node> rootNodeQueue = new LinkedList<Node>();
		rootNodeQueue.addAll(rootNodes);

		// Queue of subpath tuples used for ParallelScheduler
		Queue<SubpathTuple> subpathQueue = new LinkedList<SubpathTuple>();

		// Create empty node stack for SubpathTuple
		List<Queue<Node>> nodeStack = new ArrayList<Queue<Node>>();
		nodeStack.add(0, rootNodeQueue);	// Add root nodes onto the stack

		// Create tuple to store in queue
		ProcessorAllocatorInterface processorAllocatorInitial = new ProcessorAllocator(numProcessors);
		SubpathTuple subpathTuple = new SubpathTuple(nodeList, nodeStack, processorAllocatorInitial);
		subpathQueue.add(subpathTuple);

		// Initially subpaths are equal to root nodes, only one processor is allocated, others will just be mirrors
		int numSubpath = rootNodes.size();
		int nextNumSubpath = 0;
		int heuristic = traverseThreads;	// TODO: improve heuristic (maybe)
		int level = 0;
		
		while (numSubpath < heuristic) {
			SubpathTuple tuple = subpathQueue.peek();
			nodeStack = tuple.nodeStack;
			ProcessorAllocatorInterface processorAllocator = tuple.processorAllocator;
			
			if (level != nodeStack.size() - 1) {
				level = nodeStack.size() - 1;
				numSubpath = nextNumSubpath;
				nextNumSubpath = 0;
				continue;
			}
			
			subpathQueue.remove();

			Queue<Node> nodeQueue = nodeStack.get(level);

			// Loop through nodes on this level
			while (nodeQueue.size() > 0) {
				List<Node> schedule = new ArrayList<Node>();
				Node currentNode = nodeQueue.remove();
				while (processorAllocator.allocateProcessor(schedule,
						currentNode, currentNode.getCheckedProcessors())) { // Loop to new processor on same dependent node
					SubpathTuple newSubpathTuple = cloneSubpathTuple(tuple);
					List<Node> newNodeList = newSubpathTuple.nodeList;
					List<Queue<Node>> newNodeStack = newSubpathTuple.nodeStack;
					List<Node> satisfiedNodes = nodeFinder.findSatisfiedNodes(newNodeList);
					nextNumSubpath += satisfiedNodes.size();
					newNodeStack.add(new LinkedList<Node>());
					subpathQueue.add(newSubpathTuple);
				}
				
				currentNode.setHasRun(false);
				currentNode.resetCheckedProcessors();
			}
		}
		
		return subpathQueue;
	}

	private class ComparisonTuple {
		public List<Node> schedule;
		public int scheduleBound;

		public ComparisonTuple(List<Node> schedule, int scheduleBound) {
			this.schedule = schedule;
			this.scheduleBound = scheduleBound;
		}
	}

	private class SubpathTuple {
		public List<Node> nodeList;
		public List<Queue<Node>> nodeStack;
		public ProcessorAllocatorInterface processorAllocator;

		public SubpathTuple(List<Node> nodeList, List<Queue<Node>> nodeStack,
				ProcessorAllocatorInterface processorAllocator) {
			this.nodeList = nodeList;
			this.nodeStack = nodeStack;
			this.processorAllocator = processorAllocator;
		}

	}
}
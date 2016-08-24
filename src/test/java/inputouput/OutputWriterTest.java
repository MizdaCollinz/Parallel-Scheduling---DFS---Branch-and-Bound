package inputouput;

import inputoutput.OutputWriter;
import junitx.framework.FileAssert;
import models.Edge;
import models.Node;
<<<<<<< HEAD
import models.NodeTuple;
=======
>>>>>>> origin/master
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
<<<<<<< HEAD
import java.util.HashMap;
=======
>>>>>>> origin/master

/**
 * @author William Lin
 */
public class OutputWriterTest {

    public List<Node> inputNodeList = new ArrayList<Node>();
    public List<Edge> inputEdgeList = new ArrayList<Edge>();
<<<<<<< HEAD
    private HashMap<String, NodeTuple> inputNodeInfo = new HashMap<String, NodeTuple>();
=======
>>>>>>> origin/master

    @Test
    public void OutputFileTest() {
        //Instantiation of input nodes
        Node firstNode = new Node("a", 2);
        Node secondNode = new Node("b", 3);
        Node thirdNode = new Node("c", 3);
        Node fourthNode = new Node("d", 2);

        //Instantiation of input edges
        Edge firstEdge = new Edge(firstNode, secondNode, 1);
        Edge secondEdge = new Edge(firstNode, thirdNode, 2);
        Edge thirdEdge = new Edge(secondNode, fourthNode, 2);
        Edge fourthEdge = new Edge(thirdNode, fourthNode, 1);

        //Adding input node dependencies
        firstNode.addOutgoingEdge(firstEdge);
        firstNode.addOutgoingEdge(secondEdge);
        secondNode.addIncomingEdge(firstEdge);
        secondNode.addOutgoingEdge(thirdEdge);
        thirdNode.addIncomingEdge(secondEdge);
        thirdNode.addOutgoingEdge(fourthEdge);
        fourthNode.addIncomingEdge(thirdEdge);
        fourthNode.addIncomingEdge(fourthEdge);

        //Adding correct input node properties
<<<<<<< HEAD
        inputNodeInfo.put(firstNode.getName(), new NodeTuple(0, 1, new ArrayList<Integer>(), true));
        inputNodeInfo.put(secondNode.getName(), new NodeTuple(2, 1, new ArrayList<Integer>(), true));
        inputNodeInfo.put(thirdNode.getName(), new NodeTuple(4, 2, new ArrayList<Integer>(), true));
        inputNodeInfo.put(fourthNode.getName(), new NodeTuple(7, 2, new ArrayList<Integer>(), true));
=======
        firstNode.setStartTime(0);
        firstNode.setProcessor(1);
        secondNode.setStartTime(2);
        secondNode.setProcessor(1);
        thirdNode.setStartTime(4);
        thirdNode.setProcessor(2);
        fourthNode.setStartTime(7);
        fourthNode.setProcessor(2);
>>>>>>> origin/master

        //Adding nodes and edges into their respective lists to pass into OutputWriter
        inputNodeList.add(firstNode);
        inputNodeList.add(secondNode);
        inputNodeList.add(thirdNode);
        inputNodeList.add(fourthNode);

        inputEdgeList.add(firstEdge);
        inputEdgeList.add(secondEdge);
        inputEdgeList.add(thirdEdge);
        inputEdgeList.add(fourthEdge);

        //Name of output file hard coded as "outputFile"
        String outputFileName = "outputFile";

        OutputWriter ow = new OutputWriter();
<<<<<<< HEAD
        ow.writeFile(inputNodeList, inputNodeInfo, inputEdgeList, outputFileName);
=======
        ow.writeFile(inputNodeList, inputEdgeList, outputFileName);
>>>>>>> origin/master

        //Get working directory
        String workingDir = System.getProperty("user.dir");
        String dir = workingDir;

        if (!dir.contains("src")) {
            dir = dir + "/src/test";
        }

        //Prepared "expected" file to compare output file against
        File expected = new File(dir + "/resources/expectedOutput2.dot");

        File output = new File(workingDir + "/" + outputFileName + ".dot");

        FileAssert.assertEquals(expected, output);
    }
}

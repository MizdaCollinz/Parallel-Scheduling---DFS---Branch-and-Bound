package scheduler;

import java.util.List;
import models.Node;

public interface MasterSchedulerInterface extends SchedulerInterface {
	public void compare(List<Node> schedule, int scheduleBound);
	public List<Node> createSchedule(List<Node> nodeList);
}
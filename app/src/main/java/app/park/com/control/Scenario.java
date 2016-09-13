package app.park.com.control;

public class Scenario {
	/*
	 * 몇초부터 몇초 사이에 
	 * 어떤 일을 해야 하는지에 대한 정보를 담고 있어야 함
	 * 
	 * 어떤 객체의 어떤 이벤트가 발생했는지에 대한 정보를 담고 있어야 하고
	 * 나중에 이를 validate 해야함
	 */
	int startSecond;
	int endSecond;
	boolean scenarioTask1;
	int objectId;
	String success_action_log;
	String fail_action_log;
	
	public Scenario(int startSecond, int endSecond, boolean scenarioTask1,
			int objectId, String success_action_log, String fail_action_log) {
		super();
		this.startSecond = startSecond;
		this.endSecond = endSecond;
		this.scenarioTask1 = scenarioTask1;
		this.objectId = objectId;
		this.success_action_log = success_action_log;
		this.fail_action_log = fail_action_log;
	}
}

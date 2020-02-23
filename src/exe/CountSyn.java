package exe;

public class CountSyn {
	private int value = 0;	// the initial value is 0; the first return is 1
	public synchronized int getNext(){
		value = value + 1;
		return value;
	}
}

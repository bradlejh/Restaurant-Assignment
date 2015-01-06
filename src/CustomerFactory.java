import java.util.Random;
import java.util.Vector;
import java.util.concurrent.locks.Lock;

public class CustomerFactory extends Thread {

	private Vector<CustomerThread> customerThreadVector = new Vector<CustomerThread>();
	private Hostess hostessThread;
	
	public CustomerFactory(Hostess hostessThread) {
		this.hostessThread = hostessThread;;
		this.start();
	}
	
	public void run() {
		try {
			int customerNumber = 0;
			while (true) {
				CustomerThread ct = new CustomerThread(customerNumber++, hostessThread);
				customerThreadVector.add(ct);
				Thread.sleep(1000 * (int)(Math.random() * 5)); // customers come in between 0 and 5 seconds apart
			}
		} catch (InterruptedException ie) {
			System.out.println("CustomerFactory.run(): InterruptedException: " + ie.getMessage());
			for (CustomerThread ct : customerThreadVector) {
				ct.interrupt();
			}
		}
	}
}

class CustomerThread extends Thread {
	private int customerNumber;
	private Hostess hostessThread;
	private Table table;
	//private Lock lock;
	public volatile boolean  doneEating;
	public CustomerThread(int customerNumber, Hostess hostessThread) {
		this.customerNumber = customerNumber;
		this.hostessThread = hostessThread;
		doneEating=false;
		this.start();
	}
	
	public int getCustomerNumber() {
		return this.customerNumber;
	}
	
	public Table getTable() {
		return this.table;
	}
	
	public void run() {
		try {
		
			table = hostessThread.seatCustomer(this);
			Random rand=new Random();
			int order=rand.nextInt(3);
			String name;
			if ( order == 0 ) {
				name="Steak Dinner";
			} else if ( order == 1 ) {
				name ="Seafood Dinner";
			} 
			else{
				name="Vegetarian Diner";
			}
			
			table.getWaiterThread().takeOrder(order,this);
			System.out.println("here");
			table.getLock().lock();
			table.getReadyCondition().await();//waiting for waiter to receive order then signal customer
			table.getLock().unlock();
			Restaurant.addMessage("Customer " + this.customerNumber + " has received order " +name +  " at table " + this.table.getTableNumber());
			System.out.println("custoemr sleeping");
			Thread.sleep(1000 * (int)(Math.random() * 10)); // sleep for between 0 and 10 seconds
			if(hostessThread != null){
				hostessThread.customerLeaving(this);
			}
		} catch (InterruptedException ie) {
			System.out.println("CustomerThread.run(): InterruptedException: " + ie.getMessage());
		}
			}
}
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WaiterFactory {

	private Vector<WaiterThread> waiterThreadVector = new Vector<WaiterThread>();
	private int numWaiters;
	private int numTablesPerWaiter;
	private Hostess hostess;
	private Lock lock = new ReentrantLock();
	private Condition waiterAvailable = lock.newCondition();

	public WaiterFactory(Hostess hostess, int numWaiters, int numTablesPerWaiter) {
		this.hostess = hostess;
		this.numWaiters = numWaiters;
		this.numTablesPerWaiter = numTablesPerWaiter;
		for (int i=0; i < numWaiters; i++) {
			waiterThreadVector.add(new WaiterThread(hostess, i, this, numTablesPerWaiter));
		}
	}

	public void returnWaiter(WaiterThread wt) {
		lock.lock();
		wt.returnTable(wt.getTable(0));
		//waiterThreadVector.set(wt.getWaiterNumber(), new WaiterThread(wt.getHostess(), wt.getWaiterNumber(), this));
		waiterAvailable.signal();
		lock.unlock();

	}

	public WaiterThread getWaiter() {
		WaiterThread wt = null;
		try {
			lock.lock();
			while (wt == null) {
				int i;
				for (i=0; i < waiterThreadVector.size(); i++) {
					wt = waiterThreadVector.get(i);
					if (wt != null) {
						// this will only allow one table per waiter
						// use a semaphore to allow more than one table per waiter
						if (wt.getNumAvailableTables() > 0) {
//						if (wt.getTable() == null) {
							break;
						}
					}
				}
				if (i == waiterThreadVector.size()) {
					// if i get here, i haven't secured a waiter yet
					waiterAvailable.await();
				}
			}
		} catch(InterruptedException ie) {
			System.out.println("WaiterFactory.getWaiter(): IE : " + ie.getMessage());
		} finally {
			lock.unlock();
		}
		return wt;
	}

}

class WaiterThread extends Thread {
	private Hostess hostess;
//	private Table table;
	private Vector<Table> tables;
	private int waiterNumber;
	private WaiterFactory waiterFactory;
	private Semaphore numTablesSemaphore;
	private Lock waiterLock = new ReentrantLock();
	private Condition tableAssignedCondition = waiterLock.newCondition();
	private Vector<Order> waiterOrders;

	public WaiterThread(Hostess hostess, int waiterNumber, WaiterFactory waiterFactory, int numTablesPerWaiter) {
		this.hostess = hostess;
		this.waiterNumber = waiterNumber;
		this.waiterFactory = waiterFactory;
		this.numTablesSemaphore = new Semaphore(numTablesPerWaiter);
		tables = new Vector<Table>(numTablesPerWaiter);
		waiterOrders=new Vector<Order>(numTablesPerWaiter);
		this.start();
	}

	public int getNumAvailableTables() {
		int numPermitsAvailable = numTablesSemaphore.availablePermits();
		return numPermitsAvailable;
	}
	public Hostess getHostess() {
		return this.hostess;
	}

	public void returnTable(Table table) {
//		this.table = null;
		tables.remove(table);
		numTablesSemaphore.release();
	}

	public void setTable(Table table) {
		try {
			numTablesSemaphore.acquire();
//			this.table = table;
			tables.add(table);
			this.waiterLock.lock();
			this.tableAssignedCondition.signalAll();
			this.waiterLock.unlock();
		} catch (InterruptedException ie) {
			System.out.println("WaiterFactory.setTable(" + table.getTableNumber() + ") IE: " + ie.getMessage());
		}
	}

	public Table getTable(int i) {
		if (tables.size() > i) {
			return tables.elementAt(i);			
		}
		return null;
//		return this.table;
	}

	public int getWaiterNumber() {
		return this.waiterNumber;
	}

	public synchronized void takeOrder(int order, CustomerThread ct)
	{
		Restaurant.addMessage("Waiter " + this.waiterNumber + " has taken an order for customer " + ct.getCustomerNumber() + " from table "+ ct.getTable().getTableNumber());
		System.out.println(order);
		Order dummy=new Order(order,ct.getCustomerNumber(),ct.getTable(),this);
		CookFactory.addOrder(dummy);
	}
	public synchronized void finishOrder(Order o)
	{
		System.out.println(this.waiterNumber + " finishOrder");
		waiterOrders.add(o);
	}
	
	public void run() {
		try {
			while (true) {
				// waiter can only have one table currently
				// wait until notified, meaning that a table has been seated
				// i don't think i always want to do this - what if a table has already been set?
				//this.waiterLock.lock();
				System.out.println("just after lock");
				//this.tableAssignedCondition.await();
				//this.waiterLock.unlock();
				System.out.println("here?");
				Thread.sleep(1000 * (int)(Math.random() * 10)); // sleep for between 0 and 10 seconds
				System.out.println(this.waiterNumber + "waiter just slept");
				if (!waiterOrders.isEmpty()) {
					System.out.println("waiter has something to deliver");
					Order deliverOrder=waiterOrders.get(0);
					deliverOrder.getTable().getLock().lock();
					// signal the customer who is "eating"
					deliverOrder.getTable().getReadyCondition().signal();//signals custoemr that dinner has arrived
					deliverOrder.getTable().getLock().unlock();
					Restaurant.addMessage("Waiter "+ this.waiterNumber + " has delivered order " + deliverOrder.getOrderText() + " to table " + deliverOrder.getTable().getTableNumber());
					waiterOrders.remove(0);//remove item from vector as it has been delivered
				}
				//waiterFactory.returnWaiter(this);
			}
		} catch (InterruptedException ie) {
			System.out.println("CustomerThread.run(): InterruptedException: " + ie.getMessage());
		}
	}
}
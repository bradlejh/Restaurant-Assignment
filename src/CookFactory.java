import java.util.*;
import java.util.concurrent.locks.*;

public class CookFactory {
	private static Vector<Order> ordersToCook;

	public CookFactory(CookPanel cp, int numCooks ) {
		ordersToCook = new Vector<Order>();

		for ( int i=0; i<numCooks; i++ ) {
			CookThread c = new CookThread( i, cp, this );
			c.start();
		}

	}
	
	public Vector<Order> getOrdersToCook()
	{
		return ordersToCook;
	}
	public static synchronized void addOrder(Order order)
	{
		ordersToCook.add(order);
	}
}


class CookThread extends Thread {
	private int cookNumber;
	private CookPanel cookPanel;
	private CookFactory cookFactory;
	
	public CookThread( int n, CookPanel cp, CookFactory cf ) {
		cookNumber = n;
		cookPanel = cp;
		cookFactory = cf;		
	}
	
	public void run() {
		cookPanel.addCookMessage( "Cook" + cookNumber + " is ready to cook." );
		while(true)
		{
			if(cookFactory.getOrdersToCook().isEmpty())
			{
				System.out.println("Cook " + this.cookNumber + " in sleep");
				try{
					sleep(10000);
				}
				catch(InterruptedException ie)
				{
					System.out.println("cook interruped during sleep");
				}
			}
			else
			{
				String orderText= cookFactory.getOrdersToCook().get(0).getOrderText();
				Order currOrder=cookFactory.getOrdersToCook().get(0);
				cookFactory.getOrdersToCook().remove(0);
				cookPanel.addCookMessage("Cook " + cookNumber + " is cooking an order for customer " + currOrder.getCustomerNumber() + " for table " + currOrder.getTable().getTableNumber()+ ".");
				Random rand=new Random();
				try{
					sleep((rand.nextInt(6)+5)*1000);
					currOrder.getWaiter().finishOrder(currOrder);
					cookPanel.addCookMessage("Cook " + cookNumber + " has completed an order " + currOrder.getCustomerNumber() + " for table " + currOrder.getTable().getTableNumber() + ".");
				}
				catch(InterruptedException ie)
				{
					System.out.println("cook interrupted while sleep of cooking");
				}
			}
			
		}
		
	}
}

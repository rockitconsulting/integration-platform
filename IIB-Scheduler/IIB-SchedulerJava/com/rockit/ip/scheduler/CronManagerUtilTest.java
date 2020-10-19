package com.rockit.ip.scheduler;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;

public class CronManagerUtilTest {

	
	@Test
	public void cronutil() throws ParseException {
		 System.out.println( CronUtil.nextExecution("0 0 8-10 * * *"));
		 System.out.println( CronUtil.nextExecution("0/5 0 8-10 * * *"));
		 System.out.println( CronUtil.nextExecution("0 0/2 * * * *"));

		 System.out.println("MEZ");
		 System.out.println( CronUtil.nextExecution("0 0/2 * * * *","2017-11-03 15:32:00 MEZ"));
		 System.out.println("MESZ");
		 System.out.println( CronUtil.nextExecution("0 0/7 * * * *","2017-11-03 15:32:00 MESZ"));
		 System.out.println("CEST");
		 System.out.println( CronUtil.nextExecution("0 0/7 * * * *","2017-11-03 15:32:00 CEST"));
		 
		 
	}

	
	@Test
	public void test() {
		 CronManagerUtil cmu = new CronManagerUtil("0 0 8-10 * * *");
		 printOut(cmu);
		 
		 cmu = new CronManagerUtil("0/5 0 8-10 * * *");
		 printOut(cmu);

		 cmu = new CronManagerUtil("0 0/2 * * * *");
		 printOut(cmu);
		 
	}

	private void printOut(CronManagerUtil cmu) {
		String expression = cmu.getExpression();
		Date next = cmu.next(new Date());
		
		System.out.println(expression + " - " + next);
	}

}

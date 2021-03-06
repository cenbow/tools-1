/* 
 * Copyright 2005 - 2009 Terracotta, Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package tools.test.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * <p>
 * This is just a simple job that says "Hello" to the world.
 * </p>
 * 
 * @author Bill Kratzer
 */
public class HelloJob implements Job {

	/**
	 * <p>
	 * Empty constructor for job initilization
	 * </p>
	 * <p>
	 * Quartz requires a public empty constructor so that the scheduler can instantiate the class whenever it needs.
	 * </p>
	 */
	public HelloJob() {
	}

	/**
	 * <p>
	 * Called by the <code>{@link org.quartz.Scheduler}</code> when a <code>{@link org.quartz.Trigger}</code> fires that
	 * is associated with the <code>Job</code>.
	 * </p>
	 * 
	 * @throws JobExecutionException if there is an exception while executing the job.
	 */
	public void execute(JobExecutionContext context) throws JobExecutionException {

		// Say Hello to the World and display the date/time
		System.out.println("Hello World! - " + context.getJobDetail().getJobDataMap().get("id"));

		// 计数
		SimpleExample.atomicInteger.incrementAndGet();

		// 睡睡
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

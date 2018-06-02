package io.goobox.sync.storj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorjExecutorService extends ThreadPoolExecutor {
	
	private static final Logger logger = LoggerFactory.getLogger(StorjExecutorService.class);
	
	private ExecutorService executorService; 
	
	private TaskQueue tasks;
	
	private List< Runnable> running = Collections.synchronizedList(new ArrayList());
	
    private volatile Runnable currentTask;
	
	public StorjExecutorService(int processors, LinkedBlockingQueue<Runnable> queue) {
		super(processors, processors, 0L, TimeUnit.MILLISECONDS, queue);
	}

	@Override
	public void execute(Runnable command) {
		logger.info("running task");
		super.execute(command);
	}
	
	
	
	
	
	

}

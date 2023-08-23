package com.seamless.ers.links.uciplink.schedulers;

import com.seamless.common.ERSConfigurationException;
import com.seamless.common.service.Service;
import com.seamless.common.service.ServiceManager;
import com.seamless.ers.links.uciplink.UCIPLinkConfig;
import com.seamless.ers.links.uciplink.commandProcessor.RequestProcessorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KeepAliveScheduler implements Service,Runnable{

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);
    private boolean isStopped = false;
    private final RequestProcessorHandler requestProcessorHandler;
    private final UCIPLinkConfig config;
    ExecutorService executorService;
    ExecutorService poolExecutorService;

    public KeepAliveScheduler(UCIPLinkConfig config,RequestProcessorHandler requestProcessorHandler) {
        this.config = config;
        this.requestProcessorHandler = requestProcessorHandler;
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(this);
        try
        {
            logger.info("Adding " + getServiceName() + " to Service-manager");
            ServiceManager.getInstance().addService(this);
        }
        catch (ERSConfigurationException e)
        {

            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run()
    {

        logger.info("Keep alive Scheduler started. Sleeping for [" + config.getKeepAliveStartDelayMillis() + "] ms");
        try
        {
            TimeUnit.MILLISECONDS.sleep(config.getKeepAliveStartDelayMillis());
            logger.info("Starting");
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }

        int keepAliveThreadCount = config.getKeepAliveThreadPoolSize();
        this.poolExecutorService = Executors.newFixedThreadPool(keepAliveThreadCount);

        while (true)
        {
            synchronized (this)
            {
                if(isStopped)
                {
                    logger.info("Keep-alive stopped!");
                    break;
                }
            }

            try
            {

                for(int i=0; i < config.getKeepAliveRequestCount(); i++)
                {
                    this.poolExecutorService.submit(new KeepAliveRunner(requestProcessorHandler, config));
                }

            }
            catch (Exception e)
            {
                logger.error("Exception while executing keep alive requests");
                logger.error(e.getMessage(), e);
            }

            try
            {
                TimeUnit.MILLISECONDS.sleep(config.getKeepAlivePeriodInMillis());
            }
            catch (Exception e)
            {
                logger.error("Exception while sleeping periodically");
                logger.error(e.getMessage(), e);
            }
        }

    }

    @Override
    public boolean shutdown(long l) {
        logger.info("Shutting down keep-alive single scheduler");
        synchronized (this){
            isStopped = true;
        }
        if(poolExecutorService != null)
        {
            try
            {
                logger.info("Shutting down keep-alive single-thread executor");
                poolExecutorService.shutdown();
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }
        }
        if(executorService != null)
        {
            try
            {
                logger.info("Shutting down keep-alive fixed-thread-pool");
                executorService.shutdown();
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }
        }



        return true;
    }

    @Override
    public String getServiceName() {
        return "Keep-Alive";
    }
}

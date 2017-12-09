package player.client;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectJob implements Job {
    private static Logger log = LoggerFactory.getLogger(ConnectJob.class);
    public static final String KEY_MAX_CLIENT = "KEY_MAX_CLIENT";

    private MaxClient mMaxClient;

    public ConnectJob(){}

    public void execute(JobExecutionContext context) throws JobExecutionException {
        mMaxClient = (MaxClient) context.getJobDetail().getJobDataMap().get(KEY_MAX_CLIENT);
        if (mMaxClient.getWebSocketClient() == null ||
                mMaxClient.getWebSocketClient().getConnection() == null ||
                    !mMaxClient.getWebSocketClient().getConnection().isOpen()){

            log.debug("ConnectJob: try connect");

            mMaxClient.createNewWebSocketClient();
            mMaxClient.getWebSocketClient().connect();
        }
    }
}

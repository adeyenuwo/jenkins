package metanectar.provisioning.task;

import hudson.model.Node;
import metanectar.model.MasterServer;
import metanectar.provisioning.MasterProvisioningNodeProperty;
import metanectar.provisioning.MasterProvisioningService;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Paul Sandoz
 */
public class MasterProvisionTask extends MasterServerTask<MasterProvisioningService.Provisioned> {
    private static final Logger LOGGER = Logger.getLogger(MasterProvisionTask.class.getName());

    private final URL metaNectarEndpoint;

    private final Map<String, Object> properties;

    private final Node node;

    private final int id;

    public MasterProvisionTask(long timeout, MasterServer ms, URL metaNectarEndpoint, Map<String, Object> properties, Node node, int id) {
        super(timeout, ms, MasterServer.Action.Provision);

        this.metaNectarEndpoint = metaNectarEndpoint;
        this.properties = properties;
        this.node = node;
        this.id = id;
    }

    public Future<MasterProvisioningService.Provisioned> doStart() throws Exception {
        try {
            LOGGER.info("Provisioning master " + ms.getName() + " on node " + node.getNodeName());

            // Set the provision started state on the master server
            ms.setProvisionStartedState(node, id);

            final MasterProvisioningNodeProperty p = MasterProvisioningNodeProperty.get(node);

            Map<String, Object> provisionProperties = new HashMap<String, Object>(properties);
            return p.getProvisioningService().provision(ms, metaNectarEndpoint, provisionProperties);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Provisioning error for master " + ms.getName() + " on node " + node.getNodeName(), e);

            ms.setProvisionErrorState(e);
            throw e;
        }
    }

    public MasterServerTask end(Future<MasterProvisioningService.Provisioned> f) throws Exception {
        try {
            final MasterProvisioningService.Provisioned provisioned = f.get();

            LOGGER.info("Provisioning completed for master " + ms.getName() + " on node " + node.getNodeName());

            // Set the provision completed state on the master server
            ms.setProvisionCompletedState(provisioned.getHome(), provisioned.getEndpoint());
        } catch (Exception e) {
            final Throwable cause = (e instanceof ExecutionException) ? e.getCause() : e;
            LOGGER.log(Level.WARNING, "Provisioning completion error for master " + ms.getName() + " on node " + node.getNodeName(), cause);

            ms.setProvisionErrorState(cause);
            throw e;
        }

        return null;
    }
}

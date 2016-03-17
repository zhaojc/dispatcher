package simpleci.dispatcher.listener;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.*;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleci.dispatcher.settings.Settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GceApi {
    private final static Logger logger = LoggerFactory.getLogger(GceApi.class);
    private static final String APPLICATION_NAME = "simpleci-dispatcher";
    private final Settings settings;

    public GceApi(Settings settings) {
        this.settings = settings;
    }


    public void createInstance(String instanceName) {
        try {
            Compute compute = createApi();
            Snapshot snapshot = instanceSnapshot(compute);
            logger.info(String.format("Will use snapshot %s for instance %s", snapshot.getName(), instanceName));
            Operation diskOperation = createDisk(compute, instanceName, snapshot);
            logger.info(String.format("Wait for disk creation: %s", instanceName));
            waitForOperation(compute, diskOperation);
            Operation instanceCreateOperation = makeInstance(compute, instanceName);
            logger.info(String.format("Wait for instance creation: %s", instanceName));
            waitForOperation(compute, instanceCreateOperation);
            logger.info(String.format("Instance %s created successfully", instanceName));
        } catch (IOException | GeneralSecurityException e) {
            logger.error("", e);
        }

    }

    private Operation makeInstance(Compute compute, String instanceName) throws IOException {
        String startupScript = generateStartupScript();

        Instance instance = new Instance();
        instance
                .setName(instanceName)
                .setZone(String.format("projects/%s/zones/%s", settings.gceProject, settings.gceZone))
                .setMachineType(String.format("projects/%s/zones/%s/machineTypes/%s", settings.gceProject, settings.gceZone, settings.gceMachineType))
                .setMetadata(new Metadata().setItems(new ImmutableList.Builder<Metadata.Items>().add(
                        new Metadata.Items()
                                .setKey("startup-script")
                                .setValue(startupScript))
                        .build()))
                .setDisks(new ImmutableList.Builder<AttachedDisk>().add(
                        new AttachedDisk()
                                .setType("PERSISTENT")
                                .setBoot(true)
                                .setMode("READ_WRITE")
                                .setAutoDelete(true)
                                .setDeviceName(instanceName)
                                .setSource(String.format("projects/%s/zones/%s/disks/%s", settings.gceProject, settings.gceZone, instanceName))
                ).build())
                .setCanIpForward(false)
                .setNetworkInterfaces(new ImmutableList.Builder<NetworkInterface>().add(
                        new NetworkInterface()
                                .setNetwork(String.format("projects/%s/global/networks/default", settings.gceProject))
                                .setAccessConfigs(new ImmutableList.Builder<AccessConfig>().add(
                                        new AccessConfig()
                                                .setName("External NAT")
                                                .setType("ONE_TO_ONE_NAT")
                                ).build())
                ).build())
                .setServiceAccounts(new ImmutableList.Builder<ServiceAccount>().add(
                        new ServiceAccount()
                                .setEmail("default")
                                .setScopes(new ImmutableList.Builder<String>()
                                        .add("https://www.googleapis.com/auth/cloud-platform")
                                        .build())
                ).build());

        return compute.instances().insert(settings.gceProject, settings.gceZone, instance).execute();
    }

    private String generateStartupScript() {
        final int minRunningTime = 60 * 10;
        final int granulatiry = 60;
        return "#!/bin/bash\n" +
                "docker run " +
                "-e HOSTNAME=$(hostname) " +
                String.format("-e RABBITMQ_HOST=%s ", settings.gceRabbitmqHost) +
                String.format("-e RABBITMQ_PORT=%d ", settings.gceRabbitmqPort) +
                String.format("-e RABBITMQ_USER=%s ", settings.gceRabbitmqUser) +
                String.format("-e RABBITMQ_PASSWORD=%s ", settings.gceRabbitmqPassword) +
                "-e EXIT_IF_INACTIVE=true " +
                String.format("-e MINIMUM_RUNNING_TIME=%d ", minRunningTime) +
                String.format("-e TIME_GRANULARITY=%d ", granulatiry) +
                "-v \"/var/run/docker.sock:/var/run/docker.sock\" " +
                "simpleci/worker";
    }

    private void waitForOperation(Compute compute, Operation diskOperation) throws IOException {
        while (true) {
            Operation currentOperation = compute.zoneOperations().get(settings.gceProject, settings.gceZone, diskOperation.getName()).execute();
            if (currentOperation.getStatus().equals("DONE")) {
                return;
            }
        }
    }

    private Operation createDisk(Compute compute, String name, Snapshot snapshot) throws IOException {
        Disk disk = new Disk();
        disk
                .setName(name)
                .setSizeGb(10L)
                .setSourceSnapshot(String.format("projects/%s/global/snapshots/%s", settings.gceProject, snapshot.getName()))
                .setType(String.format("projects/%s/zones/%s/diskTypes/pd-standard", settings.gceProject, settings.gceZone))
                .setZone(String.format("projects/%s/zones/%s", settings.gceProject, settings.gceZone));
        Compute.Disks.Insert operation = compute.disks().insert(settings.gceProject, settings.gceZone, disk);
        return operation.execute();
    }

    private Snapshot instanceSnapshot(Compute compute) throws IOException {
        SnapshotList snapshots = compute.snapshots().list(settings.gceProject).execute();
        List<Snapshot> snapshotList = snapshots.getItems();
        Collections.sort(snapshotList, new Comparator<Snapshot>() {
            @Override
            public int compare(Snapshot first, Snapshot second) {
                return dateFromGce(second.getCreationTimestamp()).compareTo(dateFromGce(first.getCreationTimestamp()));
            }
        });
        return snapshotList.get(0);
    }

    private Date dateFromGce(String creationTimestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            return sdf.parse(creationTimestamp);
        } catch (ParseException e) {
            logger.error("", e);
            return null;
        }
    }

    private Compute createApi() throws IOException, GeneralSecurityException {
        GoogleCredential credential = GoogleCredential.fromStream(new ByteArrayInputStream(settings.gceServiceAccount.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singleton(ComputeScopes.COMPUTE));

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        return new Compute.Builder(
                httpTransport, JSON_FACTORY, null).setApplicationName(APPLICATION_NAME)
                .setHttpRequestInitializer(credential).build();
    }

    public void stopAndRemoveInstance(String instanceName) {
        try {
            Compute compute = createApi();
            logger.info(String.format("Stopping and removing instance: %s", instanceName));
            Operation operation = compute.instances().stop(settings.gceProject, settings.gceZone, instanceName).execute();
            waitForOperation(compute, operation);
            operation = compute.instances().delete(settings.gceProject, settings.gceZone, instanceName).execute();
            waitForOperation(compute, operation);
            logger.info(String.format("Instance %s stopped", instanceName));
        } catch(IOException |GeneralSecurityException e) {
            logger.error("", e);
        }
    }
}

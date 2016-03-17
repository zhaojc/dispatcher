package simpleci.dispatcher.settings;

public class Settings {

    @Parameter(name = "cache.use_cache")
    public boolean useCache;

    @Parameter(name = "cache.type")
    public String cacheType;

    @Parameter(name = "cache.ssh_host")
    public String cacheSshHost;

    @Parameter(name = "cache.ssh_user")
    public String cacheSshUser;

    @Parameter(name = "cache.ssh_port")
    public int cacheSshPort;

    @Parameter(name = "cache.ssh_dir")
    public String cacheSshDir;

    @Parameter(name = "cache.ssh_private_key")
    public String cacheSshPrivateKey;

    @Parameter(name = "cache.ssh_public_key")
    public String cacheSshPublicKey;

    @Parameter(name = "gce.active")
    public boolean useGce;

    @Parameter(name = "gce.service_account")
    public String gceServiceAccount;

    @Parameter(name = "gce.project")
    public String gceProject;

    @Parameter(name = "gce.zone")
    public String gceZone;

    @Parameter(name = "gce.machine_type")
    public String gceMachineType;

    @Parameter(name = "gce.rabbitmq_host")
    public String gceRabbitmqHost;

    @Parameter(name = "gce.rabbitmq_port")
    public int gceRabbitmqPort;

    @Parameter(name = "gce.rabbitmq_user")
    public String gceRabbitmqUser = "guest";

    @Parameter(name = "gce.rabbitmq_password")
    public String gceRabbitmqPassword = "guest";
}

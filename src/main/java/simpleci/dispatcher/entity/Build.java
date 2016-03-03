package simpleci.dispatcher.entity;

public class Build {
    public long id;
    public long projectId;
    public int number;
    public String config;
    public String branch;
    public String commit;
    public String commitRange;
}

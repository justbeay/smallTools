package com.tool.redismgr;

import com.sampullara.cli.Argument;

/**
 * Created by Administrator on 2017/12/14.
 */
public class RedisMgmtCommand {

    @Argument(alias = "h", description = "Redis host to connect (eg:127.0.0.1:7002)")
    private String host;
    @Argument(alias = "p", description = "Redis host password")
    private String password;
    @Argument(alias = "s", description = "Redis select db to query")
    private Integer selectDB;
    @Argument(alias = "c", description = "Redis command to execute")
    private String command;
    @Argument(description = "Redis command params", delimiter = " ")
    private String[] params;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getSelectDB() {
        return selectDB;
    }

    public void setSelectDB(Integer selectDB) {
        this.selectDB = selectDB;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }
}

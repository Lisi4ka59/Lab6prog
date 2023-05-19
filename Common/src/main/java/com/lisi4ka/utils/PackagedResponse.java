package com.lisi4ka.utils;

import java.io.Serializable;

public class PackagedResponse implements Serializable {
    private String message = null;
    private CommandMap commandMap = null;
    private int packageCount;
    private int packageNumber;
    public PackagedResponse(String message, ResponseStatus status){
        this.message = message;
        this.status = status;
    }
    public PackagedResponse(int packageCount, ResponseStatus status){
        this.packageCount = packageCount;
        this.status = status;
    }
    public PackagedResponse(CommandMap commandMap){
        this.message = null;
        this.status = ResponseStatus.OK;
        this.commandMap = commandMap;
    }
    public String getMessage() {
        return message;
    }
    public CommandMap getCommandMap(){return commandMap;}

    public int getPackageCount() {
        return packageCount;
    }
    public ResponseStatus status;
}


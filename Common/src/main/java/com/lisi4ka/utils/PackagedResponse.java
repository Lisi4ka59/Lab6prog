package com.lisi4ka.utils;

import java.io.Serializable;

public class PackagedResponse implements Serializable {
    private final String message;
    private CommandMap commandMap = null;
    public PackagedResponse(String message, ResponseStatus status){
        this.message = message;
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
    ResponseStatus status;
}


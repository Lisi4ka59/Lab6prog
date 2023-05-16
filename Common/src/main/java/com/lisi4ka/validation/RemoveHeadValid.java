package com.lisi4ka.validation;

import com.lisi4ka.utils.PackagedCommand;

public class RemoveHeadValid implements Validation{
    public PackagedCommand[] valid(String[] commandText){
        if (commandText.length == 1){
            return new PackagedCommand[]{new PackagedCommand(commandText[0], null)};
        }
        else {
            throw new IllegalArgumentException("Command \"remove_head\" takes no arguments!\n");
        }
    }
}

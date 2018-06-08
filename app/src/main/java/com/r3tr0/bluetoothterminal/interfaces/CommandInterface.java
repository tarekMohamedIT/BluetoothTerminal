package com.r3tr0.bluetoothterminal.interfaces;

/**
 * Created by r3tr0 on 5/24/18.
 */

public interface CommandInterface<input> {
    void execute(input[] params);

}

/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils;

public class EulaExcption extends Exception {
    @java.io.Serial
    static final long serialVersionUID = 1145141919810L;

    public EulaExcption() {
        super();
    }

    public EulaExcption(String message) {
        super(message);
    }
}

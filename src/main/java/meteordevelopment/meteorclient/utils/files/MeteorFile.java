/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.files;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;

public class MeteorFile extends File {
    public MeteorFile(@NotNull String pathname) {
        super(pathname);
    }

    public MeteorFile(String parent, @NotNull String childName) {
        super(parent, childName + ".meteor");
    }

    public MeteorFile(File parent, @NotNull String childName) {
        super(parent, childName + ".meteor");
    }

    public MeteorFile(@NotNull URI uri) {
        super(uri);
    }
}

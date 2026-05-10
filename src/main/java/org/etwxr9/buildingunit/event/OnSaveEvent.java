package org.etwxr9.buildingunit.event;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.nio.file.Path;

@Deprecated
public class OnSaveEvent extends PostSaveSchematicEvent {

    public OnSaveEvent(Clipboard clipboard, Path filePath) {
        super(clipboard, filePath, filePath.getFileName().toString());
    }
}

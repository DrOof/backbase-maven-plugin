package com.backbase.maven.plugins.watch;

import org.apache.maven.model.FileSet;

/**
 * A PatternSet for files.
 */
public class WatchFileSet extends FileSet {

    private boolean recursive;

    public WatchFileSet() {
        this.recursive = true;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

}